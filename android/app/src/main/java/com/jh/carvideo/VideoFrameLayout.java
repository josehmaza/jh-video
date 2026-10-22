package com.jh.carvideo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Cuadro del video en el reproductor: toma todo el ancho que le dan y su alto es 16:9 de ese ancho
 * (como aspect-ratio: 16 / 9 de la web).
 */
public class VideoFrameLayout extends FrameLayout {

    public VideoFrameLayout(Context context) {
        super(context);
    }

    public VideoFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * 9f / 16f);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
