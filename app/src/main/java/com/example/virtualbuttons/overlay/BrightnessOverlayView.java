package com.example.virtualbuttons.overlay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

public class BrightnessOverlayView extends android.view.View {
    private static final int WIDTH_DP = 56;
    private static final int HEIGHT_DP = 220;
    private static final int TRACK_WIDTH_DP = 4;
    private static final int ANIM_DURATION = 200;
    private static final int DISMISS_DELAY = 1200;

    private final Paint backgroundPaint;
    private final Paint trackPaint;
    private final Paint fillPaint;
    private final Paint thumbPaint;
    private final Paint textPaint;
    private final Paint sunPaint;
    private final RectF trackRect;
    private final RectF fillRect;

    private float brightness = 0.5f;
    private float animatedBrightness;
    private ValueAnimator brightnessAnimator;
    private ValueAnimator fadeAnimator;
    private final Handler dismissHandler;
    private final Runnable dismissRunnable;
    private WindowManager.LayoutParams params;
    private WindowManager wm;
    private boolean isShowing;
    private final float density;

    public BrightnessOverlayView(Context context, float density) {
        super(context);
        this.density = density;

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.rgb(20, 20, 30));
        backgroundPaint.setAlpha(200);

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(Color.rgb(60, 60, 80));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(TRACK_WIDTH_DP * density);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.rgb(255, 193, 7));
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(TRACK_WIDTH_DP * density);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(Color.WHITE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14 * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(Color.rgb(255, 193, 7));
        sunPaint.setAlpha(200);
        sunPaint.setTextSize(18 * density);
        sunPaint.setTextAlign(Paint.Align.CENTER);

        trackRect = new RectF();
        fillRect = new RectF();

        dismissHandler = new Handler(Looper.getMainLooper());
        dismissRunnable = this::dismiss;

        setAlpha(0f);
    }

    public void setWindowManager(WindowManager wm) {
        this.wm = wm;
    }

    public void setWindowParams(WindowManager.LayoutParams params) {
        this.params = params;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void show(float brightness, int x, int y, int edge) {
        this.brightness = brightness;

        if (params != null) {
            params.x = x;
            params.y = y - (int)(HEIGHT_DP * density / 2);
            if (edge == 1) {
                params.x = x + (int)(12 * density);
            } else {
                params.x = x - (int)(WIDTH_DP * density) - (int)(12 * density);
            }
        }

        if (!isShowing) {
            isShowing = true;
            setAlpha(0f);
            setTranslationY(20 * density);

            if (fadeAnimator != null) fadeAnimator.cancel();
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f);
            fadeAnimator.setDuration(ANIM_DURATION);
            fadeAnimator.setInterpolator(new DecelerateInterpolator());
            fadeAnimator.addUpdateListener(a -> {
                float v = (float) a.getAnimatedValue();
                setAlpha(v);
                setTranslationY(20 * density * (1 - v));
            });
            fadeAnimator.start();

            animateBrightness(0f, brightness);
        } else {
            animateBrightness(animatedBrightness, brightness);
        }

        if (wm != null && params != null) {
            try {
                wm.updateViewLayout(this, params);
            } catch (Exception ignored) {}
        }

        dismissHandler.removeCallbacks(dismissRunnable);
        dismissHandler.postDelayed(dismissRunnable, DISMISS_DELAY);
    }

    public void updateBrightness(float brightness) {
        this.brightness = brightness;
        animateBrightness(animatedBrightness, brightness);

        dismissHandler.removeCallbacks(dismissRunnable);
        dismissHandler.postDelayed(dismissRunnable, DISMISS_DELAY);
    }

    private void animateBrightness(float from, float to) {
        if (brightnessAnimator != null) brightnessAnimator.cancel();
        brightnessAnimator = ValueAnimator.ofFloat(from, to);
        brightnessAnimator.setDuration(150);
        brightnessAnimator.setInterpolator(new DecelerateInterpolator());
        brightnessAnimator.addUpdateListener(a -> {
            animatedBrightness = (float) a.getAnimatedValue();
            invalidate();
        });
        brightnessAnimator.start();
    }

    private void dismiss() {
        if (!isShowing) return;
        if (fadeAnimator != null) fadeAnimator.cancel();
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f);
        fadeAnimator.setDuration(150);
        fadeAnimator.setInterpolator(new DecelerateInterpolator());
        fadeAnimator.addUpdateListener(a -> setAlpha((float) a.getAnimatedValue()));
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isShowing = false;
                if (wm != null && params != null && getParent() != null) {
                    try {
                        wm.removeView(BrightnessOverlayView.this);
                    } catch (Exception ignored) {}
                }
            }
        });
        fadeAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float corner = 16 * density;
        float trackLeft = w / 2f - TRACK_WIDTH_DP * density / 2f;
        float trackRight = trackLeft + TRACK_WIDTH_DP * density;
        float trackTop = 48 * density;
        float trackBottom = h - 48 * density;
        float thumbRadius = 8 * density;

        canvas.drawRoundRect(0, 0, w, h, corner, corner, backgroundPaint);

        trackRect.set(trackLeft, trackTop, trackRight, trackBottom);
        canvas.drawRoundRect(trackRect, TRACK_WIDTH_DP * density / 2f,
            TRACK_WIDTH_DP * density / 2f, trackPaint);

        if (animatedBrightness > 0.01f) {
            float fillBottom = trackBottom;
            float fillTop = trackBottom - (trackBottom - trackTop) * animatedBrightness;
            fillRect.set(trackLeft, fillTop, trackRight, fillBottom);
            canvas.drawRoundRect(fillRect, TRACK_WIDTH_DP * density / 2f,
                TRACK_WIDTH_DP * density / 2f, fillPaint);
        }

        float thumbY = trackBottom - (trackBottom - trackTop) * animatedBrightness;
        canvas.drawCircle(w / 2f, thumbY, thumbRadius, thumbPaint);

        int brightnessPercent = (int) (brightness * 100);
        canvas.drawText(String.valueOf(brightnessPercent) + "%", w / 2f, 32 * density, textPaint);

        String icon;
        if (brightness < 0.2f) icon = "🌙";
        else if (brightness < 0.6f) icon = "☀";
        else icon = "☀";
        sunPaint.setTextSize(18 * density);
        canvas.drawText(icon, w / 2f, trackBottom + 34 * density, sunPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
            (int) (WIDTH_DP * density),
            (int) (HEIGHT_DP * density));
    }
}
