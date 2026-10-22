package com.jh.carvideo;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Analiza el inicio y el final de una canción para que Automix mezcle en el lugar correcto:
 *
 * - Inicio real: dónde termina el silencio inicial (para no entrar con un hueco).
 * - Fin real: dónde empieza el silencio final (muchas canciones tienen 1-3 s de silencio).
 * - Fin de la parte fuerte: dónde la canción ya bajó claramente de volumen (fade-out),
 *   para empezar la mezcla ahí y no cuando ya casi no se oye.
 *
 * - Sonoridad: qué tan fuerte se oye la canción (aprox. LUFS, como ReplayGain 2.0 / EBU R128),
 *   para igualar el volumen entre canciones.
 *
 * Decodifica solo los primeros 10 s, 10 s del medio y los últimos 25 s, con el decodificador por SOFTWARE de
 * Android (los radios de gama baja tienen pocos decodificadores por hardware y la música los
 * necesita), en un hilo de baja prioridad y de a una canción por vez. Si algo falla, el
 * resultado queda como "sin análisis" y Automix usa la mezcla simple.
 */
final class AudioAnalyzer {

    /** Resultado en milisegundos desde el inicio de la canción. */
    static final class Result {
        final int contentStartMs;  // fin del silencio inicial
        final int contentEndMs;    // inicio del silencio final
        final int loudEndMs;       // último momento con volumen "normal" antes del final
        final float referenceDb;   // nivel de la parte fuerte del final (detección de fade-out)
        final float loudnessDb;    // sonoridad de la canción en LUFS aprox. (igualar volumen); NaN si no se pudo medir

        Result(int contentStartMs, int contentEndMs, int loudEndMs, float referenceDb, float loudnessDb) {
            this.contentStartMs = contentStartMs;
            this.contentEndMs = contentEndMs;
            this.loudEndMs = loudEndMs;
            this.referenceDb = referenceDb;
            this.loudnessDb = loudnessDb;
        }
    }

    // Umbrales (ajustables al probar con música real)
    static final float SILENCE_DB = -45f;     // por debajo: silencio
    static final float FADE_DROP_DB = 8f;     // bajó 8 dB respecto a la parte fuerte: ya se está apagando
    private static final int WINDOW_MS = 100;   // nivel medido en ventanas de 100 ms
    private static final int SMOOTH_WINDOWS = 10; // promedio de 1 s para ignorar golpes sueltos
    private static final int HEAD_MS = 10000;
    private static final int TAIL_MS = 25000;
    private static final int MID_MS = 10000;   // tramo del medio: solo para medir la sonoridad
    private static final int MAX_CONTENT_START_MS = 8000; // no saltar más de 8 s de "silencio" inicial
    private static final long MAX_ANALYSIS_MS = 10000;     // tiempo límite por canción
    private static final int CACHE_SIZE = 300;

    /** Marca de análisis fallido: evita reintentar la misma canción una y otra vez. */
    private static final Result FAILED = new Result(0, 0, 0, 0f, Float.NaN);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }, "jh-audio-analyzer");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Result> cache = new LinkedHashMap<String, Result>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private final Set<String> pending = new HashSet<>();

    /** Análisis listo para esa canción, o null si aún no está o falló. */
    Result get(String path) {
        if (path == null) return null;
        synchronized (cache) {
            Result r = cache.get(path);
            return r == FAILED ? null : r;
        }
    }

    /** Pide analizar la canción (si no está ya analizada o en cola). */
    void request(String path) {
        if (path == null) return;
        synchronized (cache) {
            if (cache.containsKey(path) || !pending.add(path)) return;
        }
        executor.execute(() -> {
            Result result;
            try {
                result = analyze(path);
            } catch (Throwable t) {
                result = null;
            }
            synchronized (cache) {
                pending.remove(path);
                cache.put(path, result != null ? result : FAILED);
            }
        });
    }

    void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------

    /** Niveles medidos de un tramo: dB de cada ventana y su instante de inicio. */
    private static final class Levels {
        final List<Float> db = new ArrayList<>();
        final List<Integer> startMs = new ArrayList<>();
        final List<Double> weightedPower = new ArrayList<>(); // potencia con ponderación K (sonoridad)
    }

    private static Result analyze(String path) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(path);
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    format = f;
                    break;
                }
            }
            if (format == null || !format.containsKey(MediaFormat.KEY_DURATION)) return null;
            long durationUs = format.getLong(MediaFormat.KEY_DURATION);
            if (durationUs <= (HEAD_MS + 5000) * 1000L) return null;

            codec = createSoftwareDecoder(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();
            long deadline = SystemClock.uptimeMillis() + MAX_ANALYSIS_MS;

            int[] pcm = {
                    format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100,
                    format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2
            };

            // Inicio de la canción
            Levels head = decodeRange(extractor, codec, 0, HEAD_MS * 1000L, pcm, deadline);
            if (head == null) return null;

            long tailStartUs = Math.max(HEAD_MS * 1000L, durationUs - TAIL_MS * 1000L);

            // Un tramo del medio (solo si la canción es larga): el inicio y el final suelen ser más
            // suaves que el cuerpo de la canción, y sin él la sonoridad saldría baja
            Levels mid = null;
            long midStartUs = durationUs / 2 - MID_MS * 500L;
            if (midStartUs >= HEAD_MS * 1000L && midStartUs + MID_MS * 1000L <= tailStartUs) {
                codec.flush();
                extractor.seekTo(midStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                mid = decodeRange(extractor, codec, midStartUs, midStartUs + MID_MS * 1000L, pcm, deadline);
                if (mid == null) return null;
            }

            // Final de la canción
            codec.flush();
            extractor.seekTo(tailStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            Levels tail = decodeRange(extractor, codec, tailStartUs, Long.MAX_VALUE, pcm, deadline);
            if (tail == null || tail.db.isEmpty()) return null;

            float loudness = gatedLoudness(head, mid, tail);
            return buildResult(head, tail, (int) (tailStartUs / 1000), loudness);
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (Exception ignored) {}
                try {
                    codec.release();
                } catch (Exception ignored) {}
            }
            extractor.release();
        }
    }

    /** Decodificador por software de Android; si no hay, el que ofrezca el sistema. */
    private static MediaCodec createSoftwareDecoder(String mime) throws Exception {
        MediaCodecInfo[] infos = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
        for (MediaCodecInfo info : infos) {
            if (info.isEncoder()) continue;
            String name = info.getName();
            if (!name.startsWith("OMX.google.") && !name.startsWith("c2.android.")) continue;
            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mime)) return MediaCodec.createByCodecName(name);
            }
        }
        return MediaCodec.createDecoderByType(mime);
    }

    /**
     * Decodifica desde startUs hasta endUs (o el final) y mide el nivel RMS en ventanas de 100 ms.
     * pcm = {frecuencia de muestreo, canales}, se actualiza si el decodificador lo informa.
     */
    private static Levels decodeRange(MediaExtractor extractor, MediaCodec codec, long startUs, long endUs,
                                      int[] pcm, long deadline) {
        Levels levels = new Levels();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        long windowUs = WINDOW_MS * 1000L;
        int currentWindow = -1;
        double sumSquares = 0;
        long samples = 0;
        double weightedSum = 0;   // suma de la potencia ponderada de todos los canales
        long frames = 0;
        KWeighting kFilter = null;

        while (true) {
            if (SystemClock.uptimeMillis() > deadline || Thread.currentThread().isInterrupted()) return null;

            if (!inputDone) {
                int in = codec.dequeueInputBuffer(10000);
                if (in >= 0) {
                    ByteBuffer buffer = codec.getInputBuffer(in);
                    int size = buffer != null ? extractor.readSampleData(buffer, 0) : -1;
                    long sampleTimeUs = extractor.getSampleTime();
                    if (size < 0 || sampleTimeUs > endUs) {
                        codec.queueInputBuffer(in, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        codec.queueInputBuffer(in, 0, size, sampleTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            int out = codec.dequeueOutputBuffer(info, 10000);
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat f = codec.getOutputFormat();
                if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) pcm[0] = f.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) pcm[1] = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            } else if (out >= 0) {
                ByteBuffer outBuffer = codec.getOutputBuffer(out);
                if (outBuffer != null && info.size > 0) {
                    outBuffer.position(info.offset);
                    outBuffer.limit(info.offset + info.size);
                    ShortBuffer shorts = outBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    int channels = Math.max(1, pcm[1]);
                    if (kFilter == null || !kFilter.matches(pcm[0], channels)) {
                        kFilter = new KWeighting(Math.max(8000, pcm[0]), channels);
                    }
                    int bufferFrames = shorts.remaining() / channels;
                    for (int frame = 0; frame < bufferFrames; frame++) {
                        long timeUs = info.presentationTimeUs + frame * 1000000L / Math.max(1, pcm[0]);
                        if (timeUs < startUs) {
                            shorts.position(shorts.position() + channels);
                            continue;
                        }
                        if (timeUs >= endUs) break;
                        int window = (int) ((timeUs - startUs) / windowUs);
                        if (window != currentWindow) {
                            if (currentWindow >= 0 && samples > 0) {
                                addWindow(levels, startUs, currentWindow, sumSquares, samples, weightedSum, frames);
                            }
                            currentWindow = window;
                            sumSquares = 0;
                            samples = 0;
                            weightedSum = 0;
                            frames = 0;
                        }
                        for (int c = 0; c < channels; c++) {
                            double s = shorts.get() / 32768.0;
                            sumSquares += s * s;
                            samples++;
                            double k = kFilter.process(c, s);
                            weightedSum += k * k;
                        }
                        frames++;
                    }
                }
                codec.releaseOutputBuffer(out, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
            }
        }
        if (currentWindow >= 0 && samples > 0) {
            addWindow(levels, startUs, currentWindow, sumSquares, samples, weightedSum, frames);
        }
        return levels;
    }

    /**
     * Ponderación K de ITU-R BS.1770 (la que usan EBU R128 y ReplayGain 2.0): resta peso a los bajos
     * y suma a los agudos, como el oído. Sin ella, una cumbia o un reguetón con mucho bajo
     * parecería más fuerte de lo que se oye. Dos filtros biquad por canal: muy poco cálculo.
     * Coeficientes para cualquier frecuencia de muestreo (fórmulas de libebur128).
     */
    private static final class KWeighting {
        private final int sampleRate;
        private final int channels;
        private final double sb0, sb1, sb2, sa1, sa2; // etapa 1: realce de agudos
        private final double hb0, hb1, hb2, ha1, ha2; // etapa 2: filtro pasa altos
        private final double[] z1, z2, z3, z4;

        KWeighting(int sampleRate, int channels) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            double fs = sampleRate;

            double f0 = 1681.974450955533;
            double gain = 3.999843853973347;
            double q = 0.7071752369554196;
            double k = Math.tan(Math.PI * f0 / fs);
            double vh = Math.pow(10.0, gain / 20.0);
            double vb = Math.pow(vh, 0.4996667741545416);
            double a0 = 1.0 + k / q + k * k;
            sb0 = (vh + vb * k / q + k * k) / a0;
            sb1 = 2.0 * (k * k - vh) / a0;
            sb2 = (vh - vb * k / q + k * k) / a0;
            sa1 = 2.0 * (k * k - 1.0) / a0;
            sa2 = (1.0 - k / q + k * k) / a0;

            f0 = 38.13547087602444;
            q = 0.5003270373238773;
            k = Math.tan(Math.PI * f0 / fs);
            double d = 1.0 + k / q + k * k;
            hb0 = 1.0;
            hb1 = -2.0;
            hb2 = 1.0;
            ha1 = 2.0 * (k * k - 1.0) / d;
            ha2 = (1.0 - k / q + k * k) / d;

            z1 = new double[channels];
            z2 = new double[channels];
            z3 = new double[channels];
            z4 = new double[channels];
        }

        boolean matches(int sampleRate, int channels) {
            return this.sampleRate == Math.max(8000, sampleRate) && this.channels == channels;
        }

        double process(int c, double x) {
            double y = sb0 * x + z1[c];
            z1[c] = sb1 * x - sa1 * y + z2[c];
            z2[c] = sb2 * x - sa2 * y;
            double out = hb0 * y + z3[c];
            z3[c] = hb1 * y - ha1 * out + z4[c];
            z4[c] = hb2 * y - ha2 * out;
            return out;
        }
    }

    /**
     * Sonoridad aproximada en LUFS, con las compuertas de EBU R128: se ignoran los silencios
     * (por debajo de −70) y los pasajes muy suaves (10 dB por debajo del promedio), para que una
     * intro o un final que se apaga no hagan parecer más baja la canción.
     */
    private static float gatedLoudness(Levels... parts) {
        double absoluteGate = Math.pow(10.0, (-70.0 + 0.691) / 10.0);
        double sum = 0;
        int count = 0;
        for (Levels part : parts) {
            if (part == null) continue;
            for (double p : part.weightedPower) {
                if (p > absoluteGate) {
                    sum += p;
                    count++;
                }
            }
        }
        if (count == 0) return Float.NaN;
        double relativeGate = (sum / count) * 0.1; // −10 dB
        double gatedSum = 0;
        int gatedCount = 0;
        for (Levels part : parts) {
            if (part == null) continue;
            for (double p : part.weightedPower) {
                if (p > absoluteGate && p >= relativeGate) {
                    gatedSum += p;
                    gatedCount++;
                }
            }
        }
        if (gatedCount == 0) return Float.NaN;
        return (float) (-0.691 + 10.0 * Math.log10(gatedSum / gatedCount));
    }

    private static void addWindow(Levels levels, long startUs, int window, double sumSquares, long samples,
                                  double weightedSum, long frames) {
        double rms = Math.sqrt(sumSquares / samples);
        float db = rms > 0 ? (float) (20 * Math.log10(rms)) : -100f;
        levels.db.add(db);
        levels.startMs.add((int) (startUs / 1000) + window * WINDOW_MS);
        levels.weightedPower.add(frames > 0 ? weightedSum / frames : 0.0);
    }

    private static Result buildResult(Levels head, Levels tail, int tailStartMs, float loudness) {
        // Inicio real: primera ventana con sonido
        int contentStart = 0;
        for (int i = 0; i < head.db.size(); i++) {
            if (head.db.get(i) > SILENCE_DB) {
                contentStart = head.startMs.get(i);
                break;
            }
        }
        contentStart = Math.min(contentStart, MAX_CONTENT_START_MS);

        // Fin real: última ventana con sonido
        int lastSound = -1;
        for (int i = tail.db.size() - 1; i >= 0; i--) {
            if (tail.db.get(i) > SILENCE_DB) {
                lastSound = i;
                break;
            }
        }
        if (lastSound < 0) {
            // Todo el final es silencio: se toma el inicio del tramo analizado
            return new Result(contentStart, tailStartMs, tailStartMs, SILENCE_DB, loudness);
        }
        int contentEnd = tail.startMs.get(lastSound) + WINDOW_MS;

        // Nivel suavizado (promedio de potencia de 1 s) y referencia = parte más fuerte del final
        int n = lastSound + 1;
        float[] smooth = new float[n];
        float reference = -100f;
        for (int i = 0; i < n; i++) {
            double power = 0;
            int from = Math.max(0, i - SMOOTH_WINDOWS + 1);
            for (int j = from; j <= i; j++) power += Math.pow(10, tail.db.get(j) / 10.0);
            smooth[i] = (float) (10 * Math.log10(power / (i - from + 1)));
            reference = Math.max(reference, smooth[i]);
        }

        // Fin de la parte fuerte: última vez que el nivel estuvo cerca de la referencia
        int loudEnd = contentEnd;
        for (int i = n - 1; i >= 0; i--) {
            if (smooth[i] >= reference - FADE_DROP_DB) {
                loudEnd = tail.startMs.get(i) + WINDOW_MS;
                break;
            }
        }
        return new Result(contentStart, contentEnd, Math.min(loudEnd, contentEnd), reference, loudness);
    }
}
