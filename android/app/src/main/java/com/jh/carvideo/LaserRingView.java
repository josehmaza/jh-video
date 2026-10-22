package com.jh.carvideo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Borde láser giratorio del cuadro de la carátula, igual que la web (.laser-spinner):
 *
 * - Reproduciendo: un haz cian con cola que da la vuelta al borde cada 2,2 s (sentido horario).
 * - Mezclando (Automix): además, un haz magenta desde el lado opuesto que gira al revés cada 1,5 s.
 *
 * Se dibuja solo en un anillo de 2,5 dp por el borde (como el relleno del contenedor en la web) y
 * aparece/desaparece en 0,4 s. Se anima con postInvalidateDelayed (no con Animator: en radios
 * con animaciones desactivadas los Animator no se mueven) y deja de redibujar cuando no se ve.
 */
public class LaserRingView extends View {

    private static final long CYAN_PERIOD_MS = 2200;
    private static final long MAGENTA_PERIOD_MS = 1500;
    private static final long FADE_MS = 400;
    private static final long FRAME_MS = 33; // ~30 fps: suficiente y liviano en gama baja

    private static final float RING_DP = 2.5f;
    private static final float OUTER_RADIUS_DP = 20f;

    // conic-gradient de la web: transparente 0°-280°, tenue en 320°, color en 350°, blanco en 360°
    private static final float[] STOPS = {0f, 280f / 360f, 320f / 360f, 350f / 360f, 1f};

    private final Paint cyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint magentaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ringPath = new Path();
    private final Matrix matrix = new Matrix();
    private SweepGradient cyanGradient;
    private SweepGradient magentaGradient;

    private boolean playing = false;
    private boolean mixing = false;
    private float cyanAlpha = 0f;
    private float magentaAlpha = 0f;
    private long lastFrameMs = 0;

    public LaserRingView(Context context) {
        super(context);
    }

    public LaserRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPlaying(boolean playing) {
        if (this.playing == playing) return;
        this.playing = playing;
        kick();
    }

    public void setMixing(boolean mixing) {
        if (this.mixing == mixing) return;
        this.mixing = mixing;
        kick();
    }

    private void kick() {
        lastFrameMs = 0;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float ring = RING_DP * density;
        float outerRadius = OUTER_RADIUS_DP * density;

        // Anillo = rectángulo exterior menos el interior (como el relleno de 2,5 px de la web)
        ringPath.reset();
        ringPath.setFillType(Path.FillType.EVEN_ODD);
        ringPath.addRoundRect(new RectF(0, 0, w, h), outerRadius, outerRadius, Path.Direction.CW);
        ringPath.addRoundRect(new RectF(ring, ring, w - ring, h - ring),
                outerRadius - ring, outerRadius - ring, Path.Direction.CW);

        float cx = w / 2f;
        float cy = h / 2f;
        cyanGradient = new SweepGradient(cx, cy,
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(77, 0, 210, 255),
                        Color.rgb(0, 210, 255), Color.WHITE}, STOPS);
        magentaGradient = new SweepGradient(cx, cy,
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(77, 244, 63, 94),
                        Color.rgb(244, 63, 94), Color.WHITE}, STOPS);
        cyanPaint.setShader(cyanGradient);
        magentaPaint.setShader(magentaGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cyanGradient == null) return;

        long now = SystemClock.uptimeMillis();
        float step = lastFrameMs == 0 ? 0f : (now - lastFrameMs) / (float) FADE_MS;
        lastFrameMs = now;
        cyanAlpha = approach(cyanAlpha, playing ? 1f : 0f, step);
        magentaAlpha = approach(magentaAlpha, mixing ? 1f : 0f, step);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        if (cyanAlpha > 0f) {
            // La web empieza el degradado arriba (12 en punto); Android a las 3: se gira -90°
            float angle = 360f * (now % CYAN_PERIOD_MS) / CYAN_PERIOD_MS - 90f;
            matrix.setRotate(angle, cx, cy);
            cyanGradient.setLocalMatrix(matrix);
            cyanPaint.setAlpha(Math.round(255 * cyanAlpha));
            canvas.drawPath(ringPath, cyanPaint);
        }
        if (magentaAlpha > 0f) {
            // Haz magenta: empieza en el lado opuesto (from 180deg) y gira al revés
            float angle = 90f - 360f * (now % MAGENTA_PERIOD_MS) / MAGENTA_PERIOD_MS;
            matrix.setRotate(angle, cx, cy);
            magentaGradient.setLocalMatrix(matrix);
            magentaPaint.setAlpha(Math.round(255 * magentaAlpha));
            canvas.drawPath(ringPath, magentaPaint);
        }

        // Seguir animando mientras algún haz esté visible o apareciendo/desapareciendo
        if (playing || mixing || cyanAlpha > 0f || magentaAlpha > 0f) {
            postInvalidateDelayed(FRAME_MS);
        } else {
            lastFrameMs = 0;
        }
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }
}
