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
 * Visualizador animado de barras de audio estilo ecualizador en vivo (cyan).
 * Cuando reproduce, las 5 barras pulsan rítmicamente como en la versión web.
 * Cuando está en pausa, se asientan suavemente a altura mínima.
 */
public class SoundWaveBarsView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private boolean isPlaying = false;
    private final float[] heights = new float[]{0.15f, 0.15f, 0.15f, 0.15f, 0.15f};

    public SoundWaveBarsView(Context context) {
        super(context);
        init();
    }

    public SoundWaveBarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SoundWaveBarsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setColor(Color.parseColor("#38bdf8")); // Cyan idéntico a web
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setPlaying(boolean playing) {
        if (this.isPlaying != playing) {
            this.isPlaying = playing;
            invalidate();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int count = 5;
        float barWidth = dpToPx(3.8f);
        float barGap = dpToPx(4.5f);
        float totalWidth = (count * barWidth) + ((count - 1) * barGap);
        float startX = (w - totalWidth) / 2f;
        float minHeight = dpToPx(3.5f);
        float maxHeight = h - dpToPx(1.5f);

        long now = SystemClock.uptimeMillis();

        if (isPlaying) {
            // Animación suave y desfasada de 5 frecuencias
            heights[0] = (float) (0.15 + 0.70 * Math.abs(Math.sin(now / 190.0)));
            heights[1] = (float) (0.15 + 0.85 * Math.abs(Math.sin((now / 140.0) + 1.2)));
            heights[2] = (float) (0.15 + 0.80 * Math.abs(Math.sin((now / 220.0) + 2.3)));
            heights[3] = (float) (0.15 + 0.75 * Math.abs(Math.sin((now / 170.0) + 0.7)));
            heights[4] = (float) (0.15 + 0.65 * Math.abs(Math.sin((now / 200.0) + 1.8)));
        } else {
            // En pausa: retorno suave a reposo
            for (int i = 0; i < count; i++) {
                heights[i] = Math.max(0.12f, heights[i] * 0.85f);
            }
        }

        for (int i = 0; i < count; i++) {
            float barH = minHeight + heights[i] * (maxHeight - minHeight);
            float left = startX + i * (barWidth + barGap);
            float top = h - barH;
            float right = left + barWidth;
            float bottom = h;

            rect.set(left, top, right, bottom);
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint);
        }

        if (isPlaying) {
            postInvalidateOnAnimation();
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
