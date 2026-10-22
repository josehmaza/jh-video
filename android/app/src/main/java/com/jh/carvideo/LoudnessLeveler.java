package com.jh.carvideo;

import android.media.MediaPlayer;
import android.media.audiofx.LoudnessEnhancer;

import java.util.HashMap;
import java.util.Map;

/**
 * Igualar volumen entre canciones (Automix, fase 3).
 *
 * Con la sonoridad que mide {@link AudioAnalyzer}, cada canción se lleva hacia un mismo nivel:
 * - Las fuertes se BAJAN con setVolume() del reproductor (no cuesta nada de procesamiento).
 * - Las suaves se SUBEN con LoudnessEnhancer de Android, que trae su propio limitador para no
 *   saturar. Solo se crea si la canción necesita subir.
 *
 * La corrección tiene tope (−6 dB / +4 dB): subir mucho distorsiona en los parlantes del auto.
 * Si el radio no permite LoudnessEnhancer, las canciones suaves simplemente no se suben.
 */
final class LoudnessLeveler {

    /** Nivel al que se llevan todas las canciones (LUFS aprox.). */
    static final float TARGET_LUFS = -12f;
    static final float MAX_CUT_DB = 6f;
    static final float MAX_BOOST_DB = 4f;

    private static final class State {
        float gainDb;
        LoudnessEnhancer enhancer;
        boolean enhancerFailed;
    }

    private final Map<MediaPlayer, State> states = new HashMap<>();

    /** Corrección para una canción según su análisis; 0 si no hay medida. */
    static float gainFor(AudioAnalyzer.Result analysis) {
        if (analysis == null || Float.isNaN(analysis.loudnessDb)) return 0f;
        float gain = TARGET_LUFS - analysis.loudnessDb;
        return Math.max(-MAX_CUT_DB, Math.min(MAX_BOOST_DB, gain));
    }

    float getGain(MediaPlayer mp) {
        State s = states.get(mp);
        return s != null ? s.gainDb : 0f;
    }

    /** Fija la corrección del reproductor. La parte de "subir" se aplica aquí; la de "bajar", con {@link #volumeFactor}. */
    void setGain(MediaPlayer mp, float gainDb) {
        if (mp == null) return;
        State s = states.get(mp);
        if (s == null) {
            if (Math.abs(gainDb) < 0.05f) return; // sin corrección: no hace falta guardar nada
            s = new State();
            states.put(mp, s);
        }
        s.gainDb = gainDb;
        float boostDb = Math.max(0f, gainDb);
        if (boostDb > 0f && s.enhancer == null && !s.enhancerFailed) {
            try {
                s.enhancer = new LoudnessEnhancer(mp.getAudioSessionId());
            } catch (Throwable t) {
                s.enhancerFailed = true; // el radio no lo soporta: la canción queda sin subir
            }
        }
        if (s.enhancer != null) {
            try {
                s.enhancer.setTargetGain(Math.round(boostDb * 100)); // milibeles
                s.enhancer.setEnabled(boostDb > 0f);
            } catch (Throwable ignored) {}
        }
    }

    /** Factor para setVolume() (0-1): la parte de la corrección que baja el volumen. */
    float volumeFactor(MediaPlayer mp) {
        float gain = getGain(mp);
        return gain < 0f ? (float) Math.pow(10.0, gain / 20.0) : 1f;
    }

    /** El reproductor se va a liberar: soltar su LoudnessEnhancer. */
    void release(MediaPlayer mp) {
        State s = states.remove(mp);
        if (s != null && s.enhancer != null) {
            try {
                s.enhancer.release();
            } catch (Throwable ignored) {}
        }
    }

    void releaseAll() {
        for (State s : states.values()) {
            if (s.enhancer != null) {
                try {
                    s.enhancer.release();
                } catch (Throwable ignored) {}
            }
        }
        states.clear();
    }
}
