package com.example.virtualbuttons.overlay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class RadialMenuView extends View {
    public interface RadialMenuListener {
        void onItemSelected(int index);
        void onMenuDismissed();
    }

    private static final int ITEM_COUNT = 6;
    private static final float RADIUS_DP = 80;
    private static final float ITEM_RADIUS_DP = 22;

    private final Paint bgPaint;
    private final Paint[] itemPaints;
    private final Paint[] itemBgPaints;
    private final Paint centerPaint;
    private final Paint textPaint;
    private final Paint iconPaint;
    private final Paint selectedPaint;

    private final String[] labels = {"Volume", "Bright", "Flash", "Mute", "Screenshot", "Lock"};
    private final String[] icons = {"🔊", "☀", "🔦", "🔇", "📷", "🔒"};
    private final int[] iconColors = {
        0xFF6750A4, 0xFFFFC107, 0xFFFF9800, 0xFFF44336, 0xFF4CAF50, 0xFF2196F3
    };

    private float density;
    private float radius;
    private float itemRadius;
    private float centerX, centerY;
    private float touchAngle;
    private float touchDist;
    private int selectedIndex = -1;
    private boolean isShowing;
    private float animationProgress;
    private ValueAnimator showAnimator;
    private ValueAnimator dismissAnimator;
    private RadialMenuListener listener;
    private float[] itemAngles;

    public RadialMenuView(Context context, float density) {
        super(context);
        this.density = density;
        this.radius = RADIUS_DP * density;
        this.itemRadius = ITEM_RADIUS_DP * density;

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(Color.rgb(30, 30, 45));
        centerPaint.setAlpha(220);
        centerPaint.setStyle(Paint.Style.FILL);

        itemPaints = new Paint[ITEM_COUNT];
        itemBgPaints = new Paint[ITEM_COUNT];
        for (int i = 0; i < ITEM_COUNT; i++) {
            itemPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            itemPaints[i].setColor(iconColors[i]);
            itemPaints[i].setAlpha(220);
            itemPaints[i].setStyle(Paint.Style.FILL);

            itemBgPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            itemBgPaints[i].setColor(Color.WHITE);
            itemBgPaints[i].setAlpha(20);
            itemBgPaints[i].setStyle(Paint.Style.STROKE);
            itemBgPaints[i].setStrokeWidth(2 * density);
        }

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(9 * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setTextSize(18 * density);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setAlpha(60);
        selectedPaint.setStyle(Paint.Style.FILL);

        itemAngles = new float[ITEM_COUNT];
        for (int i = 0; i < ITEM_COUNT; i++) {
            itemAngles[i] = (float) Math.toRadians(90 - 60 * i);
        }

        setAlpha(0f);
        setVisibility(GONE);
    }

    public void setListener(RadialMenuListener listener) {
        this.listener = listener;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void show(float x, float y) {
        centerX = x;
        centerY = y;
        selectedIndex = -1;

        if (dismissAnimator != null) dismissAnimator.cancel();

        setVisibility(VISIBLE);
        isShowing = true;

        if (showAnimator != null) showAnimator.cancel();
        showAnimator = ValueAnimator.ofFloat(0f, 1f);
        showAnimator.setDuration(300);
        showAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        showAnimator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            setAlpha(animationProgress);
            setScaleX(0.5f + 0.5f * animationProgress);
            setScaleY(0.5f + 0.5f * animationProgress);
            invalidate();
        });
        showAnimator.start();
    }

    public void dismiss() {
        if (!isShowing) return;
        if (showAnimator != null) showAnimator.cancel();
        if (dismissAnimator != null) dismissAnimator.cancel();
        dismissAnimator = ValueAnimator.ofFloat(1f, 0f);
        dismissAnimator.setDuration(200);
        dismissAnimator.setInterpolator(new DecelerateInterpolator());
        dismissAnimator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            setAlpha(animationProgress);
            setScaleX(animationProgress);
            setScaleY(animationProgress);
            invalidate();
        });
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isShowing = false;
                setVisibility(GONE);
                if (listener != null) listener.onMenuDismissed();
            }
        });
        dismissAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isShowing) return false;

        float x = event.getX();
        float y = event.getY();
        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                selectedIndex = findNearestItem(angle, dist);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int idx = findNearestItem(angle, dist);
                if (idx >= 0 && dist < radius + itemRadius) {
                    if (listener != null) listener.onItemSelected(idx);
                }
                dismiss();
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                dismiss();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private int findNearestItem(float angle, float dist) {
        if (dist < itemRadius * 1.5f) return -1;
        if (dist > radius + itemRadius) return -1;

        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < ITEM_COUNT; i++) {
            float diff = (float) Math.abs(Math.atan2(
                Math.sin(angle - itemAngles[i]),
                Math.cos(angle - itemAngles[i])));
            if (diff < bestDist) {
                bestDist = diff;
                best = i;
            }
        }
        if (bestDist < Math.toRadians(40)) return best;
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float animRadius = radius * animationProgress;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            bgPaint.setShader(new RadialGradient(cx, cy, animRadius + itemRadius,
                Color.argb(80, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        } else {
            bgPaint.setColor(Color.argb(80, 0, 0, 0));
        }
        canvas.drawCircle(cx, cy, animRadius + itemRadius, bgPaint);

        canvas.drawCircle(cx, cy, 16 * density * animationProgress, centerPaint);
        iconPaint.setTextSize(16 * density);
        canvas.drawText("✕", cx, cy + 6 * density, iconPaint);

        for (int i = 0; i < ITEM_COUNT; i++) {
            float angle = itemAngles[i];
            float ix = cx + (float) Math.cos(angle) * animRadius;
            float iy = cy + (float) Math.sin(angle) * animRadius;
            float ir = itemRadius * (0.3f + 0.7f * animationProgress);

            if (selectedIndex == i) {
                canvas.drawCircle(ix, iy, ir + 4 * density, selectedPaint);
            }
            canvas.drawCircle(ix, iy, ir, itemPaints[i]);
            canvas.drawCircle(ix, iy, ir, itemBgPaints[i]);

            iconPaint.setColor(Color.WHITE);
            iconPaint.setTextSize(16 * density);
            canvas.drawText(icons[i], ix, iy + 6 * density, iconPaint);

            textPaint.setAlpha((int) (255 * animationProgress));
            canvas.drawText(labels[i], ix, iy + ir + 14 * density, textPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) ((RADIUS_DP + ITEM_RADIUS_DP + 20) * 2 * density);
        setMeasuredDimension(size, size);
    }
}
