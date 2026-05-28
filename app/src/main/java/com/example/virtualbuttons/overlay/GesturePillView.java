package com.example.virtualbuttons.overlay;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.example.virtualbuttons.HapticFeedbackUtil;
import com.example.virtualbuttons.gesture.GestureEngine;

public class GesturePillView extends View {
    private static final int PILL_WIDTH_DP = 8;
    private static final int PILL_HEIGHT_DP_LARGE = 72;
    private static final int PILL_HEIGHT_DP_SMALL = 48;

    private final Paint pillPaint;
    private final Paint glowPaint;
    private final Paint borderPaint;
    private final Path pillPath;
    private final RectF pillRect;

    private int pillColor;
    private int pillOpacity;
    private int pillSize;
    private int pillHeight;
    private float density;
    private boolean isDragging;
    private boolean isSnapping;
    private float snapTargetX;
    private float snapTargetY;

    private GestureEngine gestureEngine;
    private HapticFeedbackUtil haptics;
    private boolean attachedToLeft;
    private OnTouchListener pillTouchListener;

    public interface OnTouchListener {
        void onTouch(MotionEvent event);
    }

    public void setPillTouchListener(OnTouchListener listener) {
        this.pillTouchListener = listener;
    }

    private WindowManager.LayoutParams params;
    private WindowManager wm;

    private float touchOffsetX, touchOffsetY;

    public GesturePillView(Context context, int color, int opacity, int size, float density) {
        super(context);
        this.density = density;
        this.pillColor = color;
        this.pillOpacity = opacity;
        this.pillSize = size;
        this.pillHeight = (int) (PILL_HEIGHT_DP_LARGE * density);

        pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillPaint.setStyle(Paint.Style.FILL);
        pillPaint.setColor(pillColor);
        pillPaint.setAlpha((int) (pillOpacity * 2.55f));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(pillColor);
        glowPaint.setAlpha(30);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAlpha(40);
        borderPaint.setStrokeWidth(density);

        pillPath = new Path();
        pillRect = new RectF();
    }

    public void setHaptics(HapticFeedbackUtil haptics) {
        this.haptics = haptics;
    }

    public void setGestureEngine(GestureEngine engine) {
        this.gestureEngine = engine;
    }

    public void setWindowParams(WindowManager.LayoutParams params) {
        this.params = params;
    }

    public void setWindowManager(WindowManager wm) {
        this.wm = wm;
    }

    public void setAttachedToLeft(boolean left) {
        this.attachedToLeft = left;
        invalidate();
    }

    public boolean isAttachedToLeft() {
        return attachedToLeft;
    }

    public void updateAppearance(int color, int opacity, int size) {
        this.pillColor = color;
        this.pillOpacity = opacity;
        this.pillSize = size;
        pillPaint.setColor(color);
        pillPaint.setAlpha((int) (opacity * 2.55f));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pillW = PILL_WIDTH_DP * density;
        float left = (w - pillW) / 2f;
        float top = (h - pillHeight) / 2f;
        pillRect.set(left, top, left + pillW, top + pillHeight);
        buildPillPath();
    }

    private void buildPillPath() {
        pillPath.reset();
        float r = PILL_WIDTH_DP * density / 2f;
        pillPath.addRoundRect(pillRect, r, r, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float glowRadius = pillRect.width() * 3f;
        canvas.drawCircle(cx, cy, glowRadius, glowPaint);

        float r = PILL_WIDTH_DP * density / 2f;
        canvas.drawRoundRect(pillRect, r, r, pillPaint);
        canvas.drawRoundRect(pillRect, r, r, borderPaint);

        if (isSnapping) {
            float indicatorY = pillRect.centerY();
            float indicatorX = attachedToLeft ? pillRect.right + 8 * density : pillRect.left - 8 * density;
            Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            indicatorPaint.setColor(Color.WHITE);
            indicatorPaint.setAlpha(80);
            float arrowSize = 6 * density;
            Path arrow = new Path();
            if (attachedToLeft) {
                arrow.moveTo(indicatorX, indicatorY - arrowSize);
                arrow.lineTo(indicatorX + arrowSize * 1.5f, indicatorY);
                arrow.lineTo(indicatorX, indicatorY + arrowSize);
            } else {
                arrow.moveTo(indicatorX, indicatorY - arrowSize);
                arrow.lineTo(indicatorX - arrowSize * 1.5f, indicatorY);
                arrow.lineTo(indicatorX, indicatorY + arrowSize);
            }
            canvas.drawPath(arrow, indicatorPaint);
        }
    }

    public void snapToEdge(int screenWidth, int screenHeight, boolean animated) {
        isSnapping = true;
        float targetX;
        float targetY = params.y;

        if (params.x + getWidth() / 2f < screenWidth / 2f) {
            targetX = 0;
            attachedToLeft = true;
        } else {
            targetX = screenWidth - getWidth();
            attachedToLeft = false;
        }

        snapTargetX = targetX;
        snapTargetY = targetY;

        if (animated) {
            ValueAnimator snapX = ValueAnimator.ofInt(params.x, (int) targetX);
            snapX.setDuration(300);
            snapX.setInterpolator(new OvershootInterpolator(0.8f));
            snapX.addUpdateListener(a -> {
                params.x = (int) (float) a.getAnimatedValue();
                if (wm != null) wm.updateViewLayout(this, params);
                invalidate();
            });
            snapX.start();
        } else {
            params.x = (int) targetX;
            if (wm != null) wm.updateViewLayout(this, params);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureEngine != null) {
            gestureEngine.onTouchEvent(event);
        }
        if (pillTouchListener != null) {
            pillTouchListener.onTouch(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                touchOffsetX = event.getRawX() - params.x;
                touchOffsetY = event.getRawY() - params.y;
                if (haptics != null) haptics.lightTap();
                isDragging = false;
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float newX = event.getRawX() - touchOffsetX;
                float newY = event.getRawY() - touchOffsetY;
                float dx = Math.abs(event.getRawX() - (params.x + touchOffsetX));
                float dy = Math.abs(event.getRawY() - (params.y + touchOffsetY));

                if (!isDragging && (dx > 10 || dy > 10)) {
                    isDragging = true;
                }

                if (isDragging) {
                    params.x = (int) newX;
                    params.y = (int) newY;
                    if (wm != null) wm.updateViewLayout(this, params);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (isDragging && wm != null) {
                    int[] location = new int[2];
                    getLocationOnScreen(location);
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;

                    float centerX = params.x + getWidth() / 2f;
                    int targetX = centerX < screenWidth / 2f ? 0 : screenWidth - getWidth();
                    attachedToLeft = targetX == 0;

                    ValueAnimator snapAnim = ValueAnimator.ofInt(params.x, targetX);
                    snapAnim.setDuration(250);
                    snapAnim.setInterpolator(new OvershootInterpolator(0.6f));
                    snapAnim.addUpdateListener(a -> {
                        params.x = (int) (float) a.getAnimatedValue();
                        wm.updateViewLayout(this, params);
                        invalidate();
                    });
                    snapAnim.start();
                }
                isDragging = false;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public int getPillWidth() {
        return (int) (PILL_WIDTH_DP * density * 3);
    }

    public int getPillHeight() {
        return pillHeight + (int) (16 * density);
    }
}
