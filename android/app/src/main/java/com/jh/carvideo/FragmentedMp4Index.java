package com.jh.carvideo;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saltos en MP4 fragmentados, como los que bajan los descargadores de YouTube.
 *
 * Esos archivos no dicen cuánto duran (mvhd = 0) ni traen índice al inicio (sidx). Solo tienen al
 * final una tabla (mfra) con el tiempo y la posición de cada fragmento. Ni el reproductor de Android
 * ni ExoPlayer la leen: no sabían la duración y no podían saltar. Aquí se lee esa tabla y se le da a
 * ExoPlayer como mapa de saltos (tiempo → posición en el archivo).
 *
 * Además, muchos fragmentos empiezan con un cuadro I que no es IDR: empezar ahí deja la imagen verde.
 * El mapa solo ofrece fragmentos que empiezan con IDR (se revisa al saltar, no al abrir).
 */
@OptIn(markerClass = UnstableApi.class)
final class FragmentedMp4Index {

    private static final int MOOV = fourcc("moov");
    private static final int TRAK = fourcc("trak");
    private static final int TKHD = fourcc("tkhd");
    private static final int MDIA = fourcc("mdia");
    private static final int MDHD = fourcc("mdhd");
    private static final int HDLR = fourcc("hdlr");
    private static final int MINF = fourcc("minf");
    private static final int STBL = fourcc("stbl");
    private static final int STSD = fourcc("stsd");
    private static final int AVC1 = fourcc("avc1");
    private static final int AVC3 = fourcc("avc3");
    private static final int AVCC = fourcc("avcC");
    private static final int MVEX = fourcc("mvex");
    private static final int TREX = fourcc("trex");
    private static final int MOOF = fourcc("moof");
    private static final int TRAF = fourcc("traf");
    private static final int TFHD = fourcc("tfhd");
    private static final int TFDT = fourcc("tfdt");
    private static final int TRUN = fourcc("trun");
    private static final int MFRA = fourcc("mfra");
    private static final int TFRA = fourcc("tfra");
    private static final int MFRO = fourcc("mfro");
    private static final int VIDE = fourcc("vide");

    private static final int MAX_BOX_READ = 8 * 1024 * 1024; // moov, mfra y moof de estos archivos pesan KB

    private FragmentedMp4Index() {}

    private static final class TrackInfo {
        long timescale;
        boolean video;
        int nalLengthSize;          // H.264: bytes del largo de cada NAL (avcC); 0 si no es H.264
        long defaultSampleDuration; // de trex: lo que dura cada muestra si el fragmento no lo dice
    }

    /** Duración en ms según la tabla del final; 0 si el archivo no es un MP4 fragmentado con esa tabla. */
    static int readDurationMs(String path) {
        SeekMap index = readIndex(path);
        return index != null ? (int) (index.getDurationUs() / 1000) : 0;
    }

    /**
     * Mapa de saltos según la tabla del final (mfra), con la duración real (fin del último fragmento).
     * null si el archivo no la trae o algo no cuadra: entonces sigue sin poder saltar, como antes.
     */
    static SeekMap readIndex(String path) {
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            long length = file.length();
            if (length < 16) return null;

            // mfro (últimos 16 bytes) dice cuánto mide mfra
            ParsableByteArray mfro = readBytes(file, length - 16, 16);
            if (mfro.readUnsignedInt() != 16 || mfro.readInt() != MFRO) return null;
            mfro.skipBytes(4); // versión y flags
            long mfraSize = mfro.readUnsignedInt();
            if (mfraSize < 16 || mfraSize > Math.min(length, MAX_BOX_READ)) return null;
            long mfraStart = length - mfraSize;
            ParsableByteArray mfra = readBytes(file, mfraStart, (int) mfraSize);
            if (mfra.readUnsignedInt() != mfraSize || mfra.readInt() != MFRA) return null;

            Map<Integer, TrackInfo> tracks = readTracks(file, length);
            if (tracks.isEmpty()) return null;

            // tfra de la pista de video (si no hay, la primera que aparezca)
            long[][] entries = null;
            TrackInfo entriesTrack = null;
            int entriesTrackId = -1;
            int pos = 8;
            while (pos + 8 <= mfraSize) {
                mfra.setPosition(pos);
                long size = mfra.readUnsignedInt();
                int type = mfra.readInt();
                if (size < 8 || pos + size > mfraSize) break;
                if (type == TFRA) {
                    int version = mfra.readUnsignedByte();
                    mfra.skipBytes(3);
                    int trackId = mfra.readInt();
                    TrackInfo track = tracks.get(trackId);
                    if (track != null && (entries == null || (track.video && !entriesTrack.video))) {
                        long[][] parsed = readTfra(mfra, version, pos + (int) size);
                        if (parsed != null) {
                            entries = parsed;
                            entriesTrack = track;
                            entriesTrackId = trackId;
                        }
                    }
                }
                pos += (int) size;
            }
            if (entries == null || entriesTrack.timescale <= 0) return null;

            // Tiempos en µs, en orden y dentro del archivo; una entrada por fragmento
            List<long[]> chunks = new ArrayList<>(); // {tiempo µs, posición}
            long lastTime = -1;
            long lastOffset = -1;
            for (long[] e : entries) {
                long timeUs = e[0] * 1_000_000L / entriesTrack.timescale;
                long offset = e[1];
                if (offset <= lastOffset || offset >= mfraStart) {
                    if (offset == lastOffset) continue; // varias muestras clave en el mismo fragmento
                    return null;
                }
                if (timeUs < lastTime) return null;
                chunks.add(new long[]{timeUs, offset});
                lastTime = timeUs;
                lastOffset = offset;
            }
            if (chunks.size() < 2) return null;

            // Duración: donde termina el último fragmento
            long endUs = readFragmentEndUs(file, lastOffset, mfraStart, tracks);
            if (endUs <= lastTime) {
                // No se pudo leer: el último dura lo que el promedio
                endUs = lastTime + lastTime / (chunks.size() - 1);
            }

            long[] timesUs = new long[chunks.size()];
            long[] offsets = new long[chunks.size()];
            for (int i = 0; i < chunks.size(); i++) {
                timesUs[i] = chunks.get(i)[0];
                offsets[i] = chunks.get(i)[1];
            }
            // Los IDR se revisan en la pista de video H.264 (la de la tabla, si es esa)
            int idrTrackId = entriesTrack.video && entriesTrack.nalLengthSize > 0 ? entriesTrackId : -1;
            return new FragmentSeekMap(path, timesUs, offsets, endUs, idrTrackId,
                    idrTrackId >= 0 ? entriesTrack.nalLengthSize : 0);
        } catch (Exception e) {
            return null; // archivo raro o pendrive retirado: queda sin saltos, como antes
        }
    }

    /**
     * Saltos a fragmentos que empiezan con un cuadro IDR: el anterior y el siguiente al tiempo pedido
     * (ExoPlayer usa el más cercano). Cada fragmento se revisa la primera vez que se lo necesita.
     */
    private static final class FragmentSeekMap implements SeekMap {
        private static final int MAX_LOOKUP = 40; // fragmentos que se revisan hacia cada lado

        private final String path;
        private final long[] timesUs;
        private final long[] offsets;
        private final long durationUs;
        private final int videoTrackId;   // -1: no se revisan IDR (no es H.264)
        private final int nalLengthSize;
        private final byte[] idrState;    // 0 sin revisar, 1 empieza con IDR, 2 no

        FragmentSeekMap(String path, long[] timesUs, long[] offsets, long durationUs, int videoTrackId,
                        int nalLengthSize) {
            this.path = path;
            this.timesUs = timesUs;
            this.offsets = offsets;
            this.durationUs = durationUs;
            this.videoTrackId = videoTrackId;
            this.nalLengthSize = nalLengthSize;
            this.idrState = new byte[timesUs.length];
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long getDurationUs() {
            return durationUs;
        }

        @NonNull
        @Override
        public SeekPoints getSeekPoints(long timeUs) {
            int index = Util.binarySearchFloor(timesUs, timeUs, true, true);
            int before = index;
            for (int i = index; i >= 0 && i > index - MAX_LOOKUP; i--) {
                if (startsWithIdr(i)) {
                    before = i;
                    break;
                }
            }
            SeekPoint first = new SeekPoint(timesUs[before], offsets[before]);
            if (timesUs[before] >= timeUs) return new SeekPoints(first);
            for (int i = index + 1; i < timesUs.length && i <= index + MAX_LOOKUP; i++) {
                if (startsWithIdr(i)) return new SeekPoints(first, new SeekPoint(timesUs[i], offsets[i]));
            }
            return new SeekPoints(first);
        }

        /** Lo piden el hilo del reproductor y el de carga: de a uno. */
        private synchronized boolean startsWithIdr(int index) {
            if (videoTrackId < 0) return true;
            if (idrState[index] == 0) {
                int result = readFirstSliceType(index);
                // Si no se pudo leer, se ofrece igual: es mejor saltar que no saltar
                idrState[index] = (byte) (result == 1 ? 2 : 1);
            }
            return idrState[index] == 1;
        }

        /** Tipo del primer corte de video del fragmento (5 = IDR, 1 = otro cuadro); -1 si no se pudo leer. */
        private int readFirstSliceType(int index) {
            long moofStart = offsets[index];
            try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
                ParsableByteArray header = readBytes(file, moofStart, 8);
                long moofSize = header.readUnsignedInt();
                if (header.readInt() != MOOF || moofSize < 8 || moofSize > MAX_BOX_READ) return -1;
                ParsableByteArray moof = readBytes(file, moofStart, (int) moofSize);
                boolean firstTraf = true;
                for (int[] traf : children(moof, 8, (int) moofSize)) {
                    if (traf[0] != TRAF) continue;
                    long base = -1;
                    long dataOffset = 0;
                    boolean isVideo = false;
                    for (int[] box : children(moof, traf[1], traf[2])) {
                        moof.setPosition(box[1]);
                        if (box[0] == TFHD) {
                            int flags = moof.readInt() & 0xFFFFFF;
                            isVideo = moof.readInt() == videoTrackId;
                            if ((flags & 0x01) != 0) {
                                base = moof.readUnsignedLongToLong(); // base_data_offset
                            } else if ((flags & 0x020000) != 0 || firstTraf) {
                                base = moofStart; // default-base-is-moof, o primera pista del fragmento
                            }
                        } else if (box[0] == TRUN && isVideo) {
                            int flags = moof.readInt() & 0xFFFFFF;
                            moof.skipBytes(4); // sample_count
                            if ((flags & 0x01) != 0) dataOffset = moof.readInt();
                            break; // la primera muestra es la de la primera tanda
                        }
                    }
                    firstTraf = false;
                    if (!isVideo) continue;
                    if (base < 0) return -1;
                    return firstSliceType(file, base + dataOffset);
                }
                return -1;
            } catch (Exception e) {
                return -1;
            }
        }

        /** Recorre las NAL de la muestra (largo + datos) hasta el primer corte. */
        private int firstSliceType(RandomAccessFile file, long samplePos) throws IOException {
            long fileLength = file.length();
            int size = (int) Math.min(4096, fileLength - samplePos);
            if (size <= nalLengthSize) return -1;
            ParsableByteArray data = readBytes(file, samplePos, size);
            int pos = 0;
            while (pos + nalLengthSize < size) {
                long nalLength = 0;
                for (int i = 0; i < nalLengthSize; i++) nalLength = (nalLength << 8) | (data.getData()[pos + i] & 0xFF);
                int type = data.getData()[pos + nalLengthSize] & 0x1F;
                if (type == 5 || type == 1) return type;
                if (nalLength <= 0) return -1;
                pos += nalLengthSize + nalLength;
            }
            return -1;
        }
    }

    /** Entradas {tiempo, posición del moof} de un tfra; el cursor está después del track_ID. */
    private static long[][] readTfra(ParsableByteArray data, int version, int end) {
        int lengths = data.readInt();
        int skip = ((lengths >> 4) & 3) + 1 + ((lengths >> 2) & 3) + 1 + (lengths & 3) + 1;
        long count = data.readUnsignedInt();
        int entrySize = (version == 1 ? 16 : 8) + skip;
        if (count <= 0 || data.getPosition() + count * entrySize > end) return null;
        long[][] entries = new long[(int) count][];
        for (int i = 0; i < count; i++) {
            long time = version == 1 ? data.readLong() : data.readUnsignedInt();
            long offset = version == 1 ? data.readLong() : data.readUnsignedInt();
            data.skipBytes(skip);
            entries[i] = new long[]{time, offset};
        }
        return entries;
    }

    /** Pistas del moov: escala de tiempo, si es video (y H.264) y la duración por defecto de sus muestras (trex). */
    private static Map<Integer, TrackInfo> readTracks(RandomAccessFile file, long length) throws IOException {
        Map<Integer, TrackInfo> tracks = new HashMap<>();
        // En un MP4 fragmentado el moov va antes del primer fragmento
        long pos = 0;
        while (pos + 8 <= length) {
            ParsableByteArray header = readBytes(file, pos, 16 <= length - pos ? 16 : 8);
            long size = header.readUnsignedInt();
            int type = header.readInt();
            if (size == 1 && header.bytesLeft() >= 8) size = header.readUnsignedLongToLong();
            if (size < 8 || pos + size > length || type == MOOF) break;
            if (type == MOOV) {
                if (size > MAX_BOX_READ) break;
                ParsableByteArray moov = readBytes(file, pos, (int) size);
                for (int[] trak : children(moov, 8, (int) size)) {
                    if (trak[0] == TRAK) readTrak(moov, trak[1], trak[2], tracks);
                }
                for (int[] mvex : children(moov, 8, (int) size)) {
                    if (mvex[0] != MVEX) continue;
                    for (int[] trex : children(moov, mvex[1], mvex[2])) {
                        if (trex[0] != TREX) continue;
                        moov.setPosition(trex[1] + 4); // versión y flags
                        TrackInfo track = tracks.get(moov.readInt());
                        moov.skipBytes(4); // default_sample_description_index
                        if (track != null) track.defaultSampleDuration = moov.readUnsignedInt();
                    }
                }
                break;
            }
            pos += size;
        }
        return tracks;
    }

    private static void readTrak(ParsableByteArray moov, int start, int end, Map<Integer, TrackInfo> tracks) {
        int trackId = -1;
        TrackInfo track = new TrackInfo();
        for (int[] box : children(moov, start, end)) {
            if (box[0] == TKHD) {
                moov.setPosition(box[1]);
                int version = moov.readUnsignedByte();
                moov.skipBytes(3 + (version == 1 ? 16 : 8)); // flags, creación y modificación
                trackId = moov.readInt();
            } else if (box[0] == MDIA) {
                for (int[] child : children(moov, box[1], box[2])) {
                    moov.setPosition(child[1]);
                    if (child[0] == MDHD) {
                        int version = moov.readUnsignedByte();
                        moov.skipBytes(3 + (version == 1 ? 16 : 8));
                        track.timescale = moov.readUnsignedInt();
                    } else if (child[0] == HDLR) {
                        moov.skipBytes(8); // versión, flags y pre_defined
                        track.video = moov.readInt() == VIDE;
                    } else if (child[0] == MINF) {
                        track.nalLengthSize = readNalLengthSize(moov, child[1], child[2]);
                    }
                }
            }
        }
        if (trackId >= 0 && track.timescale > 0) tracks.put(trackId, track);
    }

    /** minf → stbl → stsd → avc1 → avcC: cuántos bytes mide el largo de cada NAL. 0 si no es H.264. */
    private static int readNalLengthSize(ParsableByteArray moov, int start, int end) {
        for (int[] stbl : children(moov, start, end)) {
            if (stbl[0] != STBL) continue;
            for (int[] stsd : children(moov, stbl[1], stbl[2])) {
                if (stsd[0] != STSD) continue;
                // versión y flags (4) + cantidad (4); después la primera descripción
                for (int[] entry : children(moov, stsd[1] + 8, stsd[2])) {
                    if (entry[0] != AVC1 && entry[0] != AVC3) return 0;
                    // 78 bytes de campos del video antes de las cajas hijas
                    for (int[] avcC : children(moov, entry[1] + 78, entry[2])) {
                        if (avcC[0] != AVCC) continue;
                        moov.setPosition(avcC[1] + 4);
                        return (moov.readUnsignedByte() & 3) + 1;
                    }
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Fin del último fragmento en µs (el mayor entre sus pistas): su tiempo de inicio (tfdt) más lo que
     * duran sus muestras (trun). -1 si no se puede saber.
     */
    private static long readFragmentEndUs(RandomAccessFile file, long moofStart, long limit,
                                          Map<Integer, TrackInfo> tracks) throws IOException {
        ParsableByteArray header = readBytes(file, moofStart, 8);
        long moofSize = header.readUnsignedInt();
        if (header.readInt() != MOOF || moofSize < 8 || moofStart + moofSize > limit || moofSize > MAX_BOX_READ) {
            return -1;
        }
        ParsableByteArray moof = readBytes(file, moofStart, (int) moofSize);
        long endUs = -1;
        for (int[] traf : children(moof, 8, (int) moofSize)) {
            if (traf[0] != TRAF) continue;
            TrackInfo track = null;
            long defaultDuration = 0;
            long baseTime = -1;
            long total = 0;
            for (int[] box : children(moof, traf[1], traf[2])) {
                moof.setPosition(box[1]);
                if (box[0] == TFHD) {
                    int flags = moof.readInt() & 0xFFFFFF;
                    track = tracks.get(moof.readInt());
                    if (track == null) break;
                    defaultDuration = track.defaultSampleDuration;
                    if ((flags & 0x01) != 0) moof.skipBytes(8); // base_data_offset
                    if ((flags & 0x02) != 0) moof.skipBytes(4); // sample_description_index
                    if ((flags & 0x08) != 0) defaultDuration = moof.readUnsignedInt();
                } else if (box[0] == TFDT) {
                    int version = moof.readUnsignedByte();
                    moof.skipBytes(3);
                    baseTime = version == 1 ? moof.readUnsignedLongToLong() : moof.readUnsignedInt();
                } else if (box[0] == TRUN) {
                    int flags = moof.readInt() & 0xFFFFFF;
                    long count = moof.readUnsignedInt();
                    if ((flags & 0x01) != 0) moof.skipBytes(4); // data_offset
                    if ((flags & 0x04) != 0) moof.skipBytes(4); // first_sample_flags
                    if ((flags & 0x100) == 0) {
                        total += count * defaultDuration;
                        continue;
                    }
                    int perSample = 4 * (1 + ((flags & 0x200) != 0 ? 1 : 0) + ((flags & 0x400) != 0 ? 1 : 0)
                            + ((flags & 0x800) != 0 ? 1 : 0));
                    if (moof.getPosition() + count * perSample > box[2]) return -1;
                    for (long i = 0; i < count; i++) {
                        total += moof.readUnsignedInt();
                        moof.skipBytes(perSample - 4);
                    }
                }
            }
            // Sin tfdt, después de un salto los tiempos saldrían mal: mejor no saltar
            if (track == null || baseTime < 0) return -1;
            endUs = Math.max(endUs, (baseTime + total) * 1_000_000L / track.timescale);
        }
        return endUs;
    }

    /** Cajas hijas en [start, end): {tipo, inicio del contenido, fin}. */
    private static List<int[]> children(ParsableByteArray data, int start, int end) {
        List<int[]> boxes = new ArrayList<>();
        int pos = start;
        while (pos + 8 <= end) {
            data.setPosition(pos);
            long size = data.readUnsignedInt();
            int type = data.readInt();
            int header = 8;
            if (size == 1) {
                size = data.readUnsignedLongToLong();
                header = 16;
            }
            if (size < header || pos + size > end) break;
            boxes.add(new int[]{type, pos + header, (int) (pos + size)});
            pos += (int) size;
        }
        return boxes;
    }

    private static ParsableByteArray readBytes(RandomAccessFile file, long pos, int size) throws IOException {
        byte[] bytes = new byte[size];
        file.seek(pos);
        file.readFully(bytes);
        return new ParsableByteArray(bytes);
    }

    private static int fourcc(String s) {
        return (s.charAt(0) << 24) | (s.charAt(1) << 16) | (s.charAt(2) << 8) | s.charAt(3);
    }
}
