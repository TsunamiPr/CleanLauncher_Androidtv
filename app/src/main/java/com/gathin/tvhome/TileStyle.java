package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * How the app tiles look, chosen in Launcher settings: the shape (aspect ratio and corners), whether every tile
 * shows the app's icon instead of its banner, and how far the icon is enlarged to fill the shape.
 * The default is exactly the original look (16:9 banner tile, 16dp corners, banner first).
 * Stored in the same preferences file as the other on-TV choices (Config.SETTINGS_NAME).
 */
final class TileStyle {
    static final int SHAPE_WIDE = 0, SHAPE_CLASSIC = 1, SHAPE_SQUARE = 2, SHAPE_CIRCLE = 3;
    static final int CORNER_SMALL = 0, CORNER_MEDIUM = 1, CORNER_LARGE = 2;
    static final int ICON_NORMAL = 0, ICON_LARGE = 1, ICON_FILL = 2;

    static final String KEY_SHAPE = "tile_shape";
    static final String KEY_CORNER = "tile_corner";
    static final String KEY_ICON_ONLY = "tile_icon_only";
    static final String KEY_ICON_ZOOM = "tile_icon_zoom";

    /** The original look. */
    static final TileStyle DEFAULT = new TileStyle(SHAPE_WIDE, CORNER_MEDIUM, false, ICON_NORMAL);

    /** The style Home was built with; set once in MainActivity.onCreate, read by AppAdapter and the dock. */
    static TileStyle current = DEFAULT;

    final int shape;
    final int corner;
    /** Every app tile shows its icon on a tinted card, never its banner. */
    final boolean iconOnly;
    /** ICON_NORMAL, ICON_LARGE or ICON_FILL (the icon grows until it fills the tile shape). */
    final int iconZoom;

    TileStyle(int shape, int corner, boolean iconOnly, int iconZoom) {
        this.shape = clamp(shape, SHAPE_WIDE, SHAPE_CIRCLE, SHAPE_WIDE);
        this.corner = clamp(corner, CORNER_SMALL, CORNER_LARGE, CORNER_MEDIUM);
        this.iconOnly = iconOnly;
        this.iconZoom = clamp(iconZoom, ICON_NORMAL, ICON_FILL, ICON_NORMAL);
    }

    private static int clamp(int v, int lo, int hi, int fallback) {
        return v < lo || v > hi ? fallback : v;
    }

    static TileStyle load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        return new TileStyle(p.getInt(KEY_SHAPE, SHAPE_WIDE), p.getInt(KEY_CORNER, CORNER_MEDIUM),
                p.getBoolean(KEY_ICON_ONLY, false), p.getInt(KEY_ICON_ZOOM, ICON_NORMAL));
    }

    /** Design width of a tile in dp; tiles shrink from here when the columns do not fit. */
    float designWidthDp() {
        switch (shape) {
            case SHAPE_CLASSIC: return 150f;
            case SHAPE_SQUARE:
            case SHAPE_CIRCLE: return 120f;
            default: return AppAdapter.DESIGN_W_DP;
        }
    }

    /** Design height of a tile in dp. */
    float designHeightDp() {
        switch (shape) {
            case SHAPE_CLASSIC: return 112f;   // 4:3
            case SHAPE_SQUARE:
            case SHAPE_CIRCLE: return 120f;    // 1:1
            default: return AppAdapter.DESIGN_H_DP;
        }
    }

    boolean circle() {
        return shape == SHAPE_CIRCLE;
    }

    /** Corner radius in dp at the design width (ignored for a circle). */
    float cornerDp() {
        switch (corner) {
            case CORNER_SMALL: return 6f;
            case CORNER_LARGE: return 30f;
            default: return 16f;
        }
    }

    /**
     * Corner radius in px of a tile this wide: the corner scaled down with a tile that shrank below its
     * design width, or half the short side for a circle.
     */
    float cornerPx(int tileWidthPx, float density) {
        int h = Math.round(tileWidthPx * designHeightDp() / designWidthDp());
        if (circle()) return Math.min(tileWidthPx, h) / 2f;
        return cornerDp() * density * Math.min(1f, tileWidthPx / (designWidthDp() * density));
    }

    /** Share of the shorter tile side an icon takes: the normal 62% mark, a large one, or the whole shape. */
    float iconShare() {
        switch (iconZoom) {
            case ICON_FILL: return 1f;
            case ICON_LARGE: return 0.82f;
            default: return 0.62f;
        }
    }

    /** True when icons are drawn big enough that the app name inside the tile would only get in the way. */
    boolean bigIcons() {
        return iconZoom != ICON_NORMAL;
    }
}
