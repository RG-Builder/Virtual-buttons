package com.example.virtualbuttons.core;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.example.virtualbuttons.extensions.ExtensionManager;

public class SimpleGestureEngine {
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    public static final int EDGE_BOTTOM = 8;

    public static final int GESTURE_NONE = 0;
    public static final int GESTURE_SWIPE_UP = 1;
    public static final int GESTURE_SWIPE_DOWN = 2;
    public static final int GESTURE_TAP = 3;
    public static final int GESTURE_DOUBLE_TAP = 4;
    public static final int GESTURE_LONG_PRESS = 5;
    public static final int GESTURE_DRAG = 6;

    public interface GestureListener {
        void onGestureDetected(int gestureType, int edge, float x, float y, float velocity);
        void onDragProgress(int edge, float delta, float total, float velocity);
        void onDragEnd(int edge, float total, float velocity);
        void onTap(float x, float y);
        void onDoubleTap(float x, float y);
        void onLongPress(float x, float y);
    }

    private GestureListener listener;
    private VelocityTracker velocityTracker;
    private ExtensionManager extensionManager;

    private int screenWidth;
    private int screenHeight;
    private int edgeWidthPx;
    private float sensitivity = 1.0f;
    private int cooldownMs = 200;
    private long lastGestureTime;
    private int pointerId;

    private float downX, downY;
    private long downTime;
    private boolean isDragging;
    private boolean isLongPress;
    private float lastDragY;
    private int activeEdge;

    private static final int SWIPE_THRESHOLD_BASE = 120;
    private static final float VELOCITY_THRESHOLD = 180f;
    private static final int TAP_TIMEOUT = 300;
    private static final int DOUBLE_TAP_TIMEOUT = 350;
    private static final int LONG_PRESS_TIMEOUT = 500;
    private static final int DRAG_START_THRESHOLD = 20;

    private float lastTapX, lastTapY;
    private long lastTapTime;
    private boolean waitingDoubleTap;

    public SimpleGestureEngine(int screenWidth, int screenHeight, float density) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.edgeWidthPx = (int) (24 * density);
    }

    public void setListener(GestureListener listener) {
        this.listener = listener;
    }

    public void setExtensionManager(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.3f, Math.min(3f, sensitivity));
    }

    public void setEdgeWidth(int dp, float density) {
        this.edgeWidthPx = (int) (dp * density);
    }

    public void setCooldownMs(int cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public int detectEdge(float x, float y) {
        int edge = 0;
        if (x <= edgeWidthPx) edge |= EDGE_LEFT;
        if (x >= screenWidth - edgeWidthPx) edge |= EDGE_RIGHT;
        if (y <= edgeWidthPx) edge |= EDGE_TOP;
        if (y >= screenHeight - edgeWidthPx) edge |= EDGE_BOTTOM;
        return edge;
    }

    private int getSwipeThreshold() {
        return (int) (SWIPE_THRESHOLD_BASE * (2f - sensitivity));
    }

    private boolean isInCooldown() {
        return System.currentTimeMillis() - lastGestureTime < cooldownMs;
    }

    public void onTouchEvent(MotionEvent event) {
        if (extensionManager != null) {
            extensionManager.onTouchEvent(event);
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                pointerId = event.getPointerId(0);
                downX = event.getX();
                downY = event.getY();
                downTime = event.getEventTime();
                isDragging = false;
                isLongPress = false;
                lastDragY = downY;
                activeEdge = detectEdge(downX, downY);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (event.getPointerCount() >= 2 && listener != null && !isInCooldown()) {
                    float y1 = event.getY(0);
                    float y2 = event.getY(1);
                    float dy = y2 - y1;
                    if (Math.abs(dy) > SWIPE_THRESHOLD_BASE) {
                        int gestureType = dy < 0 ? GESTURE_SWIPE_UP : GESTURE_SWIPE_DOWN;
                        listener.onGestureDetected(gestureType, 0,
                            (event.getX(0) + event.getX(1)) / 2f,
                            (y1 + y2) / 2f, Math.abs(dy));
                        lastGestureTime = System.currentTimeMillis();
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float y = event.getY();
                float dx = x - downX;
                float dy = y - downY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (!isDragging && dist > DRAG_START_THRESHOLD) {
                    isDragging = true;
                }

                if (isDragging && !isLongPress) {
                    if (listener != null && (activeEdge & (EDGE_LEFT | EDGE_RIGHT)) != 0) {
                        float delta = y - lastDragY;
                        float total = y - downY;
                        velocityTracker.computeCurrentVelocity(1000);
                        float vel = velocityTracker.getYVelocity(pointerId);
                        listener.onDragProgress(activeEdge, delta, total, vel);
                    }
                    lastDragY = y;
                }

                if (!isLongPress && !isDragging &&
                    event.getEventTime() - downTime > LONG_PRESS_TIMEOUT) {
                    isLongPress = true;
                    if (listener != null) {
                        listener.onLongPress(downX, downY);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                float x = event.getX();
                float y = event.getY();
                float dx = x - downX;
                float dy = y - downY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                long elapsed = event.getEventTime() - downTime;

                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velY = velocityTracker.getYVelocity(pointerId);
                    float vel = Math.abs(velY);

                    if (isDragging && dist >= getSwipeThreshold() && vel >= VELOCITY_THRESHOLD) {
                        if (!isInCooldown()) {
                            int gestureType = dy < 0 ? GESTURE_SWIPE_UP : GESTURE_SWIPE_DOWN;
                            if (listener != null) {
                                listener.onGestureDetected(gestureType, activeEdge, x, y, vel);
                            }
                            lastGestureTime = System.currentTimeMillis();
                        }
                    } else if (isDragging && (activeEdge & (EDGE_LEFT | EDGE_RIGHT)) != 0) {
                        if (listener != null) {
                            listener.onDragEnd(activeEdge, dy, velY);
                        }
                    }

                    if (!isDragging && dist < DRAG_START_THRESHOLD && elapsed < LONG_PRESS_TIMEOUT) {
                        handleTap(x, y);
                    }

                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                isDragging = false;
                isLongPress = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isDragging = false;
                isLongPress = false;
                break;
            }
        }
    }

    private void handleTap(float x, float y) {
        long now = System.currentTimeMillis();
        if (waitingDoubleTap) {
            float dx = x - lastTapX;
            float dy = y - lastTapY;
            if (now - lastTapTime < DOUBLE_TAP_TIMEOUT && Math.sqrt(dx * dx + dy * dy) < 100) {
                waitingDoubleTap = false;
                if (listener != null && !isInCooldown()) {
                    listener.onDoubleTap(x, y);
                    lastGestureTime = System.currentTimeMillis();
                }
                return;
            }
            waitingDoubleTap = false;
        }
        if (now - lastTapTime < TAP_TIMEOUT) {
            waitingDoubleTap = true;
            lastTapX = x;
            lastTapY = y;
            lastTapTime = now;
        } else {
            if (listener != null && !isInCooldown()) {
                listener.onTap(x, y);
            }
            lastTapX = x;
            lastTapY = y;
            lastTapTime = now;
        }
    }

    public void reset() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        isDragging = false;
        isLongPress = false;
        waitingDoubleTap = false;
        if (extensionManager != null) {
            extensionManager.reset();
        }
    }
}
