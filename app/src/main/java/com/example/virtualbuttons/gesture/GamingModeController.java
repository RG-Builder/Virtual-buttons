package com.example.virtualbuttons.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

public class GamingModeController {
    public static final int MODE_OFF = 0;
    public static final int MODE_AUTO = 1;
    public static final int MODE_ON = 2;

    private int mode = MODE_AUTO;
    private boolean isActive = false;
    private long lastActivityTime;
    private int rapidTouchCount;
    private long modeStartedTime;
    private static final long INACTIVITY_TIMEOUT_MS = 30000;
    private static final int RAPID_TOUCH_TRIGGER = 10;
    private static final long RAPID_TOUCH_WINDOW_MS = 2000;

    private long[] touchTimes = new long[15];
    private int touchIndex = 0;
    private int multiTouchPeak = 0;

    public GamingModeController() {}

    public void setMode(int mode) {
        this.mode = mode;
        if (mode == MODE_OFF) {
            isActive = false;
        } else if (mode == MODE_ON) {
            isActive = true;
            modeStartedTime = System.currentTimeMillis();
        }
    }

    public void onTouchEvent(MotionEvent event) {
        if (mode != MODE_AUTO) return;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                recordTouch();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int count = event.getPointerCount();
                if (count > multiTouchPeak) multiTouchPeak = count;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        updateState();
    }

    private void recordTouch() {
        touchTimes[touchIndex % touchTimes.length] = System.currentTimeMillis();
        touchIndex++;
    }

    private void updateState() {
        long now = System.currentTimeMillis();
        int recentTouches = 0;
        for (int i = 0; i < Math.min(touchIndex, touchTimes.length); i++) {
            int idx = ((touchIndex - 1 - i) + touchTimes.length) % touchTimes.length;
            if (now - touchTimes[idx] < RAPID_TOUCH_WINDOW_MS) {
                recentTouches++;
            }
        }

        boolean shouldBeActive = recentTouches >= RAPID_TOUCH_TRIGGER
            || multiTouchPeak >= 3;

        if (shouldBeActive && !isActive) {
            isActive = true;
            modeStartedTime = now;
        } else if (isActive && (now - lastActivityTime > INACTIVITY_TIMEOUT_MS)) {
            isActive = false;
            multiTouchPeak = 0;
        }

        if (isActive) {
            lastActivityTime = now;
        }
    }

    public void touchActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        if (mode == MODE_OFF) return false;
        if (mode == MODE_ON) return true;
        return isActive;
    }

    public int getMode() {
        return mode;
    }

    public void reset() {
        isActive = false;
        touchIndex = 0;
        multiTouchPeak = 0;
        rapidTouchCount = 0;
    }
}
