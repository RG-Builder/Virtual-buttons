package com.example.virtualbuttons;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.accessibility.AccessibilityEvent;

public class VirtualButtonAccessibilityService extends AccessibilityService {
    private static volatile VirtualButtonAccessibilityService instance;

    static VirtualButtonAccessibilityService getInstance() { return instance; }

    private SettingsStore settings;
    private AudioManager audioManager;
    private int maxVolume;
    private PowerManager powerManager;
    private long lastBackPress = 0;
    private long lastHomePress = 0;
    private long lastRecentsPress = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        settings = new SettingsStore(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public boolean performAction(String action) {
        switch (action) {
            case ActionManager.ACTION_BUTTON_POWER:
                return performPowerAction();
            case ActionManager.ACTION_BUTTON_HOME:
                return performHomeAction();
            case ActionManager.ACTION_BUTTON_RECENTS:
                return performRecentsAction();
            case ActionManager.ACTION_BUTTON_BACK:
                return performBackAction();
            case ActionManager.ACTION_VOLUME_UP:
                return performVolumeUp();
            case ActionManager.ACTION_VOLUME_DOWN:
                return performVolumeDown();
            default:
                return false;
        }
    }

    private boolean performPowerAction() {
        haptic();
        try {
            Runtime.getRuntime().exec("input keyevent KEYCODE_POWER");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean performHomeAction() {
        haptic();
        long now = System.currentTimeMillis();
        if (now - lastHomePress < 500) return false;
        lastHomePress = now;
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private boolean performRecentsAction() {
        haptic();
        long now = System.currentTimeMillis();
        if (now - lastRecentsPress < 500) return false;
        lastRecentsPress = now;
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    private boolean performBackAction() {
        haptic();
        long now = System.currentTimeMillis();
        if (now - lastBackPress < 300) return false;
        lastBackPress = now;
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private boolean performVolumeUp() {
        haptic();
        if (audioManager != null) {
            int step = Math.max(1, settings.volumeStep());
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int next = Math.min(maxVolume, current + step);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);
            return true;
        }
        return false;
    }

    private boolean performVolumeDown() {
        haptic();
        if (audioManager != null) {
            int step = Math.max(1, settings.volumeStep());
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int next = Math.max(0, current - step);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);
            return true;
        }
        return false;
    }

    private void haptic() {
        if (settings != null && settings.hapticFeedback()) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(15);
                }
            }
        }
    }
}