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
 * Indicador de carga animado idéntico al spinner web (arco cyan giratorio sobre pista circular sutil).
 * Diseñado a prueba de fallos para pantallas de autos Android donde ProgressBar framework
 * puede congelarse debido a 'animator_duration_scale=0' o falta de aceleración por hardware.
 */
public class SearchSpinnerView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private static final float STROKE_WIDTH_DP = 2.8f;
    private static final long ROTATION_CYCLE_MS = 800L;

    public SearchSpinnerView(Context context) {
        super(context);
        init();
    }

    public SearchSpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchSpinnerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float strokeWidth = dpToPx(STROKE_WIDTH_DP);

        // Pista circular sutil de fondo (rgba(56, 189, 248, 0.25))
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(Color.parseColor("#4038bdf8"));

        // Arco activo cyan vibrante (--accent-blue / #38bdf8)
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setColor(Color.parseColor("#38bdf8"));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float strokeWidth = dpToPx(STROKE_WIDTH_DP);
        float pad = strokeWidth / 2f + dpToPx(1.5f);
        arcBounds.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || arcBounds.width() <= 0 || arcBounds.height() <= 0) {
            return;
        }

        // 1. Pista circular de fondo
        float cx = arcBounds.centerX();
        float cy = arcBounds.centerY();
        float radius = Math.min(arcBounds.width(), arcBounds.height()) / 2f;
        canvas.drawCircle(cx, cy, radius, trackPaint);

        // 2. Arco activo giratorio (aproximadamente 100 grados de arco, estilo web)
        long now = SystemClock.uptimeMillis();
        float startAngle = (float) ((now % ROTATION_CYCLE_MS) * 360.0 / ROTATION_CYCLE_MS);
        canvas.drawArc(arcBounds, startAngle, 100f, false, arcPaint);

        // 3. Continuar animación con invalidación en tiempo real si está visible
        if (getVisibility() == VISIBLE) {
            postInvalidateDelayed(16); // ~60 FPS suave
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            invalidate();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && getVisibility() == VISIBLE) {
            invalidate();
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
