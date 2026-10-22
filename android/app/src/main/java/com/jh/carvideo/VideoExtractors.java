package com.jh.carvideo;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Los lectores de archivos de ExoPlayer, con dos arreglos para los videos que bajan de YouTube:
 *
 * - MP4 fragmentado sin índice al inicio: los saltos usan la tabla del final ({@link FragmentedMp4Index}).
 * - En H.264 solo cuenta como cuadro clave un IDR. Estos archivos marcan como clave también cuadros I
 *   que no lo son: después de un salto el decodificador empezaba ahí, mostraba la imagen verde y rota
 *   y a los 2 s se trababa. Así se empieza siempre en un IDR.
 */
@OptIn(markerClass = UnstableApi.class)
final class VideoExtractors {

    private VideoExtractors() {}

    static ExtractorsFactory create() {
        DefaultExtractorsFactory base = new DefaultExtractorsFactory()
                // MP3 o AAC sin índice: salta calculando por el bitrate
                .setConstantBitrateSeekingEnabled(true);
        return new ExtractorsFactory() {
            @NonNull
            @Override
            public Extractor[] createExtractors() {
                return wrapAll(base.createExtractors(), null);
            }

            @NonNull
            @Override
            public Extractor[] createExtractors(@NonNull Uri uri, @NonNull Map<String, List<String>> responseHeaders) {
                return wrapAll(base.createExtractors(uri, responseHeaders), uri.getPath());
            }
        };
    }

    private static Extractor[] wrapAll(Extractor[] extractors, @Nullable String path) {
        for (int i = 0; i < extractors.length; i++) {
            extractors[i] = new VideoExtractor(extractors[i], path);
        }
        return extractors;
    }

    /** El lector tal cual; solo cambian su mapa de saltos (MP4 fragmentado) y los cuadros clave (H.264). */
    private static final class VideoExtractor implements Extractor {
        private final Extractor inner;
        @Nullable private final String path; // para leer la tabla del final del MP4 fragmentado

        VideoExtractor(Extractor inner, @Nullable String path) {
            this.inner = inner;
            this.path = path;
        }

        @Override
        public boolean sniff(@NonNull ExtractorInput input) throws IOException {
            return inner.sniff(input);
        }

        @Override
        public void init(@NonNull ExtractorOutput output) {
            boolean fragmentedMp4 = inner instanceof FragmentedMp4Extractor && path != null;
            inner.init(new ExtractorOutput() {
                @NonNull
                @Override
                public TrackOutput track(int id, int type) {
                    TrackOutput track = output.track(id, type);
                    return type == C.TRACK_TYPE_VIDEO ? new IdrKeyframes(track) : track;
                }

                @Override
                public void endTracks() {
                    output.endTracks();
                }

                @Override
                public void seekMap(@NonNull SeekMap seekMap) {
                    // Sin sidx el lector dice "no se puede saltar": se usa la tabla del final
                    SeekMap index = fragmentedMp4 && !seekMap.isSeekable() ? FragmentedMp4Index.readIndex(path) : null;
                    output.seekMap(index != null ? index : seekMap);
                }
            });
        }

        @Override
        public int read(@NonNull ExtractorInput input, @NonNull PositionHolder seekPosition) throws IOException {
            return inner.read(input, seekPosition);
        }

        @Override
        public void seek(long position, long timeUs) {
            inner.seek(position, timeUs);
        }

        @Override
        public void release() {
            inner.release();
        }
    }

    /**
     * Pista de video H.264: a las muestras marcadas como clave que no son IDR se les quita la marca.
     * Solo después de ver 2 IDR: un video con un único IDR al inicio (raro) queda como venía, porque
     * sin la marca no se podría saltar en él.
     */
    private static final class IdrKeyframes implements TrackOutput {
        private static final int HEAD_BYTES = 4096; // el primer corte del cuadro aparece en los primeros bytes

        private final TrackOutput inner;
        private final byte[] head = new byte[HEAD_BYTES];
        private final ParsableByteArray scratch = new ParsableByteArray();
        private boolean avc;
        private int headLength;   // bytes guardados del inicio de la muestra en curso
        private int sampleBytes;  // bytes escritos de la muestra en curso
        private int idrCount;

        IdrKeyframes(TrackOutput inner) {
            this.inner = inner;
        }

        @Override
        public void format(@NonNull Format format) {
            avc = MimeTypes.VIDEO_H264.equals(format.sampleMimeType);
            inner.format(format);
        }

        @Override
        public int sampleData(@NonNull DataReader input, int length, boolean allowEndOfInput, int sampleDataPart)
                throws IOException {
            if (!avc || sampleDataPart != SAMPLE_DATA_PART_MAIN) {
                return inner.sampleData(input, length, allowEndOfInput, sampleDataPart);
            }
            // De a 64 KB como mucho: quien escribe vuelve a llamar hasta completar la muestra
            int toRead = Math.min(length, 64 * 1024);
            scratch.reset(toRead);
            int read = input.read(scratch.getData(), 0, toRead);
            if (read == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput) return C.RESULT_END_OF_INPUT;
                throw new EOFException();
            }
            remember(scratch.getData(), 0, read);
            scratch.setLimit(read);
            inner.sampleData(scratch, read, sampleDataPart);
            return read;
        }

        @Override
        public void sampleData(@NonNull ParsableByteArray data, int length, int sampleDataPart) {
            if (avc && sampleDataPart == SAMPLE_DATA_PART_MAIN) remember(data.getData(), data.getPosition(), length);
            inner.sampleData(data, length, sampleDataPart);
        }

        private void remember(byte[] src, int offset, int length) {
            int n = Math.min(length, HEAD_BYTES - headLength);
            if (n > 0) {
                System.arraycopy(src, offset, head, headLength, n);
                headLength += n;
            }
            sampleBytes += length;
        }

        @Override
        public void sampleMetadata(long timeUs, int flags, int size, int offset, @Nullable CryptoData cryptoData) {
            if (avc && cryptoData == null && offset == 0 && sampleBytes == size
                    && (flags & C.BUFFER_FLAG_KEY_FRAME) != 0) {
                int slice = firstSliceType(head, headLength);
                if (slice == 5) {
                    idrCount++;
                } else if (slice == 1 && idrCount >= 2) {
                    flags &= ~C.BUFFER_FLAG_KEY_FRAME;
                }
            }
            headLength = 0;
            sampleBytes = 0;
            inner.sampleMetadata(timeUs, flags, size, offset, cryptoData);
        }

        /** Tipo del primer corte (5 = IDR, 1 = otro cuadro) en datos con códigos de inicio; -1 si no está. */
        private static int firstSliceType(byte[] data, int length) {
            for (int i = 0; i + 3 < length; i++) {
                if (data[i] == 0 && data[i + 1] == 0 && data[i + 2] == 1) {
                    int type = data[i + 3] & 0x1F;
                    if (type == 5 || type == 1) return type;
                    i += 2;
                }
            }
            return -1;
        }
    }
}
