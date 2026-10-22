package com.jh.carvideo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;

/**
 * Fondo de toda la app con el color de la carátula (como YouTube Music), igual que la web:
 *
 *   radial-gradient(at 78% 4%, color 0.80 → transparente)   más intenso detrás de la carátula
 *   linear-gradient(arriba → abajo, color 0.50 → 0.18 → #07090c)
 *
 * El color se saca de la carátula reducida a 24x24 (promedio que pesa más los colores vivos), se
 * oscurece y se limita para que el texto blanco siempre se lea. Sin carátula: azul petróleo.
 * El cambio entre canciones es un fundido de 1,2 s; en una mezcla de Automix dura lo que la mezcla.
 * El resplandor de la carátula (AlbumGlowView) usa el mismo color, más claro.
 * Animado con Handler (no Animator), para radios con las animaciones del sistema desactivadas.
 */
final class AmbientTheme {

    static final int DEFAULT_FADE_MS = 1200;
    private static final long FRAME_MS = 33;
    private static final int GLOW_EVERY_FRAMES = 3;   // el resplandor (difuminado) se redibuja menos seguido

    private static final float[] THEME_DEFAULT = {23, 60, 92};    // sin carátula
    private static final float[] GLOW_DEFAULT = {56, 189, 248};   // resplandor cian de siempre
    private static final int BASE = Color.rgb(7, 9, 12);          // #07090c

    private final View root;
    private final AlbumGlowView glow;
    private final Handler handler;
    private final GradientDrawable linear = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0, 0, 0});
    private final GradientDrawable radial = new GradientDrawable();

    private final float[] theme = THEME_DEFAULT.clone();
    private final float[] glowRgb = GLOW_DEFAULT.clone();
    private float[] fromTheme, toTheme, fromGlow, toGlow;
    private long startMs;
    private int durationMs;
    private int frame;

    private final Runnable stepRunnable = new Runnable() {
        @Override
        public void run() {
            step();
        }
    };

    AmbientTheme(View root, AlbumGlowView glow, Handler handler) {
        this.root = root;
        this.glow = glow;
        this.handler = handler;
        radial.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        radial.setGradientCenter(0.78f, 0.04f);
        Drawable[] layers = {new ColorDrawable(BASE), linear, radial};
        root.setBackground(new LayerDrawable(layers));
        // El radio del degradado depende del ancho de la pantalla
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) render(true);
        });
        render(true);
    }

    /** Nueva canción: color de su carátula (o el de siempre si no tiene), con fundido de durationMs. */
    void applyCover(Bitmap art, int fadeMs) {
        float[] target = THEME_DEFAULT;
        float[] targetGlow = GLOW_DEFAULT;
        float[] rgb = art != null ? coverColor(art) : null;
        if (rgb != null) {
            target = toThemeColor(rgb);
            targetGlow = toGlowColor(rgb);
        }
        fromTheme = theme.clone();
        fromGlow = glowRgb.clone();
        toTheme = target;
        toGlow = targetGlow;
        durationMs = Math.max(200, fadeMs);
        startMs = SystemClock.uptimeMillis();
        frame = 0;
        handler.removeCallbacks(stepRunnable);
        handler.post(stepRunnable);
    }

    private void step() {
        float t = Math.min(1f, (SystemClock.uptimeMillis() - startMs) / (float) durationMs);
        float e = (float) (0.5 - 0.5 * Math.cos(Math.PI * t)); // suave al empezar y al terminar
        for (int i = 0; i < 3; i++) {
            theme[i] = fromTheme[i] + (toTheme[i] - fromTheme[i]) * e;
            glowRgb[i] = fromGlow[i] + (toGlow[i] - fromGlow[i]) * e;
        }
        frame++;
        render(t >= 1f || frame % GLOW_EVERY_FRAMES == 0);
        if (t < 1f) handler.postDelayed(stepRunnable, FRAME_MS);
    }

    private void render(boolean includeGlow) {
        int r = Math.round(theme[0]), g = Math.round(theme[1]), b = Math.round(theme[2]);
        linear.setColors(new int[]{Color.argb(128, r, g, b), Color.argb(46, r, g, b), BASE});
        radial.setColors(new int[]{Color.argb(204, r, g, b), Color.argb(0, r, g, b)});
        int width = root.getWidth() > 0 ? root.getWidth() : 1024;
        radial.setGradientRadius(width * 0.55f);
        root.invalidate();
        if (includeGlow && glow != null) {
            glow.setGlowColor(Color.rgb(Math.round(glowRgb[0]), Math.round(glowRgb[1]), Math.round(glowRgb[2])));
        }
    }

    // ------------------------------------------------------------------

    /** Color representativo: promedio de la carátula en 24x24, con más peso para los colores vivos. */
    private static float[] coverColor(Bitmap art) {
        try {
            Bitmap small = Bitmap.createScaledBitmap(art, 24, 24, true);
            int[] px = new int[24 * 24];
            small.getPixels(px, 0, 24, 0, 0, 24, 24);
            if (small != art) small.recycle();
            double r = 0, g = 0, b = 0, total = 0;
            for (int c : px) {
                int R = Color.red(c), G = Color.green(c), B = Color.blue(c);
                int max = Math.max(R, Math.max(G, B)), min = Math.min(R, Math.min(G, B));
                double sat = max == 0 ? 0 : (max - min) / (double) max;
                double weight = 0.08 + sat * sat * (max / 255.0);
                r += R * weight;
                g += G * weight;
                b += B * weight;
                total += weight;
            }
            if (total <= 0) return null;
            return new float[]{(float) (r / total), (float) (g / total), (float) (b / total)};
        } catch (Exception e) {
            return null;
        }
    }

    /** Oscurece y limita el color para que el texto blanco tenga buen contraste. */
    private static float[] toThemeColor(float[] rgb) {
        float[] hsl = rgbToHsl(rgb[0], rgb[1], rgb[2]);
        float sat = hsl[1] < 0.08f ? hsl[1] : Math.max(0.35f, Math.min(0.75f, hsl[1])); // grises quedan grises
        float light = Math.max(0.22f, Math.min(0.36f, hsl[2]));
        return hslToRgb(hsl[0], sat, light);
    }

    /** Resplandor: el mismo color, más claro y vivo. */
    private static float[] toGlowColor(float[] rgb) {
        float[] hsl = rgbToHsl(rgb[0], rgb[1], rgb[2]);
        if (hsl[1] < 0.08f) return new float[]{200, 210, 225};
        return hslToRgb(hsl[0], Math.max(0.6f, hsl[1]), 0.58f);
    }

    private static float[] rgbToHsl(float r, float g, float b) {
        r /= 255f;
        g /= 255f;
        b /= 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        if (max == min) return new float[]{0, 0, l};
        float d = max - min;
        float s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
        float h;
        if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        return new float[]{h / 6f, s, l};
    }

    private static float[] hslToRgb(float h, float s, float l) {
        if (s == 0) return new float[]{l * 255, l * 255, l * 255};
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        return new float[]{hue(p, q, h + 1f / 3) * 255, hue(p, q, h) * 255, hue(p, q, h - 1f / 3) * 255};
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 1f / 2) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}
