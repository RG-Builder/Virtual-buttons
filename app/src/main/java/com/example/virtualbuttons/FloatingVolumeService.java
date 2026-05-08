package com.example.virtualbuttons;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FloatingVolumeService extends Service implements SensorEventListener {
    private SettingsStore settings;
    private VolumeController volumeController;
    private WindowManager windowManager;
    private FrameLayout bubble;
    private TextView indicator;
    private View leftEdge;
    private View rightEdge;
    private SensorManager sensorManager;
    private long lastShake;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable singleTapRunnable = () -> adjust(1);
    private final Runnable hideBubbleRunnable = () -> hideBubble();
    private boolean bubbleVisible = false;
    private WindowManager.LayoutParams bubbleLp;

void hideBubble() {
        if (bubble != null && bubble.getParent() != null && windowManager != null) {
            bubble.animate().scaleX(0.5f).scaleY(0.5f).alpha(0f).setDuration(150).withEndAction(() -> {
                try { if (windowManager != null) windowManager.removeView(bubble); } catch (Exception ignored) {}
            }).start();
        }
        bubbleVisible = false;
        handler.removeCallbacks(hideBubbleRunnable);
    }
    void showBubble() {
        if (bubble == null) initBubble();
        if (bubble != null && bubble.getParent() == null && windowManager != null) {
            windowManager.addView(bubble, bubbleLp);
            bubble.setScaleX(0.5f);
            bubble.setScaleY(0.5f);
            bubble.setAlpha(0f);
            bubble.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200).start();
        }
        bubbleVisible = true;
        scheduleAutoHide();
    }
    void refreshEdgeGestures() { remove(leftEdge); remove(rightEdge); if (Settings.canDrawOverlays(this)) addEdgeGestures(); }
    private void scheduleAutoHide() { handler.removeCallbacks(hideBubbleRunnable); handler.postDelayed(hideBubbleRunnable, 8000); }

    @Override public void onCreate() {
        super.onCreate();
        settings = new SettingsStore(this);
        volumeController = new VolumeController(this, settings);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        AppActions.ensureChannel(this);
        startForeground(8, notification());
        if (Settings.canDrawOverlays(this)) {
            addEdgeGestures();
        }
        registerShakeSensor();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (AppActions.ACTION_STOP.equals(action)) {
                settings.setOverlayEnabled(false);
                hideBubble();
                stopSelf();
            } else if (AppActions.ACTION_VOLUME_UP.equals(action)) adjust(1);
            else if (AppActions.ACTION_VOLUME_DOWN.equals(action)) adjust(-1);
            else if (AppActions.ACTION_TOGGLE_MUTE.equals(action)) show(volumeController.muteOrRestoreMedia());
            else if (AppActions.ACTION_HIDE_BUBBLE.equals(action)) hideBubble();
            else if (AppActions.ACTION_SHOW_BUBBLE.equals(action)) {
                if (Settings.canDrawOverlays(this)) {
                    if (bubble == null) initBubble();
                    showBubble();
                }
            } else if (AppActions.ACTION_REFRESH.equals(action)) {
                stopService(new Intent(this, FloatingVolumeService.class));
                AppActions.startFloatingService(this);
            }
        } else if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            if (bubble == null) initBubble();
            showBubble();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        remove(bubble);
        remove(indicator);
        remove(leftEdge);
        remove(rightEdge);
        handler.removeCallbacks(singleTapRunnable);
        handler.removeCallbacks(hideIndicator);
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void initBubble() {
        if (bubble != null) return;
        bubble = new FrameLayout(this);
        int color = Color.argb(Math.round(settings.buttonOpacity() * 2.55f), 103, 80, 164);
        bubble.setBackground(new CircleDrawable(color));
        bubble.setContentDescription("Volume control bubble. Swipe up or down to change volume. Double-tap to mute. Long-press to hide.");
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
    }

    private void addEdgeGestures() {
        if (!settings.edgeGestures()) return;
        leftEdge = edgeView(-1);
        rightEdge = edgeView(1);
        int width = dp(settings.edgeWidthDp());
        WindowManager.LayoutParams left = baseParams(width, -1);
        left.gravity = Gravity.START | Gravity.TOP;
        WindowManager.LayoutParams right = baseParams(width, -1);
        right.gravity = Gravity.END | Gravity.TOP;
        if (windowManager != null) {
            windowManager.addView(leftEdge, left);
            windowManager.addView(rightEdge, right);
        }
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

    private void adjust(int direction) { show(volumeController.changeBySteps(direction)); }

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
        indicator.setAlpha(0f);
        indicator.animate().alpha(1f).setDuration(120).start();
        handler.removeCallbacks(hideIndicator);
        handler.postDelayed(hideIndicator, 1200);
    }

    private final Runnable hideIndicator = () -> {
        if (indicator != null) {
            indicator.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                if (indicator != null) indicator.setVisibility(View.GONE);
            }).start();
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
        long now = System.currentTimeMillis();
        if (g > 2.7f && now - lastShake > 1200) {
            lastShake = now;
            show(volumeController.muteOrRestoreMedia());
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent up = actionIntent(AppActions.ACTION_VOLUME_UP, 2);
        PendingIntent down = actionIntent(AppActions.ACTION_VOLUME_DOWN, 3);
        PendingIntent mute = actionIntent(AppActions.ACTION_TOGGLE_MUTE, 4);
        PendingIntent showBubble = actionIntent(AppActions.ACTION_SHOW_BUBBLE, 5);
        PendingIntent stop = actionIntent(AppActions.ACTION_STOP, 6);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, AppActions.CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_volume)
                .setContentTitle("Virtual Buttons is ready")
                .setContentText("Tap tile to show bubble \u2022 Swipe edges \u2022 Tap notification actions.")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(android.R.drawable.arrow_down_float, "Down", down)
                .addAction(android.R.drawable.arrow_up_float, "Up", up)
                .addAction(android.R.drawable.ic_lock_silent_mode, "Mute", mute)
                .addAction(android.R.drawable.ic_menu_view, "Show", showBubble)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
                .build();
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
        private long downAt, lastTap;
        private boolean didMove = false;
        BubbleTouch(WindowManager.LayoutParams lp) { this.lp = lp; }
        @Override public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX(); downRawY = event.getRawY(); downX = lp.x; downY = lp.y; downAt = System.currentTimeMillis(); didMove = false;
                    v.postDelayed(longPressCheck, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    if (Math.hypot(dx, dy) > dp(4)) { didMove = true; v.removeCallbacks(longPressCheck); }
                    if (didMove) { lp.x = Math.round(downX + dx); lp.y = Math.round(downY + dy); if (windowManager != null) windowManager.updateViewLayout(bubble, lp); }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.removeCallbacks(longPressCheck);
                    float fdx = event.getRawX() - downRawX;
                    float fdy = event.getRawY() - downRawY;
                    boolean moved = Math.hypot(fdx, fdy) > dp(settings.gestureSensitivity());
                    long now = System.currentTimeMillis();
                    if (moved && allowsSwipe()) {
                        handler.removeCallbacks(singleTapRunnable);
                        adjust(fdy < 0 ? 1 : -1);
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
            }
            return false;
        }
        private final Runnable longPressCheck = new Runnable() {
            @Override public void run() { hideBubble(); }
        };
        private boolean allowsSwipe() { return settings.gestureMode() != SettingsStore.GestureMode.DOUBLE_TAP; }
        private boolean allowsDoubleTap() { return settings.gestureMode() != SettingsStore.GestureMode.SWIPE; }
    }

    private final class EdgeTouch implements View.OnTouchListener {
        private final int sign;
        private float downX, downY;
        EdgeTouch(int sign) { this.sign = sign; }
        @Override public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getRawX();
                downY = event.getRawY();
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                boolean vertical = Math.abs(dy) > Math.abs(dx);
                if (vertical && Math.abs(dy) > dp(settings.gestureSensitivity())) {
                    adjust(dy < 0 ? 1 : -1);
                } else if (!vertical) {
                    // ignore horizontal swipes to avoid interfering with app gestures
                } else {
                    adjust(sign > 0 ? 1 : -1);
                }
                return true;
            }
            return true;
        }
    }
}
