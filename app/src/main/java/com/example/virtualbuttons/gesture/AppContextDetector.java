package com.example.virtualbuttons.gesture;

import android.view.MotionEvent;

public class AppContextDetector {
    public static final int CONTEXT_NORMAL = 0;
    public static final int CONTEXT_SCROLLING = 1;
    public static final int CONTEXT_GAMING = 2;
    public static final int CONTEXT_TYPING = 3;
    public static final int CONTEXT_VIDEO = 4;
    public static final int CONTEXT_RAPID_TOUCH = 5;

    private int currentContext = CONTEXT_NORMAL;
    private int previousContext = CONTEXT_NORMAL;

    private long[] touchTimestamps = new long[20];
    private int touchIndex = 0;
    private int touchCount = 0;
    private float[] lastDeltas = new float[5];
    private int deltaIndex = 0;
    private float lastX, lastY;
    private long lastTouchTime;
    private int multiTouchCount;

    private static final long SCROLLING_WINDOW_MS = 300;
    private static final int SCROLLING_THRESHOLD = 3;
    private static final int GAMING_TOUCH_RATE = 8;
    private static final long GAMING_WINDOW_MS = 1000;
    private static final long TYPING_GAP_MS = 80;
    private static final int TYPING_MIN_COUNT = 4;
    private static final float VIDEO_ASPECT_TOLERANCE = 0.15f;
    private static final float GAMING_DIAGONAL_TOLERANCE = 0.7f;

    private float screenWidth, screenHeight;

    public AppContextDetector(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void onTouchEvent(MotionEvent event) {
        long now = System.currentTimeMillis();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                recordTouch(now);
                lastX = event.getX();
                lastY = event.getY();
                lastTouchTime = now;
                multiTouchCount = 1;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouchCount++;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                multiTouchCount--;
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchCount > 0) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    lastDeltas[deltaIndex % lastDeltas.length] = (float) Math.sqrt(dx*dx + dy*dy);
                    deltaIndex++;
                    lastX = event.getX();
                    lastY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        updateContext();
    }

    private void recordTouch(long timestamp) {
        touchTimestamps[touchIndex % touchTimestamps.length] = timestamp;
        touchIndex++;
        touchCount++;
    }

    private void updateContext() {
        previousContext = currentContext;

        if (isGaming()) {
            currentContext = CONTEXT_GAMING;
        } else if (isRapidTouch()) {
            currentContext = CONTEXT_RAPID_TOUCH;
        } else if (isScrolling()) {
            currentContext = CONTEXT_SCROLLING;
        } else if (isTyping()) {
            currentContext = CONTEXT_TYPING;
        } else if (isVideo()) {
            currentContext = CONTEXT_VIDEO;
        } else {
            currentContext = CONTEXT_NORMAL;
        }
    }

    private boolean isGaming() {
        if (multiTouchCount >= 2 && touchCount > 0) {
            long now = System.currentTimeMillis();
            int recentTouches = 0;
            for (int i = 0; i < Math.min(touchCount, touchTimestamps.length); i++) {
                int idx = ((touchIndex - 1 - i) + touchTimestamps.length) % touchTimestamps.length;
                if (now - touchTimestamps[idx] < GAMING_WINDOW_MS) {
                    recentTouches++;
                }
            }
            if (recentTouches >= GAMING_TOUCH_RATE) return true;
        }

        if (deltaIndex >= 3) {
            float avgDelta = 0;
            int samples = Math.min(deltaIndex, lastDeltas.length);
            for (int i = 0; i < samples; i++) {
                avgDelta += lastDeltas[i];
            }
            avgDelta /= samples;
            if (avgDelta > screenHeight * 0.3f && touchCount > 5) return true;
        }

        return false;
    }

    private boolean isRapidTouch() {
        long now = System.currentTimeMillis();
        int recentTouches = 0;
        for (int i = 0; i < Math.min(touchCount, touchTimestamps.length); i++) {
            int idx = ((touchIndex - 1 - i) + touchTimestamps.length) % touchTimestamps.length;
            if (now - touchTimestamps[idx] < 500) {
                recentTouches++;
            }
        }
        return recentTouches >= 6;
    }

    private boolean isScrolling() {
        if (deltaIndex < 2) return false;
        float avgDelta = 0;
        int samples = Math.min(deltaIndex, lastDeltas.length);
        for (int i = 0; i < samples; i++) {
            avgDelta += lastDeltas[i];
        }
        avgDelta /= samples;
        return avgDelta > 5f && avgDelta < screenHeight * 0.15f;
    }

    private boolean isTyping() {
        if (touchCount < TYPING_MIN_COUNT) return false;
        long now = System.currentTimeMillis();
        int typed = 0;
        for (int i = 0; i < Math.min(touchCount, touchTimestamps.length); i++) {
            int idx = ((touchIndex - 1 - i) + touchTimestamps.length) % touchTimestamps.length;
            long gap = (i > 0) ? touchTimestamps[idx] - touchTimestamps[(idx - 1 + touchTimestamps.length) % touchTimestamps.length] : 0;
            if (gap > 0 && gap < TYPING_GAP_MS * 3) {
                typed++;
            }
        }
        return typed >= TYPING_MIN_COUNT;
    }

    private boolean isVideo() {
        return false;
    }

    public int getCurrentContext() {
        return currentContext;
    }

    public int getPreviousContext() {
        return previousContext;
    }

    public boolean isContext(int context) {
        return currentContext == context;
    }

    public boolean justTransitionedTo(int context) {
        return currentContext == context && previousContext != context;
    }

    public boolean justTransitionedFrom(int context) {
        return previousContext == context && currentContext != context;
    }

    public float getActiveSensitivityMultiplier() {
        switch (currentContext) {
            case CONTEXT_GAMING: return 0.3f;
            case CONTEXT_RAPID_TOUCH: return 0.4f;
            case CONTEXT_SCROLLING: return 0.5f;
            case CONTEXT_TYPING: return 0.6f;
            case CONTEXT_VIDEO: return 0.5f;
            default: return 1.0f;
        }
    }

    public int getMinSwipeDistanceMultiplier() {
        switch (currentContext) {
            case CONTEXT_GAMING: return 3;
            case CONTEXT_RAPID_TOUCH: return 2;
            case CONTEXT_SCROLLING: return 2;
            case CONTEXT_TYPING: return 2;
            case CONTEXT_VIDEO: return 2;
            default: return 1;
        }
    }

    public String getContextName() {
        switch (currentContext) {
            case CONTEXT_GAMING: return "Gaming";
            case CONTEXT_RAPID_TOUCH: return "Rapid Touch";
            case CONTEXT_SCROLLING: return "Scrolling";
            case CONTEXT_TYPING: return "Typing";
            case CONTEXT_VIDEO: return "Video";
            default: return "Normal";
        }
    }

    public void reset() {
        touchCount = 0;
        touchIndex = 0;
        deltaIndex = 0;
        multiTouchCount = 0;
        currentContext = CONTEXT_NORMAL;
        previousContext = CONTEXT_NORMAL;
    }
}
