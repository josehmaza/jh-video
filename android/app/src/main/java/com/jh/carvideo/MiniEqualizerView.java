package com.jh.carvideo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Tres barritas blancas que reemplazan al ▶ en el botón de la carpeta que suena: se mueven
 * mientras suena y quedan quietas en pausa. Se redibuja a unos 30 cuadros por segundo y solo
 * mientras está a la vista y animando (el radio es de gama baja).
 */
public class MiniEqualizerView extends View {

    private static final float[] PAUSED_LEVELS = {0.45f, 0.8f, 0.55f};
    private static final long CYCLE_MS = 900;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private boolean animating;
    private long startMs;

    public MiniEqualizerView(Context context) {
        this(context, null);
    }

    public MiniEqualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
    }

    public void setAnimating(boolean on) {
        if (animating == on) return;
        animating = on;
        startMs = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float gap = w * 0.16f;
        float barW = (w - gap * 2) / 3f;
        double t = (SystemClock.uptimeMillis() - startMs) / (double) CYCLE_MS * 2 * Math.PI;
        for (int i = 0; i < 3; i++) {
            float level = animating
                    ? 0.25f + 0.75f * (0.5f + 0.5f * (float) Math.sin(t - i * 2.1))
                    : PAUSED_LEVELS[i];
            float left = i * (barW + gap);
            bar.set(left, h - h * level, left + barW, h);
            canvas.drawRoundRect(bar, barW / 2f, barW / 2f, paint);
        }
        if (animating && isShown()) {
            postInvalidateDelayed(33);
        }
    }
}
