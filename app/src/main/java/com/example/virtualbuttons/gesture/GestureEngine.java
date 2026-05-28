package com.example.virtualbuttons.gesture;

import android.view.MotionEvent;
import android.view.VelocityTracker;

public class GestureEngine {
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    public static final int EDGE_BOTTOM = 8;
    public static final int CORNER_TOP_LEFT = 16;
    public static final int CORNER_TOP_RIGHT = 32;
    public static final int CORNER_BOTTOM_LEFT = 64;
    public static final int CORNER_BOTTOM_RIGHT = 128;

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
    private int screenWidth;
    private int screenHeight;
    private int edgeWidthPx;
    private int cornerSizePx;
    private float sensitivity;
    private int cooldownMs;
    private long lastGestureTime;
    private int pointerId;

    private float downX, downY;
    private long downTime;
    private boolean isDragging;
    private boolean isLongPress;
    private float lastDragY;
    private int activeEdge;

    private static final int TAP_TIMEOUT = 300;
    private static final int DOUBLE_TAP_TIMEOUT = 350;
    private static final int LONG_PRESS_TIMEOUT = 500;
    private static final int BASE_SWIPE_THRESHOLD = 120;
    private static final float BASE_VELOCITY_THRESHOLD = 180f;
    private static final int CORNER_SIZE_DP = 48;
    private static final float MIN_DIRECTION_CONFIDENCE = 0.65f;
    private static final int MICRO_SWIPE_MAX = 40;
    private static final float DIAGONAL_REJECTION_RATIO = 2.5f;
    private static final int RAPID_SUCCESSIVE_GESTURE_MS = 800;

    private float lastTapX, lastTapY;
    private long lastTapTime;
    private boolean waitingDoubleTap;
    private long lastGestureEndTime;
    private int rapidGestureCount;
    private long firstRapidGestureTime;

    private AppContextDetector contextDetector;
    private GesturePredictor predictor;
    private GestureZoneManager zoneManager;
    private float contextSensitivityMultiplier = 1.0f;
    private int contextMinSwipeMultiplier = 1;
    private boolean antiAccidentalEnabled = true;
    private float userSensitivity = 1.0f;

    public GestureEngine(int screenWidth, int screenHeight, float density) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.cornerSizePx = (int) (CORNER_SIZE_DP * density);
        this.sensitivity = 1.0f;
        this.cooldownMs = 200;
        setEdgeWidth(24, density);
    }

    public void setContextDetector(AppContextDetector detector) {
        this.contextDetector = detector;
    }

    public void setPredictor(GesturePredictor predictor) {
        this.predictor = predictor;
    }

    public void setZoneManager(GestureZoneManager manager) {
        this.zoneManager = manager;
    }

    public void setListener(GestureListener listener) {
        this.listener = listener;
    }

    public void setSensitivity(float sensitivity) {
        this.userSensitivity = Math.max(0.3f, Math.min(3f, sensitivity));
    }

    public void setCooldownMs(int cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public void setEdgeWidth(int dp, float density) {
        this.edgeWidthPx = (int) (dp * density);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setAntiAccidentalEnabled(boolean enabled) {
        this.antiAccidentalEnabled = enabled;
    }

    public void updateContextParams(float sensitivityMultiplier, int minSwipeMultiplier) {
        this.contextSensitivityMultiplier = sensitivityMultiplier;
        this.contextMinSwipeMultiplier = minSwipeMultiplier;
    }

    private boolean isInCooldown() {
        return System.currentTimeMillis() - lastGestureTime < cooldownMs;
    }

    private int detectEdge(float x, float y) {
        int edge = 0;
        int effectiveEdgeWidth = (zoneManager != null) ? zoneManager.getCurrentEdgeWidth() : edgeWidthPx;

        if (x <= cornerSizePx && y <= cornerSizePx) edge |= CORNER_TOP_LEFT;
        else if (x >= screenWidth - cornerSizePx && y <= cornerSizePx) edge |= CORNER_TOP_RIGHT;
        else if (x <= cornerSizePx && y >= screenHeight - cornerSizePx) edge |= CORNER_BOTTOM_LEFT;
        else if (x >= screenWidth - cornerSizePx && y >= screenHeight - cornerSizePx) edge |= CORNER_BOTTOM_RIGHT;

        if (x <= effectiveEdgeWidth) edge |= EDGE_LEFT;
        if (x >= screenWidth - effectiveEdgeWidth) edge |= EDGE_RIGHT;
        if (y <= effectiveEdgeWidth) edge |= EDGE_TOP;
        if (y >= screenHeight - effectiveEdgeWidth) edge |= EDGE_BOTTOM;
        return edge;
    }

    private boolean isCorner(int edge) {
        return (edge & (CORNER_TOP_LEFT | CORNER_TOP_RIGHT | CORNER_BOTTOM_LEFT | CORNER_BOTTOM_RIGHT)) != 0;
    }

    public void onTouchEvent(MotionEvent event) {
        if (contextDetector != null) {
            contextDetector.onTouchEvent(event);
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
                if (event.getPointerCount() >= 2) {
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);
                    if (listener != null && !isInCooldown() && !isAccidentalMultiTouch(event)) {
                        float dy = y2 - y1;
                        if (Math.abs(dy) > BASE_SWIPE_THRESHOLD) {
                            listener.onGestureDetected(
                                dy < 0 ? GESTURE_SWIPE_UP : GESTURE_SWIPE_DOWN,
                                0, (x1+x2)/2, (y1+y2)/2, Math.abs(dy));
                            lastGestureTime = System.currentTimeMillis();
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float y = event.getY();
                float dx = x - downX;
                float dy = y - downY;
                float dist = (float) Math.sqrt(dx*dx + dy*dy);

                if (!isDragging && dist > 20) {
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
                float dist = (float) Math.sqrt(dx*dx + dy*dy);
                long elapsed = event.getEventTime() - downTime;

                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velX = velocityTracker.getXVelocity(pointerId);
                    float velY = velocityTracker.getYVelocity(pointerId);
                    float vel = (float) Math.sqrt(velX*velX + velY*velY);

                    if (isDragging && dist > getEffectiveSwipeThreshold()) {
                        float angle = (float) Math.atan2(dy, dx);
                        boolean isIntentional = !antiAccidentalEnabled ||
                            isIntentionalGesture(dist, vel, angle, dx, dy);

                        if (isIntentional && !isInCooldown() && !isRapidFireBlocked()) {
                            int gestureType;
                            if (Math.abs(dy) > Math.abs(dx)) {
                                gestureType = dy < 0 ? GESTURE_SWIPE_UP : GESTURE_SWIPE_DOWN;
                            } else {
                                gestureType = dx < 0 ? GESTURE_SWIPE_UP : GESTURE_SWIPE_DOWN;
                            }
                            if (listener != null) {
                                listener.onGestureDetected(gestureType, activeEdge, x, y, vel);
                                if (predictor != null) {
                                    predictor.recordSuccessfulGesture(dist, vel);
                                }
                            }
                            lastGestureTime = System.currentTimeMillis();
                            trackRapidFire();
                        } else if (!isIntentional && predictor != null) {
                            predictor.recordAccidentalTrigger();
                        }
                    } else if (isDragging && (activeEdge & (EDGE_LEFT | EDGE_RIGHT)) != 0) {
                        if (listener != null) {
                            listener.onDragEnd(activeEdge, dy, velocityTracker.getYVelocity(pointerId));
                        }
                    }

                    if (!isDragging && dist < 20 && elapsed < LONG_PRESS_TIMEOUT) {
                        handleTap(x, y);
                    }
                }

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isDragging = false;
                isLongPress = false;
                lastGestureEndTime = System.currentTimeMillis();
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

    private boolean isIntentionalGesture(float distance, float velocity, float angle,
                                          float dx, float dy) {
        if (distance < MICRO_SWIPE_MAX) return false;

        float verticalComponent = Math.abs((float) Math.sin(angle));
        float horizontalComponent = Math.abs((float) Math.cos(angle));

        if (verticalComponent < MIN_DIRECTION_CONFIDENCE) return false;

        if (distance > 0 && Math.abs(dy) > 0 &&
            Math.abs(dx) / Math.abs(dy) > DIAGONAL_REJECTION_RATIO) {
            return false;
        }

        float velocityThreshold = BASE_VELOCITY_THRESHOLD * (2f - userSensitivity)
            * contextSensitivityMultiplier;

        if (velocity < velocityThreshold) return false;

        if (predictor != null && predictor.getTotalAttempts() > 3) {
            float predictedAngle = (float) Math.atan2(dy, dx);
            return predictor.shouldAcceptGesture(distance, velocity, predictedAngle);
        }

        return true;
    }

    private boolean isAccidentalMultiTouch(MotionEvent event) {
        if (event.getPointerCount() < 2) return false;
        float x1 = event.getX(0);
        float y1 = event.getY(0);
        float x2 = event.getX(1);
        float y2 = event.getY(1);
        float pinDist = (float) Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
        return pinDist < 30;
    }

    private boolean isRapidFireBlocked() {
        long now = System.currentTimeMillis();
        if (now - firstRapidGestureTime > RAPID_SUCCESSIVE_GESTURE_MS) {
            rapidGestureCount = 0;
            firstRapidGestureTime = now;
            return false;
        }
        rapidGestureCount++;
        if (rapidGestureCount > 3) return true;
        return false;
    }

    private void trackRapidFire() {
        long now = System.currentTimeMillis();
        if (rapidGestureCount == 0) {
            firstRapidGestureTime = now;
        }
    }

    private int getEffectiveSwipeThreshold() {
        int baseThreshold = (int) (BASE_SWIPE_THRESHOLD * (2f - userSensitivity));
        return baseThreshold * contextMinSwipeMultiplier;
    }

    private void handleTap(float x, float y) {
        long now = System.currentTimeMillis();
        if (waitingDoubleTap) {
            float dx = x - lastTapX;
            float dy = y - lastTapY;
            if (now - lastTapTime < DOUBLE_TAP_TIMEOUT && Math.sqrt(dx*dx + dy*dy) < 100) {
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
        if (contextDetector != null) contextDetector.reset();
    }
}
