package com.example.virtualbuttons;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsStore {
    private final SharedPreferences prefs;
    private static final String PREF_NAME = "gesture_settings";

    public static final int NOTIF_MODE_NORMAL = 0;
    public static final int NOTIF_MODE_MINIMAL = 1;
    public static final int NOTIF_MODE_STEALTH = 2;

    public SettingsStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ============================================================
    // Core Settings (Phase 1 - Always Available)
    // ============================================================

    private static final String KEY_GESTURE_SENSITIVITY = "gesture_sensitivity";
    private static final String KEY_EDGE_WIDTH = "edge_width";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    private static final String KEY_HAPTIC_INTENSITY = "haptic_intensity";
    private static final String KEY_COOLDOWN_MS = "cooldown_ms";
    private static final String KEY_EDGE_GESTURES = "edge_gestures";
    private static final String KEY_DOUBLE_TAP_LOCK = "double_tap_lock";
    private static final String KEY_CORNER_GESTURES = "corner_gestures";
    private static final String KEY_RADIAL_MENU = "radial_menu";
    private static final String KEY_TWO_FINGER_MEDIA = "two_finger_media";
    private static final String KEY_SHOW_INDICATORS = "show_indicators";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_NOTIFICATION_MODE = "notification_mode";
    private static final String KEY_AUTO_RESTART = "auto_restart";
    private static final String KEY_WATCHDOG = "watchdog";
    private static final String KEY_ACC_MONITOR = "acc_monitor";
    private static final String KEY_SERVICE_PROTECTION = "service_protection";
    private static final String KEY_PILL_COLOR = "pill_color";
    private static final String KEY_PILL_OPACITY = "pill_opacity";
    private static final String KEY_PILL_SIZE = "pill_size";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BATTERY_OPT_DISMISSED = "battery_opt_dismissed";

    public int getGestureSensitivity() { return prefs.getInt(KEY_GESTURE_SENSITIVITY, 50); }
    public void setGestureSensitivity(int v) { prefs.edit().putInt(KEY_GESTURE_SENSITIVITY, v).apply(); }

    public int getEdgeWidth() { return prefs.getInt(KEY_EDGE_WIDTH, 24); }
    public void setEdgeWidth(int v) { prefs.edit().putInt(KEY_EDGE_WIDTH, v).apply(); }

    public int getAnimationSpeed() { return prefs.getInt(KEY_ANIMATION_SPEED, 50); }
    public void setAnimationSpeed(int v) { prefs.edit().putInt(KEY_ANIMATION_SPEED, v).apply(); }

    public int getHapticIntensity() { return prefs.getInt(KEY_HAPTIC_INTENSITY, 50); }
    public void setHapticIntensity(int v) { prefs.edit().putInt(KEY_HAPTIC_INTENSITY, v).apply(); }

    public int getCooldownMs() { return prefs.getInt(KEY_COOLDOWN_MS, 200); }
    public void setCooldownMs(int v) { prefs.edit().putInt(KEY_COOLDOWN_MS, v).apply(); }

    public boolean isEdgeGesturesEnabled() { return prefs.getBoolean(KEY_EDGE_GESTURES, true); }
    public void setEdgeGesturesEnabled(boolean v) { prefs.edit().putBoolean(KEY_EDGE_GESTURES, v).apply(); }

    public boolean isDoubleTapLockEnabled() { return prefs.getBoolean(KEY_DOUBLE_TAP_LOCK, true); }
    public void setDoubleTapLockEnabled(boolean v) { prefs.edit().putBoolean(KEY_DOUBLE_TAP_LOCK, v).apply(); }

    public boolean isCornerGesturesEnabled() { return prefs.getBoolean(KEY_CORNER_GESTURES, true); }
    public void setCornerGesturesEnabled(boolean v) { prefs.edit().putBoolean(KEY_CORNER_GESTURES, v).apply(); }

    public boolean isRadialMenuEnabled() { return prefs.getBoolean(KEY_RADIAL_MENU, true); }
    public void setRadialMenuEnabled(boolean v) { prefs.edit().putBoolean(KEY_RADIAL_MENU, v).apply(); }

    public boolean isTwoFingerMediaEnabled() { return prefs.getBoolean(KEY_TWO_FINGER_MEDIA, true); }
    public void setTwoFingerMediaEnabled(boolean v) { prefs.edit().putBoolean(KEY_TWO_FINGER_MEDIA, v).apply(); }

    public boolean isShowIndicators() { return prefs.getBoolean(KEY_SHOW_INDICATORS, true); }
    public void setShowIndicators(boolean v) { prefs.edit().putBoolean(KEY_SHOW_INDICATORS, v).apply(); }

    public boolean isAutoStart() { return prefs.getBoolean(KEY_AUTO_START, false); }
    public void setAutoStart(boolean v) { prefs.edit().putBoolean(KEY_AUTO_START, v).apply(); }

    public int getNotificationMode() { return prefs.getInt(KEY_NOTIFICATION_MODE, NOTIF_MODE_NORMAL); }
    public void setNotificationMode(int v) { prefs.edit().putInt(KEY_NOTIFICATION_MODE, v).apply(); }

    public boolean isAutoRestartEnabled() { return prefs.getBoolean(KEY_AUTO_RESTART, true); }
    public void setAutoRestartEnabled(boolean v) { prefs.edit().putBoolean(KEY_AUTO_RESTART, v).apply(); }

    public boolean isWatchdogEnabled() { return prefs.getBoolean(KEY_WATCHDOG, true); }
    public void setWatchdogEnabled(boolean v) { prefs.edit().putBoolean(KEY_WATCHDOG, v).apply(); }

    public boolean isAccMonitoringEnabled() { return prefs.getBoolean(KEY_ACC_MONITOR, true); }
    public void setAccMonitoringEnabled(boolean v) { prefs.edit().putBoolean(KEY_ACC_MONITOR, v).apply(); }

    public boolean isServiceProtectionEnabled() { return prefs.getBoolean(KEY_SERVICE_PROTECTION, true); }
    public void setServiceProtectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_SERVICE_PROTECTION, v).apply(); }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean v) { prefs.edit().putBoolean(KEY_DARK_MODE, v).apply(); }

    public int getPillColor() { return prefs.getInt(KEY_PILL_COLOR, 0xFFFFFFFF); }
    public void setPillColor(int v) { prefs.edit().putInt(KEY_PILL_COLOR, v).apply(); }

    public int getPillOpacity() { return prefs.getInt(KEY_PILL_OPACITY, 60); }
    public void setPillOpacity(int v) { prefs.edit().putInt(KEY_PILL_OPACITY, v).apply(); }

    public int getPillSize() { return prefs.getInt(KEY_PILL_SIZE, 1); }
    public void setPillSize(int v) { prefs.edit().putInt(KEY_PILL_SIZE, v).apply(); }

    public boolean isBatteryOptDismissed() { return prefs.getBoolean(KEY_BATTERY_OPT_DISMISSED, false); }
    public void setBatteryOptDismissed(boolean v) { prefs.edit().putBoolean(KEY_BATTERY_OPT_DISMISSED, v).apply(); }

    // ============================================================
    // Extension Settings (Phase 2/3 - Optional Features)
    // ============================================================

    private static final String PREFIX_EXT = "ext_";
    private static final String KEY_ANTI_ACCIDENTAL = PREFIX_EXT + "anti_accidental";
    private static final String KEY_ADAPTIVE_LEARNING = PREFIX_EXT + "adaptive_learning";
    private static final String KEY_CONTEXT_AWARE = PREFIX_EXT + "context_aware";
    private static final String KEY_SCROLL_DETECTION = PREFIX_EXT + "scroll_detection";
    private static final String KEY_TYPING_DETECTION = PREFIX_EXT + "typing_detection";
    private static final String KEY_GAMING_MODE = PREFIX_EXT + "gaming_mode";
    private static final String KEY_GAMING_MODE_VALUE = PREFIX_EXT + "gaming_mode_value";
    private static final String KEY_PILL_AUTO_HIDE_GAMING = PREFIX_EXT + "pill_auto_hide_gaming";
    private static final String KEY_EDGE_SHRINK_FULLSCREEN = PREFIX_EXT + "edge_shrink_fullscreen";
    private static final String KEY_RESPECT_BACK_GESTURE = PREFIX_EXT + "respect_back_gesture";

    public boolean isAntiAccidentalEnabled() { return prefs.getBoolean(KEY_ANTI_ACCIDENTAL, false); }
    public void setAntiAccidentalEnabled(boolean v) { prefs.edit().putBoolean(KEY_ANTI_ACCIDENTAL, v).apply(); }

    public boolean isAdaptiveLearningEnabled() { return prefs.getBoolean(KEY_ADAPTIVE_LEARNING, false); }
    public void setAdaptiveLearningEnabled(boolean v) { prefs.edit().putBoolean(KEY_ADAPTIVE_LEARNING, v).apply(); }

    public boolean isContextAwareEnabled() { return prefs.getBoolean(KEY_CONTEXT_AWARE, false); }
    public void setContextAwareEnabled(boolean v) { prefs.edit().putBoolean(KEY_CONTEXT_AWARE, v).apply(); }

    public boolean isScrollDetectionEnabled() { return prefs.getBoolean(KEY_SCROLL_DETECTION, false); }
    public void setScrollDetectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_SCROLL_DETECTION, v).apply(); }

    public boolean isTypingDetectionEnabled() { return prefs.getBoolean(KEY_TYPING_DETECTION, false); }
    public void setTypingDetectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_TYPING_DETECTION, v).apply(); }

    public boolean isGamingModeEnabled() { return prefs.getBoolean(KEY_GAMING_MODE, false); }
    public void setGamingModeEnabled(boolean v) { prefs.edit().putBoolean(KEY_GAMING_MODE, v).apply(); }

    public int getGamingModeValue() { return prefs.getInt(KEY_GAMING_MODE_VALUE, 1); }
    public void setGamingModeValue(int v) { prefs.edit().putInt(KEY_GAMING_MODE_VALUE, v).apply(); }

    public boolean isPillAutoHideGamingEnabled() { return prefs.getBoolean(KEY_PILL_AUTO_HIDE_GAMING, false); }
    public void setPillAutoHideGamingEnabled(boolean v) { prefs.edit().putBoolean(KEY_PILL_AUTO_HIDE_GAMING, v).apply(); }

    public boolean isEdgeShrinkFullscreenEnabled() { return prefs.getBoolean(KEY_EDGE_SHRINK_FULLSCREEN, false); }
    public void setEdgeShrinkFullscreenEnabled(boolean v) { prefs.edit().putBoolean(KEY_EDGE_SHRINK_FULLSCREEN, v).apply(); }

    public boolean isRespectBackGestureEnabled() { return prefs.getBoolean(KEY_RESPECT_BACK_GESTURE, true); }
    public void setRespectBackGestureEnabled(boolean v) { prefs.edit().putBoolean(KEY_RESPECT_BACK_GESTURE, v).apply(); }

    public boolean isAnyExtensionEnabled() {
        return isAntiAccidentalEnabled() || isAdaptiveLearningEnabled() || isContextAwareEnabled()
            || isScrollDetectionEnabled() || isTypingDetectionEnabled() || isGamingModeEnabled();
    }
}
