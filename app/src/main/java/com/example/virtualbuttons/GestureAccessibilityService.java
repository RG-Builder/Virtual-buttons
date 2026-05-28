package com.example.virtualbuttons;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class GestureAccessibilityService extends AccessibilityService {
    private static GestureAccessibilityService instance;
    private static boolean serviceWasRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        serviceWasRunning = true;
        isConnected = true;
    }

    @Override
    public void onDestroy() {
        isConnected = false;
        instance = null;
        super.onDestroy();

        handler.postDelayed(() -> {
            if (!isConnected && serviceWasRunning) {
                SettingsStore settings = new SettingsStore(GestureAccessibilityService.this);
                if (settings.isAccMonitoringEnabled() && settings.isPillEnabled()) {
                    Intent intent = new Intent(this, ModernMainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("show_accessibility_prompt", true);
                    startActivity(intent);
                }
            }
        }, 2000);
    }

    public static GestureAccessibilityService getInstance() {
        return instance;
    }

    public static boolean wasRunning() {
        return serviceWasRunning;
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    public void performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public void performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public void performRecentApps() {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public void performLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    public void performScreenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }

    public void performQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    public void performNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public void performPowerDialog() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }
}
