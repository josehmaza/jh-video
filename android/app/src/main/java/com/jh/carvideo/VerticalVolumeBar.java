package com.jh.carvideo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Control vertical de volumen ergonómico para auto.
 * Evita solapamiento táctil con los botones + y -, permitiendo deslizamiento
 * suave y visualmente idéntico a la barra web (.vol-meter-track).
 */
public class VerticalVolumeBar extends View {

    public interface OnVolumeChangeListener {
        void onVolumeChanged(int percent, boolean fromUser);
    }

    private int progress = 75; // 0..100
    private OnVolumeChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF fillRect = new RectF();

    public VerticalVolumeBar(Context context) {
        super(context);
        init();
    }

    public VerticalVolumeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VerticalVolumeBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint.setColor(Color.parseColor("#11141a"));
        trackPaint.setStyle(Paint.Style.FILL);

        trackBorderPaint.setColor(Color.parseColor("#283244"));
        trackBorderPaint.setStyle(Paint.Style.STROKE);
        trackBorderPaint.setStrokeWidth(dpToPx(1.2f));

        fillPaint.setStyle(Paint.Style.FILL);

        thumbPaint.setColor(Color.parseColor("#f1f5f9"));
        thumbPaint.setStyle(Paint.Style.FILL);

        thumbBorderPaint.setColor(Color.parseColor("#0d1117"));
        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setStrokeWidth(dpToPx(2.5f));

        thumbGlowPaint.setColor(Color.parseColor("#5538bdf8"));
        thumbGlowPaint.setStyle(Paint.Style.FILL);
    }

    public void setProgress(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        if (this.progress != clamped) {
            this.progress = clamped;
            invalidate();
        }
    }

    public int getProgress() {
        return progress;
    }

    public void setOnVolumeChangeListener(OnVolumeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float trackW = dpToPx(12f);
        float thumbRadius = dpToPx(10f);
        float padY = thumbRadius + dpToPx(4f);
        float usableH = h - (padY * 2f);

        float trackLeft = (w - trackW) / 2f;
        float trackRight = trackLeft + trackW;
        float trackTop = padY;
        float trackBottom = h - padY;

        // Pista de fondo
        trackRect.set(trackLeft, trackTop, trackRight, trackBottom);
        canvas.drawRoundRect(trackRect, trackW / 2f, trackW / 2f, trackPaint);
        canvas.drawRoundRect(trackRect, trackW / 2f, trackW / 2f, trackBorderPaint);

        // Nivel de llenado (de abajo hacia arriba)
        float fillTop = trackBottom - (usableH * (progress / 100f));
        if (progress > 0) {
            fillRect.set(trackLeft, fillTop, trackRight, trackBottom);
            LinearGradient gradient = new LinearGradient(
                    trackLeft, fillTop, trackLeft, trackBottom,
                    Color.parseColor("#60a5fa"), Color.parseColor("#38bdf8"),
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(gradient);
            canvas.drawRoundRect(fillRect, trackW / 2f, trackW / 2f, fillPaint);
        }

        // Pomo / Thumb indicador
        float thumbY = fillTop;
        float thumbX = w / 2f;

        // Resplandor cian sutil
        canvas.drawCircle(thumbX, thumbY, thumbRadius + dpToPx(2f), thumbGlowPaint);
        // Centro de titanio
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint);
        // Borde oscuro del pomo
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                updateTouchProgress(event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateTouchProgress(event.getY());
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateTouchProgress(float touchY) {
        int h = getHeight();
        float thumbRadius = dpToPx(10f);
        float padY = thumbRadius + dpToPx(4f);
        float usableH = h - (padY * 2f);

        float fraction = 1.0f - ((touchY - padY) / usableH);
        fraction = Math.max(0f, Math.min(1f, fraction));
        int newPercent = Math.round(fraction * 100);

        if (newPercent != progress) {
            progress = newPercent;
            invalidate();
            if (listener != null) {
                listener.onVolumeChanged(progress, true);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
