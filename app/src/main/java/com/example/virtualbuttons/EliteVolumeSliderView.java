package com.example.virtualbuttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class EliteVolumeSliderView extends View {
    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.3f);

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint glowPaint;
    private Paint thumbPaint;
    private Paint iconPaint;

    private RectF trackRect;
    private RectF progressRect;

    private float density;
    private int cornerRadius;
    private int trackWidth;
    private int thumbRadius;
    private int padding;

    private float currentProgress = 0.5f;
    private float targetProgress = 0.5f;
    private float touchStartY;
    private float touchStartProgress;
    private int maxVolume;
    private int currentVolume;

    private VolumeChangeListener volumeListener;
    private boolean isActive = false;
    private boolean isTracking = false;

    private int primaryColor;
    private int secondaryColor;
    private int glowColor;
    private int trackColor;

    private Handler handler;
    private Runnable hapticRunnable;

    public interface VolumeChangeListener {
        void onVolumeChanged(int volume, int max);
        void onTrackingStarted();
        void onTrackingEnded();
    }

    public EliteVolumeSliderView(Context context) {
        super(context);
        init(context);
    }

    public EliteVolumeSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EliteVolumeSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        handler = new Handler(Looper.getMainLooper());

        cornerRadius = Math.round(28 * density);
        trackWidth = Math.round(8 * density);
        thumbRadius = Math.round(14 * density);
        padding = Math.round(24 * density);

        primaryColor = 0xFF6366F1;
        secondaryColor = 0xFF8B5CF6;
        glowColor = 0x406366F1;
        trackColor = 0x40242430;

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAlpha(60);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(0xFFFFFFFF);
        iconPaint.setTextSize(Math.round(16 * density));
        iconPaint.setTextAlign(Paint.Align.CENTER);

        trackRect = new RectF();
        progressRect = new RectF();

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setVolumeListener(VolumeChangeListener listener) {
        this.volumeListener = listener;
    }

    public void setVolumeRange(int max, int current) {
        this.maxVolume = max;
        this.currentVolume = current;
        this.currentProgress = max > 0 ? (float) current / max : 0.5f;
        this.targetProgress = currentProgress;
        invalidate();
    }

    public void setCurrentVolume(int volume) {
        this.currentVolume = volume;
        this.currentProgress = maxVolume > 0 ? (float) volume / maxVolume : 0.5f;
        invalidate();
    }

    private void updateProgressFromY(float y) {
        int sliderHeight = getHeight() - (padding * 2);
        if (sliderHeight <= 0) return;

        float relativeY = y - padding;
        float progress = 1f - (relativeY / sliderHeight);
        progress = Math.max(0f, Math.min(1f, progress));

        targetProgress = progress;
        int newVolume = Math.round(progress * maxVolume);

        if (newVolume != currentVolume) {
            currentVolume = newVolume;
            if (volumeListener != null) {
                volumeListener.onVolumeChanged(currentVolume, maxVolume);
            }
            triggerHaptic();
        }

        invalidate();
    }

    private void triggerHaptic() {
        handler.removeCallbacks(hapticRunnable);
        hapticRunnable = () -> {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(5);
                }
            }
        };
        handler.postDelayed(hapticRunnable, 30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
    }

    private void updateGradient() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        int gradientHeight = getHeight() - (padding * 2);
        LinearGradient gradient = new LinearGradient(
            0, getHeight() - padding,
            0, padding,
            new int[]{secondaryColor, primaryColor},
            null,
            Shader.TileMode.CLAMP
        );
        progressPaint.setShader(gradient);

        int glowGradientHeight = gradientHeight + thumbRadius;
        LinearGradient glowGradient = new LinearGradient(
            0, getHeight() - padding,
            0, padding,
            new int[]{glowColor, android.graphics.Color.TRANSPARENT},
            null,
            Shader.TileMode.CLAMP
        );
        glowPaint.setShader(glowGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int sliderTop = padding;
        int sliderBottom = getHeight() - padding;
        int sliderHeight = sliderBottom - sliderTop;

        float progressY = sliderBottom - (currentProgress * sliderHeight);

        trackRect.set(
            centerX - trackWidth,
            sliderTop,
            centerX + trackWidth,
            sliderBottom
        );
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

        progressRect.set(
            centerX - trackWidth,
            progressY,
            centerX + trackWidth,
            sliderBottom
        );
        canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);

        float glowRadius = thumbRadius * 2.5f;
        RectF glowRect = new RectF(
            centerX - glowRadius,
            progressY - glowRadius,
            centerX + glowRadius,
            progressY + glowRadius
        );
        canvas.drawRoundRect(glowRect, glowRadius, glowRadius, glowPaint);

        thumbPaint.setColor(0xFFFFFFFF);
        thumbPaint.setShadowLayer(thumbRadius * 0.8f, 0, 0, 0x40000000);
        canvas.drawCircle(centerX, progressY, thumbRadius, thumbPaint);

        thumbPaint.setColor(primaryColor);
        canvas.drawCircle(centerX, progressY, thumbRadius * 0.45f, thumbPaint);

        String icon = currentVolume == 0 ? "\uD83D\uDD07" : currentVolume < maxVolume * 0.5f ? "\uD83D\uDD09" : "\uD83D\uDD0A";
        canvas.drawText(icon, centerX, progressY + (iconPaint.getTextSize() * 0.35f), iconPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = event.getY();
                touchStartProgress = targetProgress;
                isTracking = true;
                isActive = true;
                if (volumeListener != null) {
                    volumeListener.onTrackingStarted();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isTracking) {
                    updateProgressFromY(event.getY());
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isTracking) {
                    isTracking = false;
                    isActive = false;
                    if (volumeListener != null) {
                        volumeListener.onTrackingEnded();
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void animateIn() {
        setAlpha(0f);
        setScaleX(0.6f);
        setScaleY(0.6f);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(this, "scaleX", 0.6f, 1.08f, 1f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(this, "scaleY", 0.6f, 1.08f, 1f);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
        set.setDuration(280);
        set.setInterpolator(OVERSHOOT);
        set.start();
    }

    public void animateOut(Runnable onComplete) {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.5f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.5f);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
        set.setDuration(180);
        set.setInterpolator(DECEL);
        if (onComplete != null) {
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onComplete.run();
                }
            });
        }
        set.start();
    }

    public boolean isActive() {
        return isActive;
    }
}