package com.example.virtualbuttons;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Color;
import android.graphics.PixelFormat;

public class EdgeGestureDetector {
    private static final long LONG_PRESS_TIMEOUT = 350;
    private static final int EDGE_DETECTION_WIDTH_DP = 40;

    private final Context context;
    private final WindowManager windowManager;
    private final EdgeGestureCallback callback;

    private View edgeOverlay;
    private WindowManager.LayoutParams overlayParams;

    private Handler handler;
    private Runnable longPressRunnable;
    private boolean isLongPressTriggered = false;
    private boolean isTracking = false;
    private float touchStartX;
    private float touchStartY;
    private int activeEdge = 0;
    private int edgeWidthPx;

    private float density;

    public interface EdgeGestureCallback {
        void onLongPressTriggered(float x, float y, int edge);
        void onDragStarted(float x, float y, int edge);
        void onDrag(float x, float y, float deltaY, int edge);
        void onDragEnded(float x, float y, int edge);
    }

    public EdgeGestureDetector(Context context, WindowManager windowManager, EdgeGestureCallback callback) {
        this.context = context;
        this.windowManager = windowManager;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
        this.density = context.getResources().getDisplayMetrics().density;
        this.edgeWidthPx = Math.round(EDGE_DETECTION_WIDTH_DP * density);

        longPressRunnable = () -> {
            if (!isTracking) return;
            isLongPressTriggered = true;
            triggerHaptic();
            if (callback != null) {
                callback.onLongPressTriggered(touchStartX, touchStartY, activeEdge);
            }
        };

        createOverlayView();
    }

    private void createOverlayView() {
        edgeOverlay = new View(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return handleTouch(event);
            }
        };

        edgeOverlay.setBackgroundColor(Color.TRANSPARENT);
        edgeOverlay.setClickable(false);
        edgeOverlay.setFocusable(false);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        overlayParams = new WindowManager.LayoutParams(
                edgeWidthPx * 2,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        overlayParams.alpha = 1f;
    }

    private boolean handleTouch(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        int screenWidth = windowManager.getDefaultDisplay().getWidth();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                boolean nearLeftEdge = x < edgeWidthPx;
                boolean nearRightEdge = x > screenWidth - edgeWidthPx;

                if (nearLeftEdge || nearRightEdge) {
                    activeEdge = nearLeftEdge ? -1 : 1;
                    touchStartX = x;
                    touchStartY = y;
                    isTracking = true;
                    isLongPressTriggered = false;
                    handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (!isTracking) return false;

                if (!isLongPressTriggered) {
                    float dx = Math.abs(x - touchStartX);
                    float dy = Math.abs(y - touchStartY);
                    if (dx > density * 8 || dy > density * 8) {
                        handler.removeCallbacks(longPressRunnable);
                        isTracking = false;
                        return false;
                    }
                } else {
                    float deltaY = y - touchStartY;
                    if (callback != null) {
                        callback.onDrag(x, y, deltaY, activeEdge);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isTracking) return false;

                handler.removeCallbacks(longPressRunnable);

                if (isLongPressTriggered && callback != null) {
                    callback.onDragEnded(x, y, activeEdge);
                }

                isTracking = false;
                isLongPressTriggered = false;
                return true;
        }
        return false;
    }

    public void attach() {
        try {
            if (edgeOverlay.getParent() == null) {
                windowManager.addView(edgeOverlay, overlayParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void detach() {
        try {
            if (edgeOverlay != null && edgeOverlay.getParent() != null) {
                windowManager.removeView(edgeOverlay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(longPressRunnable);
    }

    private void triggerHaptic() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(25);
            }
        }
    }
}