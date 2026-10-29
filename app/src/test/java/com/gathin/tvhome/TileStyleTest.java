package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/** The tile look chosen in Launcher settings: the default is the original tile, every other choice stays sane. */
public class TileStyleTest {
    private static final float D = 2f;

    @After public void restoreDefault() {
        TileStyle.current = TileStyle.DEFAULT;
    }

    @Test public void defaultIsTheOriginal16by9TileWith16dpCorners() {
        assertEquals(180f, TileStyle.DEFAULT.designWidthDp(), 0f);
        assertEquals(101f, TileStyle.DEFAULT.designHeightDp(), 0f);
        assertEquals(16f * D, TileStyle.DEFAULT.cornerPx(Math.round(180 * D), D), 0.001f);
        assertEquals(16f * D * 0.5f, TileStyle.DEFAULT.cornerPx(Math.round(90 * D), D), 0.001f);
        assertFalse(TileStyle.DEFAULT.iconOnly);
        assertFalse(TileStyle.DEFAULT.bigIcons());
    }

    @Test public void outOfRangeValuesFallBackToTheDefaults() {
        TileStyle s = new TileStyle(9, -1, false, 7);
        assertEquals(TileStyle.SHAPE_WIDE, s.shape);
        assertEquals(TileStyle.CORNER_MEDIUM, s.corner);
        assertEquals(TileStyle.ICON_NORMAL, s.iconZoom);
    }

    @Test public void squareAndCircleTilesAreSquare() {
        for (int shape : new int[]{TileStyle.SHAPE_SQUARE, TileStyle.SHAPE_CIRCLE}) {
            TileStyle s = new TileStyle(shape, TileStyle.CORNER_MEDIUM, false, TileStyle.ICON_NORMAL);
            assertEquals(s.designWidthDp(), s.designHeightDp(), 0f);
        }
    }

    @Test public void circleRadiusIsHalfTheSide() {
        TileStyle s = new TileStyle(TileStyle.SHAPE_CIRCLE, TileStyle.CORNER_SMALL, false, TileStyle.ICON_NORMAL);
        assertEquals(60f, s.cornerPx(120, D), 0.001f);
    }

    @Test public void cornerChoicesGrowAndStayInsideTheTile() {
        int w = Math.round(150 * D);
        float last = -1f;
        for (int corner = TileStyle.CORNER_SMALL; corner <= TileStyle.CORNER_LARGE; corner++) {
            TileStyle s = new TileStyle(TileStyle.SHAPE_CLASSIC, corner, false, TileStyle.ICON_NORMAL);
            float r = s.cornerPx(w, D);
            assertTrue(r > last);
            assertTrue(r * 2 <= w * s.designHeightDp() / s.designWidthDp());
            last = r;
        }
    }

    @Test public void iconShareGrowsWithTheZoom() {
        float normal = new TileStyle(0, 1, false, TileStyle.ICON_NORMAL).iconShare();
        float large = new TileStyle(0, 1, false, TileStyle.ICON_LARGE).iconShare();
        float fill = new TileStyle(0, 1, false, TileStyle.ICON_FILL).iconShare();
        assertTrue(normal < large && large < fill);
        assertEquals(1f, fill, 0f);
    }

    @Test public void tileSizeFollowsTheChosenShape() {
        TileStyle.current = new TileStyle(TileStyle.SHAPE_SQUARE, TileStyle.CORNER_MEDIUM, false, TileStyle.ICON_NORMAL);
        int w = AppAdapter.tileWidthPx(1696, 6, D);
        assertEquals(w, AppAdapter.tileHeightPx(w));
        assertTrue(w <= Math.round(120 * D));
    }

    @Test public void dockCornerStaysConcentricForEveryShape() {
        for (int shape = TileStyle.SHAPE_WIDE; shape <= TileStyle.SHAPE_CIRCLE; shape++) {
            TileStyle.current = new TileStyle(shape, TileStyle.CORNER_LARGE, false, TileStyle.ICON_NORMAL);
            int tile = AppAdapter.tileWidthPx(1696, 5, D);
            float r = AppAdapter.cornerRadius(tile, D);
            assertTrue(r > 0f);
            assertEquals(r + 20f, DockDecoration.radiusPx(r, 20f), 0.001f);
        }
    }

    @Test public void columnsOfTheDockStayBetweenThreeAndEight() {
        assertEquals(3, Config.MIN_COLUMNS);
        assertEquals(8, Config.MAX_COLUMNS);
        for (int columns = Config.MIN_COLUMNS; columns <= Config.MAX_COLUMNS; columns++) {
            assertTrue(AppAdapter.tileWidthPx(1696, columns, D) >= Math.round(80 * D));
        }
    }

    @Test public void pickedWallpapersAreScaledByPowersOfTwo() {
        assertEquals(1, WallpaperImport.sampleSize(1920, 1080));
        assertEquals(1, WallpaperImport.sampleSize(3000, 2000));
        assertEquals(2, WallpaperImport.sampleSize(4000, 3000));
        assertEquals(4, WallpaperImport.sampleSize(8000, 6000));
    }

    @Test public void pickedFileNamesCarryTheirTime() {
        assertEquals(1730100000000L, WallpaperImport.savedAt("pick_1730100000000.jpg"));
        assertEquals(-1L, WallpaperImport.savedAt("001.jpg"));
        assertEquals(-1L, WallpaperImport.savedAt("pick_abc.jpg"));
        assertEquals(-1L, WallpaperImport.savedAt(null));
    }

    @Test public void defaultHasAThinMirrorBorder() {
        assertEquals(TileStyle.BORDER_THIN, TileStyle.DEFAULT.border);
        assertTrue(TileStyle.DEFAULT.gloss);
    }

    @Test public void withMethodsChangeOnlyTheirOwnChoice() {
        TileStyle s = TileStyle.DEFAULT.withShape(TileStyle.SHAPE_CIRCLE).withBorder(TileStyle.BORDER_THICK).withGloss(false);
        assertEquals(TileStyle.SHAPE_CIRCLE, s.shape);
        assertEquals(TileStyle.BORDER_THICK, s.border);
        assertFalse(s.gloss);
        assertEquals(TileStyle.CORNER_MEDIUM, s.corner);
        assertFalse(s.iconOnly);
        assertEquals(TileStyle.ICON_NORMAL, s.iconZoom);
        assertEquals(TileStyle.BORDER_THIN, new TileStyle(0, 1, false, 0, 9, true).border);
    }

    @Test public void noBorderAndNoShineDrawsNothing() {
        assertEquals(null, TileBorder.create(TileStyle.DEFAULT.withBorder(TileStyle.BORDER_NONE).withGloss(false), D));
    }
}
