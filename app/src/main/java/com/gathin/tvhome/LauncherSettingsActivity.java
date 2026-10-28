package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[Config.MAX_COLUMNS - Config.MIN_COLUMNS + 1];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private final TextView[] shapeChoices = new TextView[4];
    private final TextView[] cornerChoices = new TextView[3];
    private final TextView[] contentChoices = new TextView[2];
    private final TextView[] zoomChoices = new TextView[3];
    private TileStyle style;
    private static final int REQUEST_PICK_WALLPAPER = 4107;
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns3, R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7, R.id.columns8};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + Config.MIN_COLUMNS;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        style = TileStyle.load(this);
        int[] shapeIds = {R.id.shape0, R.id.shape1, R.id.shape2, R.id.shape3};
        for (int i = 0; i < shapeIds.length; i++) {
            final int value = i;
            shapeChoices[i] = findViewById(shapeIds[i]);
            shapeChoices[i].setOnClickListener(v -> {
                if (style.shape != value) saveStyle(new TileStyle(value, style.corner, style.iconOnly, style.iconZoom));
            });
        }
        int[] cornerIds = {R.id.corner0, R.id.corner1, R.id.corner2};
        for (int i = 0; i < cornerIds.length; i++) {
            final int value = i;
            cornerChoices[i] = findViewById(cornerIds[i]);
            cornerChoices[i].setOnClickListener(v -> {
                if (style.corner != value) saveStyle(new TileStyle(style.shape, value, style.iconOnly, style.iconZoom));
            });
        }
        int[] contentIds = {R.id.content0, R.id.content1};
        for (int i = 0; i < contentIds.length; i++) {
            final boolean iconOnly = i == 1;
            contentChoices[i] = findViewById(contentIds[i]);
            contentChoices[i].setOnClickListener(v -> {
                if (style.iconOnly != iconOnly) saveStyle(new TileStyle(style.shape, style.corner, iconOnly, style.iconZoom));
            });
        }
        int[] zoomIds = {R.id.zoom0, R.id.zoom1, R.id.zoom2};
        for (int i = 0; i < zoomIds.length; i++) {
            final int value = i;
            zoomChoices[i] = findViewById(zoomIds[i]);
            zoomChoices[i].setOnClickListener(v -> {
                if (style.iconZoom != value) saveStyle(new TileStyle(style.shape, style.corner, style.iconOnly, value));
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY)
                    .remove(TileStyle.KEY_SHAPE).remove(TileStyle.KEY_CORNER).remove(TileStyle.KEY_ICON_ONLY)
                    .remove(TileStyle.KEY_ICON_ZOOM).commit();
            style = TileStyle.load(this);
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - Config.MIN_COLUMNS, columnChoices.length - 1))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        // First row: a picture from this device's storage. Then the pictures already here, then None.
        String[] labels = new String[images.size() + 2];
        labels[0] = getString(R.string.wallpaper_pick_device);
        for (int i = 0; i < images.size(); i++) labels[i + 1] = wallpaperLabel(images.get(i).getName());
        labels[images.size() + 1] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int found = indexOf(images, current);
        int checked = "none".equals(current) ? images.size() + 1 : found >= 0 ? found + 1 : -1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        pickWallpaperFromDevice();
                        return;
                    }
                    if (which == images.size() + 1) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which - 1).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The system file picker for one picture; the TV may have none (Google TV ships without), then say where to copy files. */
    private void pickWallpaperFromDevice() {
        String[] actions = {Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT};
        for (String action : actions) {
            try {
                startActivityForResult(new Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
                        REQUEST_PICK_WALLPAPER);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // try the next way of asking
            }
        }
        File dir = Config.dir(this);
        Toast.makeText(this, getString(R.string.wallpaper_pick_missing,
                dir == null ? "Android/data/" + getPackageName() + "/files/wallpapers"
                        : new File(dir, "wallpapers").getAbsolutePath()), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final android.content.Context app = getApplicationContext();
        // Decoding and saving a photo takes a moment; keep it off the UI thread.
        new Thread(() -> {
            String name = WallpaperImport.save(app, uri);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (name == null) {
                    Toast.makeText(this, R.string.wallpaper_pick_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, name).apply();
                changed();
                Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                refresh();
            });
        }, "wallpaper-import").start();
    }

    /** A picture's name for the list; one picked from the device shows when it was added instead of pick_<time>.jpg. */
    private static String wallpaperLabel(String name) {
        long when = WallpaperImport.savedAt(name);
        return when < 0 ? name
                : "\u2605 " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(when));
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    /** Saves the tile look (shape, corners, content, icon size) and rebuilds Home with it. */
    private void saveStyle(TileStyle next) {
        style = next;
        choices.edit().putInt(TileStyle.KEY_SHAPE, next.shape).putInt(TileStyle.KEY_CORNER, next.corner)
                .putBoolean(TileStyle.KEY_ICON_ONLY, next.iconOnly).putInt(TileStyle.KEY_ICON_ZOOM, next.iconZoom).apply();
        changed();
        refresh();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + Config.MIN_COLUMNS);
        }
        for (int i = 0; i < shapeChoices.length; i++) shapeChoices[i].setSelected(style.shape == i);
        for (int i = 0; i < cornerChoices.length; i++) {
            cornerChoices[i].setSelected(style.corner == i);
            cornerChoices[i].setAlpha(style.circle() ? 0.5f : 1f);   // a circle has no corners to round
        }
        contentChoices[0].setSelected(!style.iconOnly);
        contentChoices[1].setSelected(style.iconOnly);
        for (int i = 0; i < zoomChoices.length; i++) zoomChoices[i].setSelected(style.iconZoom == i);
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperLabel(wallpaperCurrent)
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains(TileStyle.KEY_SHAPE)
                || choices.contains(TileStyle.KEY_CORNER) || choices.contains(TileStyle.KEY_ICON_ONLY)
                || choices.contains(TileStyle.KEY_ICON_ZOOM);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
