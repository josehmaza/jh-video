package com.jh.carvideo;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Sombra y resplandor detrás del cuadro de la carátula, igual que el box-shadow de la web:
 *
 *   0 12px 32px rgba(0,0,0,0.7)            sombra oscura hacia abajo
 *   0 0 30px 2px  color de la canción 0.55  resplandor cercano
 *   0 0 70px 14px color de la canción 0.30  resplandor amplio
 *
 * La vista es 48dp más grande que el cuadro por cada lado (márgenes negativos en el layout) y
 * el cuadro tapa el centro. El color lo pone MainActivity con el de la carátula (cian sin carátula).
 * Se dibuja en software porque BlurMaskFilter no funciona con aceleración por hardware; solo se
 * redibuja cuando cambia el color o el tamaño.
 */
public class AlbumGlowView extends View {

    private static final float OUTSET_DP = 48f;     // cuánto sobresale la vista del cuadro
    private static final float CORNER_DP = 20f;     // esquinas del cuadro (bg_album_card)

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint widePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private int glowColor = Color.rgb(56, 189, 248);

    public AlbumGlowView(Context context) {
        this(context, null);
    }

    public AlbumGlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        shadowPaint.setColor(Color.argb(179, 0, 0, 0));
        shadowPaint.setMaskFilter(new BlurMaskFilter(26 * density, BlurMaskFilter.Blur.NORMAL));
        nearPaint.setMaskFilter(new BlurMaskFilter(24 * density, BlurMaskFilter.Blur.NORMAL));
        widePaint.setMaskFilter(new BlurMaskFilter(34 * density, BlurMaskFilter.Blur.NORMAL));
        updatePaints();
    }

    /** Color de la canción (ya aclarado para el resplandor). */
    public void setGlowColor(int color) {
        if (glowColor == color) return;
        glowColor = color;
        updatePaints();
        invalidate();
    }

    private void updatePaints() {
        int r = Color.red(glowColor), g = Color.green(glowColor), b = Color.blue(glowColor);
        nearPaint.setColor(Color.argb(140, r, g, b));   // 0.55
        widePaint.setColor(Color.argb(77, r, g, b));    // 0.30
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float outset = OUTSET_DP * density;
        float left = outset, top = outset;
        float right = getWidth() - outset, bottom = getHeight() - outset;
        if (right <= left || bottom <= top) return;
        float corner = CORNER_DP * density;

        // Resplandor amplio (sobresale 10dp antes del difuminado)
        float wide = 10 * density;
        rect.set(left - wide, top - wide, right + wide, bottom + wide);
        canvas.drawRoundRect(rect, corner + wide, corner + wide, widePaint);

        // Sombra oscura hacia abajo
        float drop = 12 * density;
        rect.set(left, top + drop, right, bottom + drop);
        canvas.drawRoundRect(rect, corner, corner, shadowPaint);

        // Resplandor cercano
        float near = 2 * density;
        rect.set(left - near, top - near, right + near, bottom + near);
        canvas.drawRoundRect(rect, corner + near, corner + near, nearPaint);
    }
}
