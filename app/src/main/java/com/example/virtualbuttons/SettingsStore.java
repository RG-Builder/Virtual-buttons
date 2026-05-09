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
    int gestureSensitivity() { return prefs.getInt("gesture_sensitivity", 24); }
    GestureMode gestureMode() {
        try { return GestureMode.valueOf(prefs.getString("gesture_mode", GestureMode.BOTH.name())); }
        catch (Exception e) { return GestureMode.BOTH; }
    }
    StreamMode streamMode() {
        try { return StreamMode.valueOf(prefs.getString("stream_mode", StreamMode.ACTIVE.name())); }
        catch (Exception e) { return StreamMode.ACTIVE; }
    }
    boolean edgeGestures() { return prefs.getBoolean("edge_gestures", true); }
    int edgeWidthDp() { return prefs.getInt("edge_width", 12); }
    boolean shakeToMute() { return prefs.getBoolean("shake_to_mute", false); }
    int shakeThreshold() { return prefs.getInt("shake_threshold", 270); }
    int bubbleColorHue() { return prefs.getInt("bubble_color_hue", 265); }
    boolean haptics() { return prefs.getBoolean("haptics", true); }
    boolean visualIndicator() { return prefs.getBoolean("visual_indicator", true); }
    boolean autoNightProfile() { return prefs.getBoolean("auto_night_profile", false); }
    int nightVolumePercent() { return prefs.getInt("night_volume", 25); }
    int nightStartHour() { return prefs.getInt("night_start", 22); }
    int nightEndHour() { return prefs.getInt("night_end", 7); }
    boolean startOnBoot() { return prefs.getBoolean("start_on_boot", true); }
    boolean hideNotification() { return prefs.getBoolean("hide_notification", false); }
    void setHideNotification(boolean hide) { prefs.edit().putBoolean("hide_notification", hide).apply(); }
    boolean backgroundRunning() { return prefs.getBoolean("background_running", false); }
    void setBackgroundRunning(boolean running) { prefs.edit().putBoolean("background_running", running).apply(); }
    int lastAudibleMedia() { return prefs.getInt("last_audible_media", -1); }
    void setLastAudibleMedia(int value) { prefs.edit().putInt("last_audible_media", value).apply(); }
    int preNightVolume() { return prefs.getInt("pre_night_volume", -1); }
    void setPreNightVolume(int value) { prefs.edit().putInt("pre_night_volume", value).apply(); }
    boolean darkMode() { return prefs.getBoolean("dark_mode", false); }
    void setDarkMode(boolean enabled) { prefs.edit().putBoolean("dark_mode", enabled).apply(); }
    boolean onboardingDone() { return prefs.getBoolean("onboarding_done", false); }
    void setOnboardingDone(boolean done) { prefs.edit().putBoolean("onboarding_done", done).apply(); }
    boolean accessibilitySpeech() { return prefs.getBoolean("accessibility_speech", false); }
    int activePreset() { return prefs.getInt("active_preset", -1); }
    void setActivePreset(int preset) { prefs.edit().putInt("active_preset", preset).apply(); }

    void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
    void putBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }
    void putString(String key, String value) { prefs.edit().putString(key, value).apply(); }
}
