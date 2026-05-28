package com.example.virtualbuttons.gesture;

import android.view.MotionEvent;

public class GestureZoneManager {
    private int screenWidth, screenHeight;
    private int baseEdgeWidthPx;
    private int currentEdgeWidthPx;
    private int cornerSizePx;
    private float density;

    public static final int ZONE_FULL = 0;
    public static final int ZONE_REDUCED = 1;
    public static final int ZONE_MINIMAL = 2;
    public static final int ZONE_DISABLED = 3;

    private int zoneState = ZONE_FULL;
    private int activeContext = AppContextDetector.CONTEXT_NORMAL;

    private static final int ANDROID_BACK_GESTURE_WIDTH_PX = 40;
    private static final int ANDROID_BACK_GESTURE_OFFSET_TOP = 0;
    private static final int ANDROID_BACK_GESTURE_OFFSET_BOTTOM = 200;

    private float pillX, pillY;
    private int pillWidth, pillHeight;

    public GestureZoneManager(int screenWidth, int screenHeight, float density) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.density = density;
        this.baseEdgeWidthPx = (int) (24 * density);
        this.currentEdgeWidthPx = baseEdgeWidthPx;
        this.cornerSizePx = (int) (48 * density);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setBaseEdgeWidth(int dp) {
        this.baseEdgeWidthPx = (int) (dp * density);
        recalcZone();
    }

    public void setActiveContext(int context) {
        this.activeContext = context;
        recalcZone();
    }

    public void setPillPosition(float x, float y, int w, int h) {
        this.pillX = x;
        this.pillY = y;
        this.pillWidth = w;
        this.pillHeight = h;
    }

    private void recalcZone() {
        switch (activeContext) {
            case AppContextDetector.CONTEXT_GAMING:
                zoneState = ZONE_MINIMAL;
                currentEdgeWidthPx = baseEdgeWidthPx / 3;
                break;
            case AppContextDetector.CONTEXT_RAPID_TOUCH:
                zoneState = ZONE_REDUCED;
                currentEdgeWidthPx = baseEdgeWidthPx / 2;
                break;
            case AppContextDetector.CONTEXT_SCROLLING:
                zoneState = ZONE_REDUCED;
                currentEdgeWidthPx = (int) (baseEdgeWidthPx * 0.6f);
                break;
            case AppContextDetector.CONTEXT_TYPING:
                zoneState = ZONE_REDUCED;
                currentEdgeWidthPx = (int) (baseEdgeWidthPx * 0.7f);
                break;
            case AppContextDetector.CONTEXT_VIDEO:
                zoneState = ZONE_MINIMAL;
                currentEdgeWidthPx = baseEdgeWidthPx / 3;
                break;
            default:
                zoneState = ZONE_FULL;
                currentEdgeWidthPx = baseEdgeWidthPx;
                break;
        }
    }

    public boolean isInGestureZone(float x, float y) {
        if (zoneState == ZONE_DISABLED) return false;

        boolean inLeftZone = x <= currentEdgeWidthPx;
        boolean inRightZone = x >= screenWidth - currentEdgeWidthPx;
        boolean inTopZone = y <= currentEdgeWidthPx;
        boolean inBottomZone = y >= screenHeight - currentEdgeWidthPx;

        boolean inCorner = (x <= cornerSizePx && y <= cornerSizePx) ||
            (x >= screenWidth - cornerSizePx && y <= cornerSizePx) ||
            (x <= cornerSizePx && y >= screenHeight - cornerSizePx) ||
            (x >= screenWidth - cornerSizePx && y >= screenHeight - cornerSizePx);

        if (inCorner && zoneState != ZONE_DISABLED) return true;

        if (inLeftZone && !isAndroidBackGestureZone(x, y)) return true;
        if (inRightZone && !isAndroidBackGestureZone(x, y)) return true;

        return inTopZone || inBottomZone;
    }

    private boolean isAndroidBackGestureZone(float x, float y) {
        if (x <= ANDROID_BACK_GESTURE_WIDTH_PX * density) {
            if (y > ANDROID_BACK_GESTURE_OFFSET_TOP &&
                y < screenHeight - ANDROID_BACK_GESTURE_OFFSET_BOTTOM * density) {
                return zoneState != ZONE_FULL;
            }
        }
        return false;
    }

    public boolean shouldPillAutoHide() {
        return zoneState == ZONE_MINIMAL || zoneState == ZONE_DISABLED;
    }

    public int getCurrentEdgeWidth() {
        return currentEdgeWidthPx;
    }

    public int getZoneState() {
        return zoneState;
    }

    public void setZoneState(int state) {
        this.zoneState = state;
    }

    public void resetToFull() {
        zoneState = ZONE_FULL;
        currentEdgeWidthPx = baseEdgeWidthPx;
    }
}
