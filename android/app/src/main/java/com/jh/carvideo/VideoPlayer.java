package com.jh.carvideo;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reproductor de video de JH VIDEO.
 *
 * Usa ExoPlayer (Media3), que lee el archivo por su cuenta: sabe la duración y la posición y salta
 * adonde se mueve la barra. Los MP4 que bajan los descargadores de YouTube vienen fragmentados y sin
 * duración: el MediaPlayer del radio devolvía -1 y no saltaba. Para esos, {@link VideoExtractors}
 * lee la tabla que traen al final y hace que los saltos empiecen en un cuadro IDR.
 *
 * Si ExoPlayer no puede con un archivo (formato que no conoce, o video o audio que no puede
 * decodificar), ese archivo se abre con el MediaPlayer del radio, como antes.
 *
 * Todo corre en el hilo principal.
 */
@OptIn(markerClass = UnstableApi.class)
final class VideoPlayer {

    interface Listener {
        /** El archivo abrió y está listo para arrancar (todavía en pausa). */
        void onPrepared();

        void onCompletion();

        void onVideoSizeChanged(int width, int height);

        /** No se pudo abrir o se cayó a mitad. code: código del reproductor que falló. */
        void onError(String code);
    }

    private final Context context;
    private final Handler handler;
    private final Listener listener;

    private ExoPlayer exo;
    private MediaPlayer radioPlayer;   // solo para el archivo que ExoPlayer no pudo abrir
    private final Map<String, String> exoFailures = new HashMap<>(); // archivo → por qué; ya van directo al radio
    private Surface surface;
    private float volume = 1f;
    private String path;
    private int openId;                // cada open() invalida lo que quedó pendiente del archivo anterior
    private boolean prepared;
    private boolean ended;
    private boolean fallingBack;       // ExoPlayer ya falló con este archivo: no avisar "listo"
    private String radioReason;        // por qué este archivo lo abre el radio
    private String videoProblem;       // "video AV1": suena con ExoPlayer, pero el video no se puede mostrar
    private boolean released;

    VideoPlayer(Context context, Handler handler, Listener listener) {
        this.context = context.getApplicationContext();
        this.handler = handler;
        this.listener = listener;
    }

    /** Abre el archivo en segundo plano; avisa con onPrepared (o onError). Lanza excepción si el radio no lo puede abrir. */
    void open(String path) throws Exception {
        this.path = path;
        openId++;
        prepared = false;
        ended = false;
        fallingBack = false;
        radioReason = null;
        videoProblem = null;
        String failure = exoFailures.get(path);
        if (failure != null) {
            releaseExo();
            openWithRadio(path, failure);
            return;
        }
        releaseRadio();
        ensureExo();
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(new File(path))));
        exo.setPlayWhenReady(false);
        exo.prepare();
    }

    void start() {
        if (exo != null) {
            // Como MediaPlayer: play después de terminar vuelve a empezar
            if (exo.getPlaybackState() == Player.STATE_ENDED) exo.seekTo(0);
            exo.play();
        } else if (radioPlayer != null) {
            radioPlayer.start();
        }
    }

    void pause() {
        if (exo != null) {
            exo.pause();
        } else if (radioPlayer != null) {
            radioPlayer.pause();
        }
    }

    /** Reproduciendo o por reproducir (cargando después de un salto cuenta como reproduciendo). */
    boolean isPlaying() {
        if (exo != null) {
            int state = exo.getPlaybackState();
            return exo.getPlayWhenReady() && state != Player.STATE_ENDED && state != Player.STATE_IDLE;
        }
        return radioPlayer != null && radioPlayer.isPlaying();
    }

    void seekTo(int ms) {
        if (exo != null) {
            exo.seekTo(ms);
        } else if (radioPlayer != null) {
            radioPlayer.seekTo(ms);
        }
    }

    /** En pausa, vuelve a dibujar el cuadro de ms (la superficie nueva queda negra si no). */
    void redraw(int ms) {
        if (exo != null) {
            // Al cuadro exacto: con el cuadro clave más cercano el video se movería unos segundos
            exo.setSeekParameters(SeekParameters.EXACT);
            exo.seekTo(ms);
            exo.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        } else if (radioPlayer != null) {
            radioPlayer.seekTo(ms);
        }
    }

    /**
     * El archivo deja saltar. Sin índice (algunos MKV, FLV o AVI) ExoPlayer no puede, y si se le pide
     * igual vuelve al inicio. Con el radio no se sabe hasta probar.
     */
    boolean canSeek() {
        if (exo != null) return !prepared || exo.isCurrentMediaItemSeekable();
        return true;
    }

    int getCurrentPosition() {
        if (exo != null) return (int) exo.getCurrentPosition();
        return radioPlayer != null ? radioPlayer.getCurrentPosition() : 0;
    }

    /** Duración en ms; -1 si no se sabe. */
    int getDuration() {
        if (exo != null) {
            long d = exo.getDuration();
            return d == C.TIME_UNSET ? -1 : (int) d;
        }
        return radioPlayer != null ? radioPlayer.getDuration() : -1;
    }

    /** Dónde se dibuja el video (null: en ningún lado, el sonido sigue). */
    void setSurface(Surface surface) {
        this.surface = surface;
        if (exo != null) {
            // Si reemplaza a otra, ExoPlayer espera a soltarla: después se puede liberar sin peligro
            exo.setVideoSurface(surface);
        } else if (radioPlayer != null && prepared) {
            // Mientras abre, la superficie se pone al quedar listo
            radioPlayer.setSurface(surface);
        }
    }

    void setVolume(float volume) {
        this.volume = volume;
        if (exo != null) {
            exo.setVolume(volume);
        } else if (radioPlayer != null) {
            radioPlayer.setVolume(volume, volume);
        }
    }

    /** Este archivo lo abrió el MediaPlayer del radio (ExoPlayer no pudo). */
    boolean usesRadioPlayer() {
        return radioPlayer != null;
    }

    /** Por qué este archivo lo abre el radio (null si lo abre ExoPlayer). */
    String getRadioReason() {
        return radioPlayer != null ? radioReason : null;
    }

    /** "video AV1" si el archivo suena pero su video no se puede mostrar en este radio; null si se ve. */
    String getVideoProblem() {
        return exo != null ? videoProblem : null;
    }

    void release() {
        released = true;
        openId++;
        releaseExo();
        releaseRadio();
    }

    // ------------------------------------------------------------------
    // ExoPlayer

    private void ensureExo() {
        if (exo != null) return;
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(context)
                // Si el decodificador principal no arranca, prueba con otro del radio
                .setEnableDecoderFallback(true);
        exo = new ExoPlayer.Builder(context, renderers)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(context, VideoExtractors.create()))
                .build();
        // Salta al cuadro clave más cercano (cada 2 a 6 s en estos videos), que es inmediato. Ir al cuadro
        // exacto obliga a decodificar los segundos de en medio: en un radio lento el salto tarda
        exo.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        // Mismo canal que la música (STREAM_MUSIC). Sin pedir el foco de audio, como el MediaPlayer de antes
        exo.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), false);
        exo.setVolume(volume);
        exo.setVideoSurface(surface);
        exo.addListener(exoListener);
    }

    private final Player.Listener exoListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (fallingBack) return;
            if (!prepared) {
                if (state == Player.STATE_READY) {
                    // Abrió, pero quizás sin poder decodificar el audio o el video
                    Tracks tracks = exo.getCurrentTracks();
                    boolean hasVideo = tracks.containsType(C.TRACK_TYPE_VIDEO);
                    boolean hasAudio = tracks.containsType(C.TRACK_TYPE_AUDIO);
                    String video = unsupported(tracks, C.TRACK_TYPE_VIDEO);
                    String audio = unsupported(tracks, C.TRACK_TYPE_AUDIO);
                    // Un .avi con códecs que ExoPlayer no conoce queda sin pistas
                    String problem = !hasVideo && !hasAudio ? "no reconoce sus pistas"
                            : audio != null ? "no decodifica " + audio
                            : video != null && !hasAudio ? "no decodifica " + video
                            : null;
                    if (problem != null) {
                        // El radio quizás sí puede
                        fallingBack = true;
                        postFallback(problem);
                        return;
                    }
                    // Video que el radio no decodifica con audio que sí: el reproductor del radio tampoco
                    // lo muestra (en el auto, el AV1 sonaba en negro) y además no saltaba. Suena aquí
                    videoProblem = video;
                    prepared = true;
                    listener.onPrepared();
                } else if (state == Player.STATE_ENDED) {
                    // Terminó sin llegar a estar listo: no encontró nada que mostrar ni que sonar
                    fallingBack = true;
                    postFallback("formato que no conoce");
                }
            } else if (state == Player.STATE_ENDED) {
                if (!ended) {
                    ended = true;
                    listener.onCompletion();
                }
            } else {
                ended = false; // volvió a sonar después de terminar (Mi Lista se detiene al inicio)
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize size) {
            if (fallingBack) return;
            int width = size.width;
            // Píxeles no cuadrados (DVD, algunos .ts): el ancho que se ve
            if (size.pixelWidthHeightRatio > 0f && size.pixelWidthHeightRatio != 1f) {
                width = Math.round(width * size.pixelWidthHeightRatio);
            }
            listener.onVideoSizeChanged(width, size.height);
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            String reason = errorText(error);
            if (!prepared) {
                // No llegó a abrir: lo intenta el reproductor del radio
                if (!fallingBack) {
                    fallingBack = true;
                    postFallback(reason);
                }
            } else {
                // Se cayó a mitad (pendrive retirado, decodificador caído)
                final int id = openId;
                handler.post(() -> {
                    if (id == openId && !released) listener.onError(reason);
                });
            }
        }
    };

    /** Error de ExoPlayer en palabras para el aviso de arriba, con su código para saber cuál fue. */
    private static String errorText(PlaybackException error) {
        String text;
        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED:
                text = "formato que no conoce";
                break;
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
                text = "archivo dañado";
                break;
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
            case PlaybackException.ERROR_CODE_IO_NO_PERMISSION:
                text = "no puede leer el archivo";
                break;
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED:
                text = "no arrancó el decodificador";
                break;
            case PlaybackException.ERROR_CODE_DECODING_FAILED:
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES:
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED:
                text = "el decodificador no puede con este video";
                break;
            default:
                text = "ExoPlayer falló";
                break;
        }
        return text + " " + error.errorCode;
    }

    /** "video AV1": la pista de ese tipo que ExoPlayer no puede decodificar; null si puede o no hay. */
    private static String unsupported(Tracks tracks, int type) {
        if (!tracks.containsType(type) || tracks.isTypeSupported(type, true)) return null;
        String mime = null;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != type || group.length == 0) continue;
            Format format = group.getTrackFormat(0);
            mime = format.sampleMimeType != null ? format.sampleMimeType : format.codecs;
            break;
        }
        return (type == C.TRACK_TYPE_VIDEO ? "video " : "audio ") + codecName(mime);
    }

    /** Nombre corto del códec para el aviso de arriba. */
    private static String codecName(String mime) {
        if (mime == null) return "?";
        switch (mime) {
            case "video/av01": return "AV1";
            case "video/hevc": return "H.265";
            case "video/avc": return "H.264";
            case "video/mp4v-es": return "MPEG-4";
            case "video/x-vnd.on2.vp9": return "VP9";
            case "video/x-vnd.on2.vp8": return "VP8";
            case "video/mpeg2": return "MPEG-2";
            case "audio/ac3": return "AC3";
            case "audio/eac3": return "E-AC3";
            case "audio/vnd.dts": return "DTS";
            case "audio/mpeg": return "MP3";
            case "audio/mp4a-latm": return "AAC";
            default:
                int slash = mime.indexOf('/');
                return (slash >= 0 ? mime.substring(slash + 1) : mime).toUpperCase(Locale.US);
        }
    }

    /** Fuera del aviso de ExoPlayer: liberarlo dentro de su propio aviso no es seguro. */
    private void postFallback(String reason) {
        final int id = openId;
        handler.post(() -> {
            if (id != openId || released) return;
            exoFailures.put(path, reason);
            releaseExo(); // suelta la superficie antes de que la tome el reproductor del radio
            try {
                openWithRadio(path, reason);
            } catch (Exception e) {
                releaseRadio();
                listener.onError(reason + " · radio: " + e.getClass().getSimpleName());
            }
        });
    }

    private void releaseExo() {
        if (exo == null) return;
        ExoPlayer old = exo;
        exo = null;
        try {
            old.removeListener(exoListener);
            old.release();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------
    // MediaPlayer del radio (respaldo)

    private void openWithRadio(String path, String reason) throws Exception {
        releaseRadio();
        fallingBack = false;
        radioReason = reason;
        MediaPlayer mp = new MediaPlayer();
        radioPlayer = mp;
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(p -> {
            if (p != radioPlayer) return;
            prepared = true;
            try {
                p.setSurface(surface);
            } catch (Exception ignored) {}
            listener.onPrepared();
        });
        mp.setOnCompletionListener(p -> {
            if (p == radioPlayer) listener.onCompletion();
        });
        mp.setOnVideoSizeChangedListener((p, width, height) -> {
            if (p == radioPlayer) listener.onVideoSizeChanged(width, height);
        });
        // Con el error manejado aquí no se llama a "terminó" (antes saltaba al siguiente sin avisar)
        mp.setOnErrorListener((p, what, extra) -> {
            if (p == radioPlayer) {
                releaseRadio();
                // Lo que dijo ExoPlayer explica más que el código del radio
                listener.onError(reason + " · radio " + what + "/" + extra);
            }
            return true;
        });
        mp.setDataSource(path);
        mp.setSurface(surface);
        mp.setVolume(volume, volume);
        mp.prepareAsync();
    }

    private void releaseRadio() {
        if (radioPlayer == null) return;
        MediaPlayer old = radioPlayer;
        radioPlayer = null;
        try {
            old.release();
        } catch (Exception ignored) {}
    }
}
