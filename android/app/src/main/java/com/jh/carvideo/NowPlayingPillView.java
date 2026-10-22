package com.jh.carvideo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Cápsula de estado al pie de la carátula, igual que la web (.now-playing-pill):
 * texto PAUSED / PLAYING / MIXING ⇄ y 5 barras de ecualizador.
 *
 * - En pausa: barras bajas y grises, texto apagado, borde tenue.
 * - Reproduciendo: barras cian que suben y bajan, texto y borde cian con brillo.
 * - Mezclando (Automix): todo pasa a magenta.
 *
 * Se anima con postInvalidateDelayed (no con Animator) y solo mientras suena.
 */
public class NowPlayingPillView extends View {

    private static final long FRAME_MS = 33;
    private static final int CYAN = Color.rgb(0, 210, 255);
    private static final int CYAN_LIGHT = Color.rgb(103, 232, 249);   // #67e8f9 (menos lavado, más nítido)
    private static final int MAGENTA = Color.rgb(244, 63, 94);        // #f43f5e
    private static final int MAGENTA_LIGHT = Color.rgb(251, 113, 133); // #fb7185 (menos lavado, más nítido)
    private static final int MUTED = Color.rgb(100, 116, 139);        // text-muted

    // Duración y retraso de cada barra (pillWave de la web, ida y vuelta)
    private static final float[] BAR_PERIOD_S = {0.7f, 0.5f, 0.8f, 0.6f, 0.75f};
    private static final float[] BAR_DELAY_S = {0f, 0.15f, 0.3f, 0.2f, 0.05f};

    // Tamaño pensado para leerse desde el asiento en una pantalla de baja densidad
    private static final float PAD_X = 18f;     // dp
    private static final float BAR_W = 5.5f;    // dp
    private static final float BAR_GAP = 4f;    // dp
    private static final float BAR_MAX_H = 22f; // dp

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private boolean playing = false;
    private boolean mixing = false;
    private final long startMs = SystemClock.uptimeMillis();

    public NowPlayingPillView(Context context) {
        this(context, null);
    }

    public NowPlayingPillView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        bgPaint.setColor(Color.argb(224, 10, 14, 22));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * density);
        glowPaint.setStyle(Paint.Style.STROKE);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(15 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setLetterSpacing(0.05f);
        // El brillo (box-shadow / text-shadow de la web) usa sombras de software
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setPlaying(boolean playing) {
        if (this.playing == playing) return;
        this.playing = playing;
        invalidate();
    }

    public void setMixing(boolean mixing) {
        if (this.mixing == mixing) return;
        this.mixing = mixing;
        invalidate();
    }

    private String label() {
        if (mixing) return "MIXING ⇄";
        return playing ? "PLAYING" : "PAUSED";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Ancho según el texto más largo, para que la cápsula no cambie de tamaño al cambiar de estado
        float textW = textPaint.measureText("MIXING ⇄");
        int w = Math.round(PAD_X * density + textW + 12 * density + barsWidth() + PAD_X * density);
        int h = Math.round(38 * density);
        setMeasuredDimension(w, h);
    }

    private float barsWidth() {
        return 5 * BAR_W * density + 4 * BAR_GAP * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean active = playing || mixing;
        int accent = mixing ? MAGENTA : CYAN;
        int accentLight = mixing ? MAGENTA_LIGHT : CYAN_LIGHT;

        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        // Fondo cápsula
        rect.set(0.5f * density, 0.5f * density, w - 0.5f * density, h - 0.5f * density);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // Borde: tenue en pausa, cian (0.45) al reproducir, magenta (0.65) al mezclar, con brillo
        if (active) {
            glowPaint.setStrokeWidth(2 * density);
            glowPaint.setColor(withAlpha(accent, mixing ? 0.45f : 0.3f));
            glowPaint.setShadowLayer(7 * density, 0, 0, withAlpha(accent, mixing ? 0.45f : 0.3f));
            canvas.drawRoundRect(rect, radius, radius, glowPaint);
            borderPaint.setColor(withAlpha(accent, mixing ? 0.65f : 0.45f));
        } else {
            borderPaint.setColor(Color.argb(31, 255, 255, 255));
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint);

        // Texto de estado
        String text = label();
        float textX = PAD_X * density;
        float textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        if (active) {
            textPaint.setColor(accent);
            textPaint.setShadowLayer(3 * density, 0, 0, withAlpha(accent, mixing ? 0.6f : 0.4f)); // brillo corto: no difumina las letras
        } else {
            textPaint.setColor(MUTED);
            textPaint.clearShadowLayer();
        }
        canvas.drawText(text, textX, textY, textPaint);

        // Barras de ecualizador (alineadas abajo, 4-22 dp). Sin sombra y con bordes en píxeles
        // enteros: en la pantalla de baja densidad el brillo y los bordes a medio píxel las
        // hacían ver borrosas (sobre todo en magenta).
        int barW = Math.round(BAR_W * density);
        int gap = Math.round(BAR_GAP * density);
        float maxH = BAR_MAX_H * density;
        float minH = 4 * density;
        int barsX = Math.round(w - PAD_X * density - barsWidth());
        int barsBottom = Math.round(h / 2f + maxH / 2f);
        float tSec = (SystemClock.uptimeMillis() - startMs) / 1000f;
        barPaint.clearShadowLayer();
        if (active) {
            barPaint.setShader(new LinearGradient(0, barsBottom, 0, barsBottom - maxH, accent, accentLight, Shader.TileMode.CLAMP));
        } else {
            barPaint.setShader(null);
            barPaint.setColor(Color.argb(64, 255, 255, 255));
        }
        float corner = 1.5f * density;
        for (int i = 0; i < 5; i++) {
            float barH = minH;
            if (active) {
                // Ida y vuelta (alternate) con suavizado (ease-in-out)
                float t = (tSec - BAR_DELAY_S[i]) / BAR_PERIOD_S[i];
                if (t < 0) t = 0;
                float cycle = t % 2f;
                float linear = cycle <= 1f ? cycle : 2f - cycle;
                float eased = (float) (0.5 - 0.5 * Math.cos(Math.PI * linear));
                barH = minH + (maxH - minH) * eased;
            }
            int left = barsX + i * (barW + gap);
            int top = barsBottom - Math.round(barH);
            rect.set(left, top, left + barW, barsBottom);
            canvas.drawRoundRect(rect, corner, corner, barPaint);
        }

        if (active) postInvalidateDelayed(FRAME_MS);
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }
}
