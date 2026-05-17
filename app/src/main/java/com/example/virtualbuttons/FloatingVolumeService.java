package com.example.virtualbuttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FloatingVolumeService extends Service implements SensorEventListener, TextToSpeech.OnInitListener {
    private SettingsStore settings;
    private VolumeController volumeController;
    private WindowManager windowManager;
    private FrameLayout bubble;
    private TextView indicator;
    private View leftEdge;
    private View rightEdge;
    private View leftTrail;
    private View rightTrail;
    private SensorManager sensorManager;
    private long lastShake;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable singleTapRunnable = () -> adjust(1);
    private final Runnable hideBubbleRunnable = () -> hideBubble(false);
    private boolean bubbleVisible = false;
    private boolean bubblePinned = false;
    private boolean adjustDebounce = false;
    private WindowManager.LayoutParams bubbleLp;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private EdgeVolumePopup edgeVolumePopup;
    private boolean edgeLongPressEnabled = true;

    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final AccelerateDecelerateInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.8f);

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(java.util.Locale.US);
            ttsReady = true;
        }
    }

    void hideBubble(boolean force) {
        if (!force && bubblePinned) return;
        if (bubble != null && bubble.getParent() != null && windowManager != null) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                ObjectAnimator.ofFloat(bubble, "scaleX", 1f, 0.4f),
                ObjectAnimator.ofFloat(bubble, "scaleY", 1f, 0.4f),
                ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f)
            );
            set.setDuration(180);
            set.setInterpolator(DECEL);
            set.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    try { if (windowManager != null) windowManager.removeView(bubble); } catch (Exception ignored) {}
                }
            });
            set.start();
        }
        bubbleVisible = false;
        bubblePinned = false;
        handler.removeCallbacks(hideBubbleRunnable);
    }
    void hideBubble() { hideBubble(false); }

    void showBubble() {
        showBubble(false);
    }

    void showBubblePermanent() {
        bubblePinned = true;
        showBubble(true);
    }

    private void showBubble(boolean preservePinned) {
        if (bubble == null) initBubble();
        if (bubble == null || windowManager == null) return;
        boolean wasPinned = bubblePinned;
        bubbleVisible = true;
        if (!preservePinned) bubblePinned = false;
        try {
            if (bubble.getParent() == null) {
                windowManager.addView(bubble, bubbleLp);
                bubble.setScaleX(0f); bubble.setScaleY(0f); bubble.setAlpha(0f);
                bubble.setVisibility(View.VISIBLE);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(
                    ObjectAnimator.ofFloat(bubble, "scaleX", 0f, 1.12f, 1f),
                    ObjectAnimator.ofFloat(bubble, "scaleY", 0f, 1.12f, 1f),
                    ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f)
                );
                set.setDuration(320);
                set.setInterpolator(OVERSHOOT);
                set.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
        if (preservePinned && wasPinned) {
            return;
        }
        scheduleAutoHide();
    }

    void refreshEdgeGestures() {
        remove(leftEdge); remove(rightEdge); remove(leftTrail); remove(rightTrail);
        leftEdge = null; rightEdge = null; leftTrail = null; rightTrail = null;
        if (Settings.canDrawOverlays(this)) addEdgeGestures();
    }

    private void scheduleAutoHide() {
        handler.removeCallbacks(hideBubbleRunnable);
        handler.postDelayed(hideBubbleRunnable, 8000);
    }

    @Override public void onCreate() {
        super.onCreate();
        try {
            settings = new SettingsStore(this);
            volumeController = new VolumeController(this, settings);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            tts = new TextToSpeech(this, this);
            ActionManager.ensureChannel(this);
            startForeground(8, notification());

            if (Settings.canDrawOverlays(this)) {
                initEdgeVolumePopup();
                addEdgeGestures();
            }
            registerShakeSensor();
            if (settings.overlayEnabled()) {
                if (bubble == null) initBubble();
                if (!settings.backgroundRunning()) {
                    showBubble();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ActionManager.ACTION_STOP.equals(action)) {
                settings.setOverlayEnabled(false);
                if (settings.backgroundRunning()) settings.setBackgroundRunning(false);
                hideBubble(true);
                stopSelf();
            } else if (ActionManager.ACTION_VOLUME_UP.equals(action)) adjust(1);
            else if (ActionManager.ACTION_VOLUME_DOWN.equals(action)) adjust(-1);
            else if (ActionManager.ACTION_TOGGLE_MUTE.equals(action)) show(volumeController.muteOrRestoreMedia());
            else if (ActionManager.ACTION_HIDE_BUBBLE.equals(action)) {
                if (settings.backgroundRunning()) {
                    bubblePinned = false;
                    hideBubble(false);
                } else {
                    hideBubble(true);
                }
            } else if (ActionManager.ACTION_SHOW_BUBBLE_PERMANENT.equals(action)) {
                if (Settings.canDrawOverlays(this)) {
                    if (bubble == null) initBubble();
                    showBubblePermanent();
                }
            } else if (ActionManager.ACTION_SHOW_BUBBLE.equals(action)) {
                if (Settings.canDrawOverlays(this)) {
                    if (bubble == null) initBubble();
                    showBubble();
                }
            } else if (ActionManager.ACTION_REFRESH.equals(action)) {
                stopService(new Intent(this, FloatingVolumeService.class));
                ActionManager.startFloatingService(this);
            } else if (ActionManager.ACTION_DISMISS_NOTIFICATION.equals(action)) {
                hideBubble(true);
                stopSelf();
            }
        } else if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            if (bubble == null) initBubble();
            if (!settings.backgroundRunning()) {
                showBubble();
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        remove(bubble); remove(indicator); remove(leftEdge); remove(rightEdge);
        remove(leftTrail); remove(rightTrail);
        handler.removeCallbacks(singleTapRunnable);
        handler.removeCallbacks(hideIndicator);
        if (sensorManager != null) sensorManager.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
        if (edgeVolumePopup != null) edgeVolumePopup.hide(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void initBubble() {
        if (bubble != null) return;
        if (windowManager == null) return;
        try {
            bubble = new FrameLayout(this);
            int hue = settings.bubbleColorHue();
            float[] hsv = new float[]{hue, 0.55f, 0.72f};
            int color = Color.HSVToColor(Math.round(settings.buttonOpacity() * 2.55f), hsv);
            bubble.setBackground(new CircleDrawable(color));
            bubble.setContentDescription("Volume control bubble");
            TextView icon = new TextView(this);
            icon.setText("\u25B2\u25BC");
            icon.setTextColor(Color.WHITE);
            icon.setTextSize(dp(11));
            icon.setGravity(Gravity.CENTER);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            bubble.addView(icon, new FrameLayout.LayoutParams(-1, -1));
            int size = dp(settings.buttonSizeDp());
            bubbleLp = baseParams(size, size);
            bubbleLp.gravity = Gravity.TOP | Gravity.START;
            bubbleLp.x = settings.buttonX();
            bubbleLp.y = settings.buttonY();
            bubble.setOnTouchListener(new BubbleTouch(bubbleLp));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addEdgeGestures() {
        if (!settings.edgeGestures()) return;
        if (windowManager == null) return;
        try {
            leftEdge = edgeView(-1);
            rightEdge = edgeView(1);
            leftTrail = trailView();
            rightTrail = trailView();
            int width = dp(settings.edgeWidthDp());
            WindowManager.LayoutParams left = baseParams(width, -1);
            left.gravity = Gravity.START | Gravity.TOP;
            WindowManager.LayoutParams right = baseParams(width, -1);
            right.gravity = Gravity.END | Gravity.TOP;
            WindowManager.LayoutParams trailLeft = baseParams(width, -1);
            trailLeft.gravity = Gravity.START | Gravity.TOP;
            trailLeft.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            WindowManager.LayoutParams trailRight = baseParams(width, -1);
            trailRight.gravity = Gravity.END | Gravity.TOP;
            trailRight.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            windowManager.addView(leftEdge, left);
            windowManager.addView(rightEdge, right);
            windowManager.addView(leftTrail, trailLeft);
            windowManager.addView(rightTrail, trailRight);
            leftTrail.setAlpha(0f);
            rightTrail.setAlpha(0f);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private View trailView() {
        View v = new View(this);
        int hue = settings.bubbleColorHue();
        int color = Color.HSVToColor(new float[]{hue, 0.7f, 0.9f});
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{color, Color.TRANSPARENT});
        v.setBackground(gd);
        return v;
    }

    private void initEdgeVolumePopup() {
        if (windowManager == null) return;

        edgeVolumePopup = new EdgeVolumePopup(this, windowManager, settings);
        edgeVolumePopup.setVolumeCallback((volume, max) -> {
            sendBroadcast(new Intent(ActionManager.ACTION_VOLUME_CHANGED));
        });
    }

    private View edgeView(int sign) {
        View view = new View(this);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setOnTouchListener(new EdgeTouch(sign));
        view.setContentDescription(sign > 0 ? "Right edge volume gesture" : "Left edge volume gesture");
        return view;
    }

    private WindowManager.LayoutParams baseParams(int width, int height) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.alpha = 1f;
        return lp;
    }

    private void adjust(int direction) {
        if (adjustDebounce) return;
        adjustDebounce = true;
        VolumeController.VolumeState state = volumeController.changeBySteps(direction);
        speakVolume(state);
        pulseBubble();
        show(state);
        handler.postDelayed(() -> adjustDebounce = false, 120);
    }

    private void pulseBubble() {
        if (bubble == null || bubble.getParent() == null) return;
        bubble.animate().cancel();
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(bubble, "scaleX", 1f, 1.15f, 0.94f, 1.04f, 1f),
            ObjectAnimator.ofFloat(bubble, "scaleY", 1f, 1.15f, 0.94f, 1.04f, 1f)
        );
            set.setDuration(380);
            set.setInterpolator(ACCEL_DECEL);
            set.start();
    }

    private void showEdgeTrail(View trail, float startY, float dy) {
        if (trail == null) return;
        trail.animate().cancel();
        int height = Math.round(Math.abs(dy));
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) trail.getLayoutParams();
        lp.height = Math.max(height, dp(40));
        if (windowManager != null) windowManager.updateViewLayout(trail, lp);
        float fromAlpha = dy > 0 ? 0.3f : 0.1f;
        trail.setAlpha(fromAlpha);
        trail.animate()
            .alpha(0.6f)
            .setDuration(80)
            .setInterpolator(DECEL)
            .withEndAction(() -> trail.animate().alpha(0f).setDuration(250).setInterpolator(DECEL).start())
            .start();
    }

    private void speakVolume(VolumeController.VolumeState state) {
        if (!settings.accessibilitySpeech() || !ttsReady) return;
        String msg = state.percent() + " percent";
        if (state.isMuted()) msg = "Muted";
        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "volume");
    }

    private void show(VolumeController.VolumeState state) {
        haptic();
        if (!settings.visualIndicator()) return;
        if (indicator == null) {
            indicator = new TextView(this);
            indicator.setTextColor(Color.WHITE);
            indicator.setTextSize(16);
            indicator.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            indicator.setGravity(Gravity.CENTER);
            int pad = dp(12);
            indicator.setPadding(pad, pad, pad, pad);
            indicator.setBackground(new RoundRectDrawable(Color.argb(230, 32, 28, 36), dp(24)));
            indicator.setAlpha(0f);
            indicator.setVisibility(View.GONE);
            indicator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            WindowManager.LayoutParams lp = baseParams(-2, -2);
            lp.gravity = Gravity.CENTER;
            if (windowManager != null) windowManager.addView(indicator, lp);
        }
        String streamLabel = state.stream == android.media.AudioManager.STREAM_MUSIC ? "\uD83C\uDFB5" : "\uD83D\uDD11";
        String muteIndicator = state.isMuted() ? " \uD83D\uDD07" : "";
        indicator.setText(streamLabel + "  " + state.percent() + "%" + muteIndicator);
        indicator.setVisibility(View.VISIBLE);
        indicator.animate().cancel();
        indicator.setScaleX(0.7f); indicator.setScaleY(0.7f);
        indicator.setAlpha(0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(indicator, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(indicator, "scaleX", 0.7f, 1.08f, 1f),
            ObjectAnimator.ofFloat(indicator, "scaleY", 0.7f, 1.08f, 1f)
        );
            set.setDuration(220);
            set.setInterpolator(OVERSHOOT);
            set.start();
        handler.removeCallbacks(hideIndicator);
        handler.postDelayed(hideIndicator, 1200);
    }

    private final Runnable hideIndicator = () -> {
        if (indicator != null && indicator.getParent() != null) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                ObjectAnimator.ofFloat(indicator, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(indicator, "scaleX", 1f, 0.8f),
                ObjectAnimator.ofFloat(indicator, "scaleY", 1f, 0.8f)
            );
            set.setDuration(200);
            set.setInterpolator(DECEL);
            set.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    if (indicator != null) indicator.setVisibility(View.GONE);
                }
            });
            set.start();
        }
    };

    private void haptic() {
        if (!settings.haptics()) return;
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(18);
    }

    private void registerShakeSensor() {
        if (!settings.shakeToMute()) return;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float g = (float) Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]) / SensorManager.GRAVITY_EARTH;
        float threshold = settings.shakeThreshold() / 100f;
        long now = System.currentTimeMillis();
        if (g > threshold && now - lastShake > 1200) {
            lastShake = now;
            pulseBubble();
            show(volumeController.muteOrRestoreMedia());
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent up = actionIntent(ActionManager.ACTION_VOLUME_UP, 2);
        PendingIntent down = actionIntent(ActionManager.ACTION_VOLUME_DOWN, 3);
        PendingIntent mute = actionIntent(ActionManager.ACTION_TOGGLE_MUTE, 4);
        PendingIntent showBubble = actionIntent(ActionManager.ACTION_SHOW_BUBBLE_PERMANENT, 5);
        PendingIntent stop = actionIntent(ActionManager.ACTION_STOP, 6);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ActionManager.CHANNEL_ID) : new Notification.Builder(this);
        boolean hideNotif = settings.hideNotification();
        Notification.Builder nb = builder.setSmallIcon(R.drawable.ic_volume)
                .setContentTitle(hideNotif ? "." : "Virtual Buttons is ready")
                .setContentText(hideNotif ? "" : "Tap tile to show bubble \u2022 Swipe edges \u2022 Tap notification actions.")
                .setOngoing(!hideNotif)
                .setContentIntent(open)
                .addAction(R.drawable.ic_action_down, "Down", down)
                .addAction(R.drawable.ic_action_up, "Up", up)
                .addAction(R.drawable.ic_action_mute, "Mute", mute)
                .addAction(R.drawable.ic_action_show, "Show", showBubble)
                .addAction(R.drawable.ic_action_stop, "Stop", stop);
        if (hideNotif) {
            PendingIntent dismiss = actionIntent(ActionManager.ACTION_DISMISS_NOTIFICATION, 7);
            nb.addAction(R.drawable.ic_volume, "Hide", dismiss);
        }
        return nb.build();
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, FloatingVolumeService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void remove(View view) { if (view != null) try { if (windowManager != null) windowManager.removeView(view); } catch (IllegalArgumentException ignored) {} }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class BubbleTouch implements View.OnTouchListener {
        private final WindowManager.LayoutParams lp;
        private float downRawX, downRawY, downX, downY;
        private long lastTap;
        private boolean didMove = false;
        private boolean tracking = false;
        BubbleTouch(WindowManager.LayoutParams lp) { this.lp = lp; }
        private void resetTouchState() {
            didMove = false;
            tracking = false;
        }
        @Override public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    AnimatorSet press = new AnimatorSet();
                    press.playTogether(
                        ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.88f, 1f),
                        ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.88f, 1f)
                    );
                    press.setDuration(120);
                    press.setInterpolator(ACCEL_DECEL);
                    press.start();
                    downRawX = event.getRawX(); downRawY = event.getRawY(); downX = lp.x; downY = lp.y; didMove = false; tracking = true;
                    v.postDelayed(longPressCheck, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tracking) return true;
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    if (!didMove && Math.hypot(dx, dy) > dp(4)) { didMove = true; v.removeCallbacks(longPressCheck); }
                    if (didMove) {
                        int displayW = windowManager.getDefaultDisplay().getWidth();
                        int displayH = windowManager.getDefaultDisplay().getHeight();
                        int bubbleW = bubble != null ? bubble.getWidth() : settings.buttonSizeDp();
                        int bubbleH = bubble != null ? bubble.getHeight() : settings.buttonSizeDp();
                        lp.x = Math.round(Math.max(0, Math.min(displayW - bubbleW, downX + dx)));
                        lp.y = Math.round(Math.max(0, Math.min(displayH - bubbleH, downY + dy)));
                        if (windowManager != null) windowManager.updateViewLayout(bubble, lp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    v.removeCallbacks(longPressCheck);
                    resetTouchState();
                    float fdx = event.getRawX() - downRawX;
                    float fdy = event.getRawY() - downRawY;
                    double dist = Math.hypot(fdx, fdy);
                    boolean moved = didMove || dist > gestureThreshold();
                    long now = System.currentTimeMillis();
                    if (moved && allowsSwipe()) {
                        handler.removeCallbacks(singleTapRunnable);
                        int dir = fdy > 0 ? 1 : -1;
                        adjust(dir);
                    } else if (allowsDoubleTap() && now - lastTap < 330) {
                        handler.removeCallbacks(singleTapRunnable);
                        show(volumeController.muteOrRestoreMedia());
                    } else if (!moved) {
                        if (allowsDoubleTap()) {
                            handler.postDelayed(singleTapRunnable, 340);
                        } else {
                            adjust(1);
                        }
                    }
                    lastTap = now;
                    settings.setButtonPosition(lp.x, lp.y);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    v.removeCallbacks(longPressCheck);
                    resetTouchState();
                    lastTap = System.currentTimeMillis();
                    return true;
            }
            return false;
        }
        private final Runnable longPressCheck = new Runnable() {
            @Override public void run() {
                Vibrator vr = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vr != null && vr.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vr.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE));
                    else vr.vibrate(8);
                }
                hideBubble();
            }
        };
        private boolean allowsSwipe() { return settings.gestureMode() != SettingsStore.GestureMode.DOUBLE_TAP; }
        private boolean allowsDoubleTap() { return settings.gestureMode() != SettingsStore.GestureMode.SWIPE; }
        private int gestureThreshold() { return dp(settings.gestureSensitivity()); }
    }

    private final class EdgeTouch implements View.OnTouchListener {
        private final int sign;
        private float downX, downY;
        private boolean isDragging = false;
        private boolean longPressTriggered = false;
        private final Runnable longPressRunnable = new Runnable() {
            @Override public void run() {
                if (!edgeLongPressEnabled || !settings.edgeGestures() || edgeVolumePopup == null || isDragging) return;
                longPressTriggered = true;
                haptic();
                edgeVolumePopup.show(downX, downY, sign);
            }
        };
        EdgeTouch(int sign) { this.sign = sign; }
        private int getVerticalDragThreshold() {
            int sensitivity = settings.gestureSensitivity();
            int minThreshold = dp(4);
            int maxThreshold = dp(24);
            int range = maxThreshold - minThreshold;
            int inverseSens = 96 - sensitivity;
            return minThreshold + (inverseSens * range / 80);
        }
        @Override public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getRawX();
                downY = event.getRawY();
                isDragging = false;
                longPressTriggered = false;
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                if (!isDragging && Math.abs(dy) > getVerticalDragThreshold()) {
                    isDragging = true;
                    handler.removeCallbacks(longPressRunnable);
                }
                if (isDragging && !longPressTriggered) {
                    showEdgeTrail(sign > 0 ? rightTrail : leftTrail, downY, dy);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                handler.removeCallbacks(longPressRunnable);
                float dy = event.getRawY() - downY;
                if (!longPressTriggered && (isDragging || Math.abs(dy) > getVerticalDragThreshold())) {
                    adjust(dy < 0 ? 1 : -1);
                }
                isDragging = false;
                longPressTriggered = false;
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(longPressRunnable);
                isDragging = false;
                longPressTriggered = false;
                return true;
            }
            return true;
        }
    }
}
