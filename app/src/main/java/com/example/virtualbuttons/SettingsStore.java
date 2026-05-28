package com.example.virtualbuttons;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsStore {
    private static final String PREF_NAME = "virtual_buttons_gesture";
    private static final String KEY_PILL_ENABLED = "pill_enabled";
    private static final String KEY_EDGE_GESTURES_ENABLED = "edge_gestures_enabled";
    private static final String KEY_GESTURE_SENSITIVITY = "gesture_sensitivity";
    private static final String KEY_EDGE_WIDTH = "edge_width";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    private static final String KEY_HAPTIC_INTENSITY = "haptic_intensity";
    private static final String KEY_PILL_COLOR = "pill_color";
    private static final String KEY_PILL_OPACITY = "pill_opacity";
    private static final String KEY_PILL_SIZE = "pill_size";
    private static final String KEY_DOUBLE_TAP_LOCK = "double_tap_lock";
    private static final String KEY_CORNER_GESTURES = "corner_gestures";
    private static final String KEY_RADIAL_MENU = "radial_menu";
    private static final String KEY_TWO_FINGER_MEDIA = "two_finger_media";
    private static final String KEY_VOLUME_STREAM = "volume_stream";
    private static final String KEY_SHOW_INDICATORS = "show_indicators";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_COOLDOWN_MS = "cooldown_ms";

    private static final String KEY_NOTIFICATION_MODE = "notification_mode";
    private static final String KEY_AUTO_RESTART = "auto_restart";
    private static final String KEY_SERVICE_PROTECTION = "service_protection";
    private static final String KEY_ACC_MONITORING = "acc_monitoring";
    private static final String KEY_BATTERY_OPT_DISMISSED = "battery_opt_dismissed";
    private static final String KEY_WATCHDOG_ENABLED = "watchdog_enabled";

    private static final String KEY_CONTEXT_AWARE = "context_aware";
    private static final String KEY_ANTI_ACCIDENTAL = "anti_accidental";
    private static final String KEY_ADAPTIVE_LEARNING = "adaptive_learning";
    private static final String KEY_GAMING_MODE = "gaming_mode";
    private static final String KEY_GAMING_MODE_VALUE = "gaming_mode_value";
    private static final String KEY_SCROLL_DETECTION = "scroll_detection";
    private static final String KEY_TYPING_DETECTION = "typing_detection";
    private static final String KEY_RESPECT_BACK_GESTURE = "respect_back_gesture";
    private static final String KEY_EDGE_SHRINK_FULLSCREEN = "edge_shrink_fullscreen";
    private static final String KEY_PILL_AUTO_HIDE_GAMING = "pill_auto_hide_gaming";

    public static final int NOTIF_MODE_NORMAL = 0;
    public static final int NOTIF_MODE_MINIMAL = 1;
    public static final int NOTIF_MODE_STEALTH = 2;
    public static final int GAMING_MODE_OFF = 0;
    public static final int GAMING_MODE_AUTO = 1;
    public static final int GAMING_MODE_ON = 2;

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPillEnabled() { return prefs.getBoolean(KEY_PILL_ENABLED, true); }
    public void setPillEnabled(boolean v) { prefs.edit().putBoolean(KEY_PILL_ENABLED, v).apply(); }

    public boolean isEdgeGesturesEnabled() { return prefs.getBoolean(KEY_EDGE_GESTURES_ENABLED, true); }
    public void setEdgeGesturesEnabled(boolean v) { prefs.edit().putBoolean(KEY_EDGE_GESTURES_ENABLED, v).apply(); }

    public int getGestureSensitivity() { return prefs.getInt(KEY_GESTURE_SENSITIVITY, 50); }
    public void setGestureSensitivity(int v) { prefs.edit().putInt(KEY_GESTURE_SENSITIVITY, v).apply(); }

    public int getEdgeWidth() { return prefs.getInt(KEY_EDGE_WIDTH, 24); }
    public void setEdgeWidth(int v) { prefs.edit().putInt(KEY_EDGE_WIDTH, v).apply(); }

    public int getAnimationSpeed() { return prefs.getInt(KEY_ANIMATION_SPEED, 50); }
    public void setAnimationSpeed(int v) { prefs.edit().putInt(KEY_ANIMATION_SPEED, v).apply(); }

    public int getHapticIntensity() { return prefs.getInt(KEY_HAPTIC_INTENSITY, 50); }
    public void setHapticIntensity(int v) { prefs.edit().putInt(KEY_HAPTIC_INTENSITY, v).apply(); }

    public int getPillColor() { return prefs.getInt(KEY_PILL_COLOR, 0xFF6750A4); }
    public void setPillColor(int v) { prefs.edit().putInt(KEY_PILL_COLOR, v).apply(); }

    public int getPillOpacity() { return prefs.getInt(KEY_PILL_OPACITY, 70); }
    public void setPillOpacity(int v) { prefs.edit().putInt(KEY_PILL_OPACITY, v).apply(); }

    public int getPillSize() { return prefs.getInt(KEY_PILL_SIZE, 48); }
    public void setPillSize(int v) { prefs.edit().putInt(KEY_PILL_SIZE, v).apply(); }

    public boolean isDoubleTapLockEnabled() { return prefs.getBoolean(KEY_DOUBLE_TAP_LOCK, true); }
    public void setDoubleTapLockEnabled(boolean v) { prefs.edit().putBoolean(KEY_DOUBLE_TAP_LOCK, v).apply(); }

    public boolean isCornerGesturesEnabled() { return prefs.getBoolean(KEY_CORNER_GESTURES, true); }
    public void setCornerGesturesEnabled(boolean v) { prefs.edit().putBoolean(KEY_CORNER_GESTURES, v).apply(); }

    public boolean isRadialMenuEnabled() { return prefs.getBoolean(KEY_RADIAL_MENU, true); }
    public void setRadialMenuEnabled(boolean v) { prefs.edit().putBoolean(KEY_RADIAL_MENU, v).apply(); }

    public boolean isTwoFingerMediaEnabled() { return prefs.getBoolean(KEY_TWO_FINGER_MEDIA, true); }
    public void setTwoFingerMediaEnabled(boolean v) { prefs.edit().putBoolean(KEY_TWO_FINGER_MEDIA, v).apply(); }

    public int getVolumeStream() { return prefs.getInt(KEY_VOLUME_STREAM, 0); }
    public void setVolumeStream(int v) { prefs.edit().putInt(KEY_VOLUME_STREAM, v).apply(); }

    public boolean isShowIndicators() { return prefs.getBoolean(KEY_SHOW_INDICATORS, true); }
    public void setShowIndicators(boolean v) { prefs.edit().putBoolean(KEY_SHOW_INDICATORS, v).apply(); }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean v) { prefs.edit().putBoolean(KEY_DARK_MODE, v).apply(); }

    public boolean isAutoStart() { return prefs.getBoolean(KEY_AUTO_START, true); }
    public void setAutoStart(boolean v) { prefs.edit().putBoolean(KEY_AUTO_START, v).apply(); }

    public int getCooldownMs() { return prefs.getInt(KEY_COOLDOWN_MS, 200); }
    public void setCooldownMs(int v) { prefs.edit().putInt(KEY_COOLDOWN_MS, v).apply(); }

    public int getNotificationMode() { return prefs.getInt(KEY_NOTIFICATION_MODE, NOTIF_MODE_NORMAL); }
    public void setNotificationMode(int v) { prefs.edit().putInt(KEY_NOTIFICATION_MODE, v).apply(); }

    public boolean isAutoRestartEnabled() { return prefs.getBoolean(KEY_AUTO_RESTART, true); }
    public void setAutoRestartEnabled(boolean v) { prefs.edit().putBoolean(KEY_AUTO_RESTART, v).apply(); }

    public boolean isServiceProtectionEnabled() { return prefs.getBoolean(KEY_SERVICE_PROTECTION, true); }
    public void setServiceProtectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_SERVICE_PROTECTION, v).apply(); }

    public boolean isAccMonitoringEnabled() { return prefs.getBoolean(KEY_ACC_MONITORING, true); }
    public void setAccMonitoringEnabled(boolean v) { prefs.edit().putBoolean(KEY_ACC_MONITORING, v).apply(); }

    public boolean isBatteryOptDismissed() { return prefs.getBoolean(KEY_BATTERY_OPT_DISMISSED, false); }
    public void setBatteryOptDismissed(boolean v) { prefs.edit().putBoolean(KEY_BATTERY_OPT_DISMISSED, v).apply(); }

    public boolean isWatchdogEnabled() { return prefs.getBoolean(KEY_WATCHDOG_ENABLED, true); }
    public void setWatchdogEnabled(boolean v) { prefs.edit().putBoolean(KEY_WATCHDOG_ENABLED, v).apply(); }

    public boolean isContextAwareEnabled() { return prefs.getBoolean(KEY_CONTEXT_AWARE, true); }
    public void setContextAwareEnabled(boolean v) { prefs.edit().putBoolean(KEY_CONTEXT_AWARE, v).apply(); }

    public boolean isAntiAccidentalEnabled() { return prefs.getBoolean(KEY_ANTI_ACCIDENTAL, true); }
    public void setAntiAccidentalEnabled(boolean v) { prefs.edit().putBoolean(KEY_ANTI_ACCIDENTAL, v).apply(); }

    public boolean isAdaptiveLearningEnabled() { return prefs.getBoolean(KEY_ADAPTIVE_LEARNING, true); }
    public void setAdaptiveLearningEnabled(boolean v) { prefs.edit().putBoolean(KEY_ADAPTIVE_LEARNING, v).apply(); }

    public boolean isGamingModeEnabled() { return prefs.getBoolean(KEY_GAMING_MODE, true); }
    public void setGamingModeEnabled(boolean v) { prefs.edit().putBoolean(KEY_GAMING_MODE, v).apply(); }

    public int getGamingModeValue() { return prefs.getInt(KEY_GAMING_MODE_VALUE, GAMING_MODE_AUTO); }
    public void setGamingModeValue(int v) { prefs.edit().putInt(KEY_GAMING_MODE_VALUE, v).apply(); }

    public boolean isScrollDetectionEnabled() { return prefs.getBoolean(KEY_SCROLL_DETECTION, true); }
    public void setScrollDetectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_SCROLL_DETECTION, v).apply(); }

    public boolean isTypingDetectionEnabled() { return prefs.getBoolean(KEY_TYPING_DETECTION, true); }
    public void setTypingDetectionEnabled(boolean v) { prefs.edit().putBoolean(KEY_TYPING_DETECTION, v).apply(); }

    public boolean isRespectBackGestureEnabled() { return prefs.getBoolean(KEY_RESPECT_BACK_GESTURE, true); }
    public void setRespectBackGestureEnabled(boolean v) { prefs.edit().putBoolean(KEY_RESPECT_BACK_GESTURE, v).apply(); }

    public boolean isEdgeShrinkFullscreenEnabled() { return prefs.getBoolean(KEY_EDGE_SHRINK_FULLSCREEN, true); }
    public void setEdgeShrinkFullscreenEnabled(boolean v) { prefs.edit().putBoolean(KEY_EDGE_SHRINK_FULLSCREEN, v).apply(); }

    public boolean isPillAutoHideGamingEnabled() { return prefs.getBoolean(KEY_PILL_AUTO_HIDE_GAMING, true); }
    public void setPillAutoHideGamingEnabled(boolean v) { prefs.edit().putBoolean(KEY_PILL_AUTO_HIDE_GAMING, v).apply(); }

    public boolean startOnBoot() { return isAutoStart(); }
    public boolean overlayEnabled() { return isPillEnabled(); }
    public void setOverlayEnabled(boolean v) { setPillEnabled(v); }
    public boolean backgroundRunning() { return isPillEnabled(); }
}
