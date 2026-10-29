package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * A rim and a soft sheen drawn over an app tile, so artwork that runs to the edge in the colour of the
 * background (a black banner on black) still has a visible outline. The rim is bright along the top and fades
 * towards the bottom like light on glass; the sheen is a faint white wash over the top half. Both follow the
 * tile's corners, or its circle (TileStyle).
 */
final class TileBorder extends Drawable {
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final float strokePx;
    private final boolean drawRim;
    private final boolean drawSheen;
    private int shaderHeight = -1;

    private TileBorder(TileStyle style, float density) {
        this.density = density;
        drawRim = style.border != TileStyle.BORDER_NONE;
        drawSheen = style.gloss;
        strokePx = (style.border == TileStyle.BORDER_THICK ? 3f : 1.5f) * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(strokePx);
    }

    /** The drawable for this style, or null when it asks for neither a border nor a sheen. */
    static Drawable create(TileStyle style, float density) {
        if (style.border == TileStyle.BORDER_NONE && !style.gloss) return null;
        return new TileBorder(style, density);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.height() == shaderHeight || bounds.height() <= 0) return;
        shaderHeight = bounds.height();
        rim.setShader(new LinearGradient(0, 0, 0, shaderHeight, 0xCCFFFFFF, 0x33FFFFFF, Shader.TileMode.CLAMP));
        sheen.setShader(new LinearGradient(0, 0, 0, shaderHeight * 0.5f, 0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) return;
        float radius = TileStyle.current.cornerPx(b.width(), density);
        if (drawSheen) {
            canvas.save();
            // A plain rectangular clip keeps this on the GPU; the rounded corners come from the shape drawn under it.
            canvas.clipRect(b.left, b.top, b.right, b.top + b.height() / 2f);
            rect.set(b);
            canvas.drawRoundRect(rect, radius, radius, sheen);
            canvas.restore();
        }
        if (drawRim) {
            float half = strokePx / 2f;
            rect.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
            float r = Math.max(0f, radius - half);
            canvas.drawRoundRect(rect, r, r, rim);
        }
    }

    @Override public void setAlpha(int alpha) { }

    @Override public void setColorFilter(ColorFilter colorFilter) { }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
