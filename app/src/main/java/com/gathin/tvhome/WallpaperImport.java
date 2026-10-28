package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Brings a picture chosen from the TV's own storage (the system file picker) into the launcher's wallpapers
 * folder, so the existing wallpaper code shows it like any theme picture and it keeps working after the picker's
 * temporary permission is gone. The copy is scaled down to 1920 px and saved as a JPEG: a 12 MP photo would
 * otherwise cost the TV 48 MB of memory to show.
 */
final class WallpaperImport {
    /** Files made here are called pick_<time>.jpg, which is how Launcher settings recognises them. */
    static final String PREFIX = "pick_";
    private static final int MAX_SIDE = 1920;
    /** How many picked pictures are kept; the oldest go first. */
    private static final int KEEP = 12;

    private WallpaperImport() { }

    /** The saved file's name inside the wallpapers folder, or null when the picture could not be read or saved. */
    static String save(Context ctx, Uri uri) {
        File root = Config.dir(ctx);
        if (root == null) return null;
        File dir = new File(root, "wallpapers");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        ContentResolver resolver = ctx.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
        Bitmap bitmap;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return null;
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return null;
        }
        if (bitmap == null) return null;

        Bitmap finished = fit(bitmap, orientation(resolver, uri));
        File out = new File(dir, PREFIX + System.currentTimeMillis() + ".jpg");
        boolean ok;
        try (FileOutputStream stream = new FileOutputStream(out)) {
            ok = finished.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        } catch (IOException e) {
            ok = false;
        }
        if (finished != bitmap) finished.recycle();
        bitmap.recycle();
        if (!ok) {
            out.delete();
            return null;
        }
        prune(dir);
        return out.getName();
    }

    /** The largest power of two that still leaves the longer side at {@link #MAX_SIDE} or more. */
    static int sampleSize(int width, int height) {
        int longest = Math.max(width, height);
        int sample = 1;
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2;
        return sample;
    }

    /** The decoded picture turned upright and scaled so its longer side is at most {@link #MAX_SIDE}. */
    private static Bitmap fit(Bitmap source, int degrees) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        float scale = longest > MAX_SIDE ? MAX_SIDE / (float) longest : 1f;
        if (degrees == 0 && scale == 1f) return source;
        Matrix matrix = new Matrix();
        if (scale != 1f) matrix.postScale(scale, scale);
        if (degrees != 0) matrix.postRotate(degrees);
        try {
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            return source;
        }
    }

    /** A photo's EXIF rotation in degrees (phones store portrait shots sideways); 0 for anything unreadable. */
    private static int orientation(ContentResolver resolver, Uri uri) {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return 0;
            int exif = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (exif) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    /** Keeps the newest {@link #KEEP} picked pictures. */
    private static void prune(File dir) {
        File[] picked = dir.listFiles((d, name) -> name.startsWith(PREFIX) && name.endsWith(".jpg"));
        if (picked == null || picked.length <= KEEP) return;
        Arrays.sort(picked, (a, b) -> a.getName().compareTo(b.getName()));
        for (int i = 0; i < picked.length - KEEP; i++) picked[i].delete();
    }

    /** The time a picked picture was saved, from its file name; -1 for any other name. */
    static long savedAt(String name) {
        if (name == null || !name.startsWith(PREFIX) || !name.endsWith(".jpg")) return -1L;
        try {
            return Long.parseLong(name.substring(PREFIX.length(), name.length() - 4));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
