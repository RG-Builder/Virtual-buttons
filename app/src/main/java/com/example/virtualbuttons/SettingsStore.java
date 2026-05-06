package com.example.virtualbuttons;

import android.content.Context;
import android.content.SharedPreferences;

final class SettingsStore {
    enum StreamMode { MEDIA, SYSTEM, ACTIVE }
    enum GestureMode { SWIPE, DOUBLE_TAP, BOTH }

    private static final String PREFS = "virtual_button_settings";
    private final SharedPreferences prefs;

    SettingsStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean overlayEnabled() { return prefs.getBoolean("overlay_enabled", false); }
    void setOverlayEnabled(boolean enabled) { prefs.edit().putBoolean("overlay_enabled", enabled).apply(); }
    int buttonX() { return prefs.getInt("button_x", 24); }
    int buttonY() { return prefs.getInt("button_y", 360); }
    void setButtonPosition(int x, int y) { prefs.edit().putInt("button_x", x).putInt("button_y", y).apply(); }
    int buttonSizeDp() { return prefs.getInt("button_size", 64); }
    int buttonOpacity() { return prefs.getInt("button_opacity", 86); }
    int volumeStep() { return prefs.getInt("volume_step", 1); }
    int gestureSensitivity() { return prefs.getInt("gesture_sensitivity", 36); }
    GestureMode gestureMode() { return GestureMode.valueOf(prefs.getString("gesture_mode", GestureMode.BOTH.name())); }
    StreamMode streamMode() { return StreamMode.valueOf(prefs.getString("stream_mode", StreamMode.ACTIVE.name())); }
    boolean edgeGestures() { return prefs.getBoolean("edge_gestures", true); }
    boolean shakeToMute() { return prefs.getBoolean("shake_to_mute", false); }
    boolean haptics() { return prefs.getBoolean("haptics", true); }
    boolean visualIndicator() { return prefs.getBoolean("visual_indicator", true); }
    boolean autoNightProfile() { return prefs.getBoolean("auto_night_profile", false); }
    int nightVolumePercent() { return prefs.getInt("night_volume", 25); }
    int nightStartHour() { return prefs.getInt("night_start", 22); }
    int nightEndHour() { return prefs.getInt("night_end", 7); }
    boolean startOnBoot() { return prefs.getBoolean("start_on_boot", true); }

    void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
    void putBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }
    void putString(String key, String value) { prefs.edit().putString(key, value).apply(); }
}
