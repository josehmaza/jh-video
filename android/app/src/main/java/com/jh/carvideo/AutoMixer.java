package com.jh.carvideo;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;

/**
 * Mezcla automática (crossfade) entre la canción que suena y la siguiente de la cola.
 *
 * - Precarga la siguiente canción con anticipación (prepareAsync) para que un pendrive lento
 *   no deje silencio.
 * - Mezcla con curva de igual potencia (cos/sin): el volumen percibido no baja a mitad de la mezcla.
 * - Si la canción termina antes de lo previsto (duración mal informada en MP3 VBR), la siguiente
 *   ya preparada entra al instante.
 * - Iguala el volumen entre canciones (fase 3, {@link LoudnessLeveler}): la que entra ya llega
 *   con su corrección; si el análisis llega tarde, se ajusta con una rampa suave al inicio.
 *
 * Todo corre en el hilo principal (Handler); los callbacks de MediaPlayer llegan a ese mismo hilo.
 */
final class AutoMixer {

    /** Lo que el mezclador necesita de la Activity. */
    interface Host {
        MediaPlayer getCurrentPlayer();

        /** Reproduciendo de verdad (no en pausa ni arrastrando la barra de progreso). */
        boolean isPlaybackActive();

        /** Siguiente canción de la cola, o null si no corresponde mezclar (repetir, cola de 1...). */
        NextTrack pickNextTrack();

        /** La siguiente elegida sigue siendo válida (la cola no cambió desde la precarga). */
        boolean isStillNext(NextTrack next);

        /** Reproductor nuevo con los mismos listeners que el principal. */
        MediaPlayer createPlayer();

        /**
         * La siguiente empezó a sonar: pasa a ser el reproductor principal y se actualiza la pantalla.
         * automatic = mezcla automática (la pantalla cambia con fundido suave); false = cambio a mano.
         */
        void onMixStarted(MediaPlayer incoming, NextTrack next, boolean automatic);

        /** Empieza o termina una mezcla automática (para mostrar el borde neón del reproductor). */
        void onMixVisualChanged(boolean mixing);
    }

    static final class NextTrack {
        final int index;
        final Track track;

        NextTrack(int index, Track track) {
            this.index = index;
            this.track = track;
        }
    }

    static final int DEFAULT_MIX_MS = 5000;
    private static final int PRELOAD_LEAD_MS = 15000;     // precargar 15 s antes de empezar la mezcla
    // Mezcla inteligente (con análisis del audio)
    private static final int FADE_MIN_MS = 1500;          // si la canción se apaga durante más de esto: es un fade-out
    private static final int MIN_MIX_MS = 2000;
    private static final int ABRUPT_MIX_MS = 3000;        // final seco: mezcla corta para no encimar dos canciones fuertes
    private static final int MIN_SKIP_INTRO_MS = 300;     // silencio inicial menor a esto no se salta
    private static final int LONG_TRACK_MS = 10 * 60 * 1000; // mixes largos (sets de DJ)
    private static final int LONG_TRACK_MIX_MS = 2000;
    private static final int EARLY_END_FADE_MS = 300;     // entrada al terminar antes de lo previsto
    static final int QUICK_FADE_MS = 500;                 // Siguiente / Anterior a mano: cambio suave pero inmediato
    private static final int TICK_MS = 250;
    private static final int FADE_STEP_MS = 50;           // pasos más largos se oyen "escalonados"
    private static final int GAIN_RAMP_MS = 2000;          // corrección de volumen que llega tarde: rampa suave
    private static final int LATE_GAIN_WINDOW_MS = 20000;  // pasado esto la canción sigue como está (sin saltos a mitad)

    private final Handler handler;
    private final Host host;
    private final AudioAnalyzer analyzer;
    private final LoudnessLeveler leveler = new LoudnessLeveler();

    private boolean enabled = false;
    private String currentPath;          // canción principal (para buscar su análisis)
    private int mixDurationMs = DEFAULT_MIX_MS;

    // Precarga de la siguiente
    private MediaPlayer nextPlayer;
    private NextTrack nextTrack;
    private boolean nextReady = false;
    private MediaPlayer preloadedFor;   // reproductor principal para el que se hizo (o intentó) la precarga

    // Mezcla en curso
    private MediaPlayer outgoing;
    private long fadeStartMs;
    private int fadeDurationMs;
    private boolean visibleMix = false;  // mezcla automática (se muestra el borde neón); el fundido de Siguiente no

    // Igualar volumen
    private String pendingGainPath;      // canción que espera su análisis para corregir el volumen
    private long pendingGainSinceMs;
    private MediaPlayer rampPlayer;
    private float rampFromDb;
    private float rampToDb;
    private long rampStartMs;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private final Runnable fadeRunnable = new Runnable() {
        @Override
        public void run() {
            stepFade();
        }
    };

    private final Runnable gainRampRunnable = new Runnable() {
        @Override
        public void run() {
            stepGainRamp();
        }
    };

    AutoMixer(Handler handler, Host host, AudioAnalyzer analyzer) {
        this.handler = handler;
        this.host = host;
        this.analyzer = analyzer;
    }

    boolean isEnabled() {
        return enabled;
    }

    /** Empezó a sonar una canción (a mano o por mezcla): analizar su final en segundo plano. */
    void onTrackStarted(Track track) {
        currentPath = track != null ? track.getFilePath() : null;
        cancelGainRamp();
        pendingGainPath = null;
        MediaPlayer current = host.getCurrentPlayer();
        if (!enabled) {
            setGainNow(current, 0f); // el reproductor reutilizado puede traer la corrección de otra canción
            return;
        }
        analyzer.request(currentPath);
        AudioAnalyzer.Result analysis = analyzer.get(currentPath);
        // Ya analizada (sonó antes o se precargó): su volumen se corrige desde el inicio
        setGainNow(current, LoudnessLeveler.gainFor(analysis));
        if (analysis == null) waitForGain(currentPath);
    }

    void setEnabled(boolean on) {
        enabled = on;
        handler.removeCallbacks(tickRunnable);
        if (on) {
            analyzer.request(currentPath);
            waitForGain(currentPath); // la que suena también se iguala, con una rampa suave
            handler.post(tickRunnable);
        } else {
            finishMixNow();
            cancelPreload();
            pendingGainPath = null;
            startGainRamp(host.getCurrentPlayer(), 0f); // vuelve a su volumen original, sin salto
        }
    }

    boolean isMixing() {
        return outgoing != null;
    }

    boolean isOutgoing(MediaPlayer mp) {
        return mp != null && mp == outgoing;
    }

    /** Reproducción iniciada a mano (tocar canción, Siguiente/Anterior...): descartar mezcla y precarga. */
    void onManualPlayback() {
        finishMixNow();
        cancelPreload();
    }

    /** Cambió algo que afecta cuál es la siguiente (aleatorio, repetir, cola). */
    void invalidateNext() {
        cancelPreload();
    }

    /** La canción que salía terminó antes que el fundido. */
    void onOutgoingCompleted() {
        endMix();
    }

    /**
     * La canción principal terminó y no hubo mezcla (duración mal informada o mezcla no aplicable).
     * Si la siguiente ya está preparada, la arranca al instante. Devuelve false si no había nada listo.
     */
    boolean startPreparedNextNow() {
        MediaPlayer current = host.getCurrentPlayer();
        if (!enabled || !nextReady || nextPlayer == null || !host.isStillNext(nextTrack)) {
            cancelPreload();
            return false;
        }
        startMix(current, EARLY_END_FADE_MS, false);
        return true;
    }

    /**
     * Siguiente a mano cuando la próxima canción ya está precargada: se usa directo (cambio
     * instantáneo aunque el pendrive sea lento) con un fundido corto. False si no había nada listo.
     */
    boolean skipToPreparedNext() {
        MediaPlayer current = host.getCurrentPlayer();
        if (!enabled || isMixing() || current == null || !nextReady || nextPlayer == null
                || !host.isStillNext(nextTrack)) {
            return false;
        }
        startMix(current, QUICK_FADE_MS, false);
        return true;
    }

    /**
     * Fundido corto a mano (Siguiente / Anterior): la Activity ya dejó {@code incoming} preparado y
     * como reproductor principal; aquí arranca en volumen 0 y se cruza con {@code old}.
     */
    boolean fadeManual(MediaPlayer old, MediaPlayer incoming) {
        finishMixNow();
        try {
            incoming.setVolume(0f, 0f);
            incoming.start();
        } catch (Exception e) {
            return false;
        }
        beginFade(old, QUICK_FADE_MS, false);
        return true;
    }

    /** Termina la mezcla en curso de inmediato: queda solo la canción nueva, a volumen completo. */
    void finishMixNow() {
        handler.removeCallbacks(fadeRunnable);
        if (outgoing != null) {
            releaseQuietly(outgoing);
            outgoing = null;
        }
        setVisibleMix(false);
        MediaPlayer current = host.getCurrentPlayer();
        if (current != null) {
            try {
                setPlayerVolume(current, 1f);
            } catch (Exception ignored) {}
        }
    }

    void release() {
        handler.removeCallbacks(tickRunnable);
        cancelGainRamp();
        finishMixNow();
        cancelPreload();
        leveler.releaseAll();
    }

    /** La Activity va a liberar este reproductor: soltar su corrección de volumen. */
    void forgetPlayer(MediaPlayer mp) {
        if (mp == rampPlayer) cancelGainRamp();
        leveler.release(mp);
    }

    // ------------------------------------------------------------------

    private void tick() {
        if (!enabled) return;
        checkPendingGain();
        if (isMixing()) return;
        MediaPlayer current = host.getCurrentPlayer();
        if (current == null || !host.isPlaybackActive()) return;

        int durationMs;
        int positionMs;
        try {
            durationMs = current.getDuration();
            positionMs = current.getCurrentPosition();
        } catch (Exception e) {
            return;
        }
        if (durationMs <= 0) return;

        int baseMixMs = mixDurationFor(durationMs, 0);
        if (baseMixMs == 0) return;
        int[] plan = planMix(durationMs, baseMixMs);
        int mixStartMs = plan[0];
        int mixMs = plan[1];

        if (preloadedFor != current && positionMs >= mixStartMs - PRELOAD_LEAD_MS) {
            preload(current);
        }

        if (nextReady && positionMs >= mixStartMs) {
            if (!host.isStillNext(nextTrack)) {
                cancelPreload();
                return;
            }
            int nextDurationMs = 0;
            try {
                nextDurationMs = nextPlayer.getDuration();
            } catch (Exception ignored) {}
            if (mixDurationFor(durationMs, nextDurationMs) == 0) return; // la siguiente es muy corta: entrará al terminar (respaldo)
            int remainingMs = durationMs - positionMs;
            startMix(current, Math.max(EARLY_END_FADE_MS, Math.min(mixMs, remainingMs)), true);
        }
    }

    /**
     * Dónde empieza la mezcla y cuánto dura: {inicio ms, duración ms}.
     * Con análisis: mezcla donde la canción empieza a apagarse (fade-out) o justo antes de su fin real
     * (ignorando el silencio final). Sin análisis: los últimos segundos del archivo.
     */
    private int[] planMix(int durationMs, int baseMixMs) {
        AudioAnalyzer.Result analysis = analyzer.get(currentPath);
        if (analysis == null || analysis.contentEndMs <= 0) {
            return new int[]{durationMs - baseMixMs, baseMixMs};
        }
        int endMs = Math.min(analysis.contentEndMs, durationMs);
        int startMs;
        int mixMs;
        if (analysis.loudEndMs < endMs - FADE_MIN_MS) {
            // Se apaga: mezclar desde que ya bajó claramente, sin esperar al silencio
            startMs = analysis.loudEndMs;
            mixMs = Math.max(MIN_MIX_MS, Math.min(baseMixMs, endMs - startMs));
        } else {
            // Final seco: mezcla corta que termina en el fin real
            mixMs = Math.min(baseMixMs, ABRUPT_MIX_MS);
            startMs = endMs - mixMs;
        }
        return new int[]{Math.max(0, startMs), mixMs};
    }

    /** Reglas de duración de la mezcla; 0 = no mezclar. */
    private int mixDurationFor(int currentDurationMs, int nextDurationMs) {
        int mix = mixDurationMs;
        if (currentDurationMs >= LONG_TRACK_MS) mix = Math.min(mix, LONG_TRACK_MIX_MS);
        if (currentDurationMs < mix * 3) return 0;               // la mezcla se comería la canción
        if (nextDurationMs > 0 && nextDurationMs < mix * 3) return 0;
        return mix;
    }

    private void preload(MediaPlayer forPlayer) {
        cancelPreload();
        preloadedFor = forPlayer; // aunque falle, no reintentar en cada tick para esta misma canción
        NextTrack next = host.pickNextTrack();
        if (next == null) return;
        analyzer.request(next.track.getFilePath()); // para saltar su silencio inicial al entrar

        final MediaPlayer player = host.createPlayer();
        nextPlayer = player;
        nextTrack = next;
        try {
            player.setDataSource(next.track.getFilePath());
            player.setOnPreparedListener(mp -> {
                if (mp == nextPlayer) nextReady = true;
            });
            player.setOnErrorListener((mp, what, extra) -> {
                if (mp == nextPlayer) {
                    // Archivo dañado o pendrive retirado: sin mezcla, el respaldo pasará a la siguiente
                    discardNextPlayer();
                    return true;
                }
                // Ya es el reproductor principal: comportamiento normal (onCompletion pasa a la siguiente)
                return false;
            });
            player.prepareAsync();
        } catch (Exception e) {
            discardNextPlayer();
        }
    }

    private void startMix(MediaPlayer current, int durationMs, boolean visible) {
        MediaPlayer incoming = nextPlayer;
        NextTrack next = nextTrack;
        nextPlayer = null;
        nextTrack = null;
        nextReady = false;

        try {
            incoming.setVolume(0f, 0f);
            // Saltar el silencio inicial de la que entra, para que no haya hueco
            AudioAnalyzer.Result nextAnalysis = next != null ? analyzer.get(next.track.getFilePath()) : null;
            if (nextAnalysis != null && nextAnalysis.contentStartMs >= MIN_SKIP_INTRO_MS) {
                incoming.seekTo(nextAnalysis.contentStartMs);
            }
            // Entra ya con su volumen igualado (en silencio, así el cambio no se oye)
            leveler.setGain(incoming, LoudnessLeveler.gainFor(nextAnalysis));
            incoming.start();
        } catch (Exception e) {
            releaseQuietly(incoming);
            return;
        }

        preloadedFor = null;
        // Primero el fundido (queda "mezclando") y después el cambio de canción: así, al actualizar
        // la pantalla, nadie le pone volumen completo a la que entra antes del primer paso del fundido
        beginFade(current, durationMs, visible);
        host.onMixStarted(incoming, next, visible);
    }

    /** Cruza volúmenes entre {@code old} (sale) y el reproductor principal (entra). */
    private void beginFade(MediaPlayer old, int durationMs, boolean visible) {
        outgoing = old;
        fadeStartMs = SystemClock.uptimeMillis();
        fadeDurationMs = durationMs;
        setVisibleMix(visible);
        handler.removeCallbacks(fadeRunnable);
        handler.post(fadeRunnable);
    }

    /** Lo que falta de la mezcla automática en curso (0 si no hay): el fondo cambia de color en ese tiempo. */
    int visibleMixRemainingMs() {
        if (!visibleMix || outgoing == null) return 0;
        long left = fadeDurationMs - (SystemClock.uptimeMillis() - fadeStartMs);
        return (int) Math.max(0, left);
    }

    private void setVisibleMix(boolean visible) {
        if (visibleMix == visible) return;
        visibleMix = visible;
        host.onMixVisualChanged(visible);
    }

    private void stepFade() {
        MediaPlayer incoming = host.getCurrentPlayer();
        if (outgoing == null || incoming == null) {
            finishMixNow();
            return;
        }
        float t = Math.min(1f, (SystemClock.uptimeMillis() - fadeStartMs) / (float) fadeDurationMs);
        // Igual potencia: cos²+sin²=1, sin hueco de volumen a mitad de la mezcla
        float outVolume = (float) Math.cos(t * Math.PI / 2);
        float inVolume = (float) Math.sin(t * Math.PI / 2);
        try {
            setPlayerVolume(outgoing, outVolume);
            setPlayerVolume(incoming, inVolume);
        } catch (Exception ignored) {}

        if (t >= 1f) {
            endMix();
        } else {
            handler.postDelayed(fadeRunnable, FADE_STEP_MS);
        }
    }

    private void endMix() {
        handler.removeCallbacks(fadeRunnable);
        if (outgoing != null) {
            releaseQuietly(outgoing);
            outgoing = null;
        }
        setVisibleMix(false);
        MediaPlayer current = host.getCurrentPlayer();
        if (current != null) {
            try {
                setPlayerVolume(current, 1f);
            } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------------
    // Igualar volumen (fase 3)

    /** Volumen del reproductor (0-1) aplicándole su corrección de volumen. */
    private void setPlayerVolume(MediaPlayer mp, float volume) {
        float v = volume * leveler.volumeFactor(mp);
        mp.setVolume(v, v);
    }

    /**
     * Fija la corrección de volumen. Si no hay mezcla en curso, se aplica ya; durante una mezcla
     * la aplica el siguiente paso del fundido.
     */
    private void setGainNow(MediaPlayer mp, float gainDb) {
        if (mp == null) return;
        leveler.setGain(mp, gainDb);
        if (!isMixing() && mp == host.getCurrentPlayer()) {
            try {
                setPlayerVolume(mp, 1f);
            } catch (Exception ignored) {}
        }
    }

    /** La canción que suena aún no tiene análisis: corregirla apenas llegue (si es pronto). */
    private void waitForGain(String path) {
        pendingGainPath = path;
        pendingGainSinceMs = SystemClock.uptimeMillis();
    }

    private void checkPendingGain() {
        if (pendingGainPath == null) return;
        if (!pendingGainPath.equals(currentPath)
                || SystemClock.uptimeMillis() - pendingGainSinceMs > LATE_GAIN_WINDOW_MS) {
            pendingGainPath = null; // muy tarde: un cambio de volumen a mitad de canción se notaría
            return;
        }
        AudioAnalyzer.Result analysis = analyzer.get(pendingGainPath);
        if (analysis == null) return;
        pendingGainPath = null;
        startGainRamp(host.getCurrentPlayer(), LoudnessLeveler.gainFor(analysis));
    }

    /** Lleva la corrección del reproductor hasta toDb en 2 s, para que el cambio no se note. */
    private void startGainRamp(MediaPlayer mp, float toDb) {
        cancelGainRamp();
        if (mp == null || Math.abs(leveler.getGain(mp) - toDb) < 0.1f) return;
        rampPlayer = mp;
        rampFromDb = leveler.getGain(mp);
        rampToDb = toDb;
        rampStartMs = SystemClock.uptimeMillis();
        handler.post(gainRampRunnable);
    }

    private void stepGainRamp() {
        MediaPlayer mp = rampPlayer;
        if (mp == null || mp != host.getCurrentPlayer()) {
            rampPlayer = null; // cambió la canción: la nueva trae su propia corrección
            return;
        }
        float t = Math.min(1f, (SystemClock.uptimeMillis() - rampStartMs) / (float) GAIN_RAMP_MS);
        setGainNow(mp, rampFromDb + (rampToDb - rampFromDb) * t);
        if (t < 1f) {
            handler.postDelayed(gainRampRunnable, FADE_STEP_MS);
        } else {
            rampPlayer = null;
        }
    }

    private void cancelGainRamp() {
        handler.removeCallbacks(gainRampRunnable);
        rampPlayer = null;
    }

    private void cancelPreload() {
        discardNextPlayer();
        preloadedFor = null;
    }

    private void discardNextPlayer() {
        if (nextPlayer != null) {
            releaseQuietly(nextPlayer);
            nextPlayer = null;
        }
        nextTrack = null;
        nextReady = false;
    }

    private void releaseQuietly(MediaPlayer mp) {
        leveler.release(mp);
        try {
            mp.release();
        } catch (Exception ignored) {}
    }
}
