package com.example.virtualbuttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class EdgeVolumePopup {
    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.2f);

    private final Context context;
    private final WindowManager windowManager;
    private final AudioManager audioManager;
    private final SettingsStore settings;

    private FrameLayout popupContainer;
    private EliteVolumeSliderView sliderView;
    private WindowManager.LayoutParams popupParams;

    private int streamType = AudioManager.STREAM_MUSIC;
    private int maxVolume;
    private int currentVolume;

    private boolean isVisible = false;
    private float touchOriginY = 0;
    private float progressOrigin = 0.5f;

    private Handler handler;
    private Runnable autoHideRunnable;
    private int hue;

    public interface VolumeChangeCallback {
        void onVolumeChanged(int volume, int max);
    }

    private VolumeChangeCallback volumeCallback;

    public EdgeVolumePopup(Context context, WindowManager windowManager, SettingsStore settings) {
        this.context = context;
        this.windowManager = windowManager;
        this.settings = settings;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.hue = settings.bubbleColorHue();

        autoHideRunnable = () -> hide(false);

        initPopup();
    }

    private void initPopup() {
        if (windowManager == null) return;

        popupContainer = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return handleTouch(event);
            }
        };

        sliderView = new EliteVolumeSliderView(context);
        sliderView.setLayoutParams(new FrameLayout.LayoutParams(
            dp(64),
            dp(280)
        ));

        popupContainer.addView(sliderView);

        sliderView.setVolumeListener(new EliteVolumeSliderView.VolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume, int max) {
                setSystemVolume(volume, max);
                resetAutoHide();
            }

            @Override
            public void onTrackingStarted() {
                resetAutoHide();
            }

            @Override
            public void onTrackingEnded() {
                scheduleAutoHide();
            }
        });

        updatePopupAppearance();

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        popupParams = new WindowManager.LayoutParams(
                dp(80),
                dp(320),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        popupParams.alpha = 0f;
    }

    private void updatePopupAppearance() {
        if (popupContainer == null) return;

        float[] hsv = new float[]{hue, 0.55f, 0.85f};
        int primaryColor = Color.HSVToColor(hsv);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(32));
        background.setColor(Color.argb(248, 28, 26, 33));
        background.setStroke(dp(1), Color.argb(80, 255, 255, 255));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.graphics.Outline outline = new android.graphics.Outline();
            outline.setRect(0, 0, dp(80), dp(320));
            background.setOutline(outline);
        }

        popupContainer.setBackground(background);

        sliderView.setVolumeRange(maxVolume, currentVolume);
    }

    public void setVolumeCallback(VolumeChangeCallback callback) {
        this.volumeCallback = callback;
    }

    private boolean handleTouch(MotionEvent event) {
        if (!isVisible) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchOriginY = event.getRawY();
                progressOrigin = getCurrentProgress();
                resetAutoHide();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaY = touchOriginY - event.getRawY();
                float sliderHeight = dp(280) - dp(48);
                float progressDelta = deltaY / sliderHeight;
                float newProgress = Math.max(0f, Math.min(1f, progressOrigin + progressDelta));

                int newVolume = Math.round(newProgress * maxVolume);
                setSystemVolume(newVolume, maxVolume);
                sliderView.setCurrentVolume(newVolume);

                resetAutoHide();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                scheduleAutoHide();
                return true;
        }
        return false;
    }

    private float getCurrentProgress() {
        return maxVolume > 0 ? (float) currentVolume / maxVolume : 0.5f;
    }

    private void setSystemVolume(int volume, int max) {
        if (audioManager == null) return;

        volume = Math.max(0, Math.min(max, volume));
        audioManager.setStreamVolume(streamType, volume, 0);
        currentVolume = volume;

        if (volumeCallback != null) {
            volumeCallback.onVolumeChanged(volume, max);
        }
    }

    public void show(float x, float y, int edge) {
        if (isVisible) {
            updatePosition(x, y, edge);
            return;
        }

        streamType = resolveStreamType();
        maxVolume = audioManager.getStreamMaxVolume(streamType);
        currentVolume = audioManager.getStreamVolume(streamType);

        sliderView.setVolumeRange(maxVolume, currentVolume);

        updatePosition(x, y, edge);

        try {
            windowManager.addView(popupContainer, popupParams);
            isVisible = true;
            animateIn();
            scheduleAutoHide();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int resolveStreamType() {
        SettingsStore.StreamMode mode = settings.streamMode();
        if (mode == SettingsStore.StreamMode.MEDIA) return AudioManager.STREAM_MUSIC;
        if (mode == SettingsStore.StreamMode.SYSTEM) return AudioManager.STREAM_SYSTEM;
        return audioManager.isMusicActive() ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_SYSTEM;
    }

    private void updatePosition(float x, float y, int edge) {
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();

        int popupWidth = dp(80);
        int popupHeight = dp(320);

        float centerX;
        if (edge < 0) {
            centerX = dp(56);
        } else {
            centerX = screenWidth - dp(56) - popupWidth;
        }

        float centerY = y - (popupHeight / 2f);
        centerY = Math.max(dp(48), Math.min(screenHeight - popupHeight - dp(48), centerY));

        popupParams.x = Math.round(centerX);
        popupParams.y = Math.round(centerY);

        if (windowManager != null && popupContainer != null && popupContainer.getParent() != null) {
            windowManager.updateViewLayout(popupContainer, popupParams);
        }
    }

    public void hide(boolean immediately) {
        if (!isVisible) return;

        handler.removeCallbacks(autoHideRunnable);

        if (immediately) {
            destroyPopup();
            return;
        }

        animateOut(() -> destroyPopup());
    }

    private void destroyPopup() {
        try {
            if (popupContainer != null && popupContainer.getParent() != null) {
                windowManager.removeView(popupContainer);
            }
        } catch (Exception ignored) {}
        isVisible = false;
    }

    private void animateIn() {
        popupContainer.setAlpha(0f);
        popupContainer.setScaleX(0.5f);
        popupContainer.setScaleY(0.5f);
        popupContainer.setTranslationY(dp(20));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(popupContainer, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(popupContainer, "scaleX", 0.5f, 1.08f, 1f),
            ObjectAnimator.ofFloat(popupContainer, "scaleY", 0.5f, 1.08f, 1f),
            ObjectAnimator.ofFloat(popupContainer, "translationY", dp(20), 0f)
        );
        set.setDuration(300);
        set.setInterpolator(OVERSHOOT);
        set.start();
    }

    private void animateOut(Runnable onComplete) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(popupContainer, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(popupContainer, "scaleX", 1f, 0.6f),
            ObjectAnimator.ofFloat(popupContainer, "scaleY", 1f, 0.6f)
        );
        set.setDuration(180);
        set.setInterpolator(DECEL);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onComplete != null) onComplete.run();
            }
        });
        set.start();
    }

    private void resetAutoHide() {
        handler.removeCallbacks(autoHideRunnable);
    }

    private void scheduleAutoHide() {
        handler.removeCallbacks(autoHideRunnable);
        handler.postDelayed(autoHideRunnable, 3000);
    }

    public boolean isShowing() {
        return isVisible;
    }

    public void refreshTheme() {
        this.hue = settings.bubbleColorHue();
        updatePopupAppearance();
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}