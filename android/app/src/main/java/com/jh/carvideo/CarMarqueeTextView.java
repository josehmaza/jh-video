package com.jh.carvideo;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Texto de una línea que, si no entra, se desplaza igual que la marquesina de la web (carMarquee):
 *
 *   0%-20%   quieto mostrando el INICIO del texto
 *   20%-65%  se desliza suave hacia la izquierda hasta mostrar el FINAL
 *   65%-82%  quieto mostrando el final
 *   82%-100% vuelve suave al inicio
 *
 * La duración del ciclo depende de cuánto sobra el texto, entre 6 y 16 s (como la web).
 * Si el texto entra, se dibuja normal (respeta gravity). Animado con postInvalidateDelayed,
 * no con Animator, para radios con animaciones desactivadas.
 */
public class CarMarqueeTextView extends TextView {

    private static final long FRAME_MS = 33;
    private static final float START_HOLD = 0.20f;
    private static final float SCROLL_END = 0.65f;
    private static final float END_HOLD = 0.82f;

    private long cycleStartMs = SystemClock.uptimeMillis();
    private boolean singleLineMode = true;
    private boolean carMarqueeEnabled = true;
    private String lastText = "";

    public CarMarqueeTextView(Context context) {
        super(context);
        init();
    }

    public CarMarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSingleLine(true);
        setEllipsize(null);              // sin "…": el texto completo se ve desplazándose
        setHorizontalFadingEdgeEnabled(false);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        // Solo si el texto cambió de verdad: las listas vuelven a poner el mismo texto al refrescarse
        String now = text != null ? text.toString() : "";
        if (!now.equals(lastText)) {
            lastText = now;
            cycleStartMs = SystemClock.uptimeMillis(); // cada canción nueva empieza mostrando el inicio
        }
        invalidate();
    }

    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        singleLineMode = singleLine;
    }

    /**
     * Activa o desactiva el desplazamiento. Desactivado, un texto largo se corta con "…"
     * (en la lista, solo la canción que suena se desplaza, como en la web).
     */
    public void setCarMarqueeEnabled(boolean enabled) {
        if (carMarqueeEnabled == enabled) return;
        carMarqueeEnabled = enabled;
        setEllipsize(enabled ? null : TextUtils.TruncateAt.END);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!carMarqueeEnabled || !singleLineMode) {
            super.onDraw(canvas); // sin desplazamiento, o en varias líneas (modo expandido de la lista)
            return;
        }
        CharSequence text = getText();
        TextPaint paint = getPaint();
        float available = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        float textWidth = TextUtils.isEmpty(text) ? 0 : paint.measureText(text, 0, text.length());
        float density = getResources().getDisplayMetrics().density;
        float overflow = textWidth - available;

        if (overflow <= 4 * density || available <= 0) {
            super.onDraw(canvas); // entra completo: dibujo normal
            return;
        }

        // Duración como la web: (sobrante / 25) + 4 s, entre 6 y 16 s
        float overflowDp = overflow / density;
        long durationMs = Math.round(Math.max(6f, Math.min(16f, overflowDp / 25f + 4f)) * 1000);
        float distance = overflow + 10 * density;

        float phase = ((SystemClock.uptimeMillis() - cycleStartMs) % durationMs) / (float) durationMs;
        float offset;
        if (phase <= START_HOLD) {
            offset = 0f;
        } else if (phase <= SCROLL_END) {
            offset = distance * ease((phase - START_HOLD) / (SCROLL_END - START_HOLD));
        } else if (phase <= END_HOLD) {
            offset = distance;
        } else {
            offset = distance * (1f - ease((phase - END_HOLD) / (1f - END_HOLD)));
        }

        paint.setColor(getCurrentTextColor());
        paint.drawableState = getDrawableState();
        float left = getCompoundPaddingLeft();
        canvas.save();
        // Si el texto no entra, TextView desplaza el lienzo por su cuenta (scrollX) para centrarlo
        // o alinearlo al final según gravity; se anula para dibujar desde el borde visible.
        canvas.translate(getScrollX(), getScrollY());
        canvas.clipRect(left, 0, getWidth() - getCompoundPaddingRight(), getHeight());
        canvas.drawText(text, 0, text.length(), left - offset, getBaseline(), paint);
        canvas.restore();

        postInvalidateDelayed(FRAME_MS);
    }

    /** Suavizado ida y vuelta, equivalente a cubic-bezier(0.42, 0, 0.58, 1) de la web. */
    private static float ease(float x) {
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * Math.max(0f, Math.min(1f, x))));
    }
}
