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
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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

    @Override public void onCreate() {
        super.onCreate();
        settings = new SettingsStore(this);
        volumeController = new VolumeController(this, settings);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        AppActions.ensureChannel(this);
        startForeground(8, notification());
        if (Settings.canDrawOverlays(this)) {
            addBubble();
            addEdgeGestures();
        }
        registerShakeSensor();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (AppActions.ACTION_STOP.equals(action)) {
                settings.setOverlayEnabled(false);
                stopSelf();
            } else if (AppActions.ACTION_VOLUME_UP.equals(action)) adjust(1);
            else if (AppActions.ACTION_VOLUME_DOWN.equals(action)) adjust(-1);
            else if (AppActions.ACTION_TOGGLE_MUTE.equals(action)) show(volumeController.muteOrRestoreMedia());
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        remove(bubble);
        remove(indicator);
        remove(leftEdge);
        remove(rightEdge);
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void addBubble() {
        bubble = new FrameLayout(this);
        int color = Color.argb(Math.round(settings.buttonOpacity() * 2.55f), 103, 80, 164);
        bubble.setBackground(new CircleDrawable(color));
        TextView icon = new TextView(this);
        icon.setText("↕");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(30);
        icon.setGravity(Gravity.CENTER);
        bubble.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        int size = dp(settings.buttonSizeDp());
        WindowManager.LayoutParams lp = baseParams(size, size);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = settings.buttonX();
        lp.y = settings.buttonY();
        bubble.setOnTouchListener(new BubbleTouch(lp));
        windowManager.addView(bubble, lp);
    }

    private void addEdgeGestures() {
        if (!settings.edgeGestures()) return;
        leftEdge = edgeView(-1);
        rightEdge = edgeView(1);
        WindowManager.LayoutParams left = baseParams(dp(18), -1);
        left.gravity = Gravity.START | Gravity.TOP;
        WindowManager.LayoutParams right = baseParams(dp(18), -1);
        right.gravity = Gravity.END | Gravity.TOP;
        windowManager.addView(leftEdge, left);
        windowManager.addView(rightEdge, right);
    }

    private View edgeView(int sign) {
        View view = new View(this);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setOnTouchListener(new EdgeTouch(sign));
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
            indicator.setTextSize(18);
            indicator.setGravity(Gravity.CENTER);
            indicator.setBackground(new RoundRectDrawable(Color.argb(220, 32, 28, 36), dp(18)));
            WindowManager.LayoutParams lp = baseParams(dp(190), dp(72));
            lp.gravity = Gravity.CENTER;
            windowManager.addView(indicator, lp);
        }
        indicator.setText((state.stream == android.media.AudioManager.STREAM_MUSIC ? "Media" : "System") + " volume  " + state.percent() + "%");
        indicator.setVisibility(View.VISIBLE);
        indicator.removeCallbacks(hideIndicator);
        indicator.postDelayed(hideIndicator, 900);
    }

    private final Runnable hideIndicator = () -> { if (indicator != null) indicator.setVisibility(View.GONE); };

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
        PendingIntent stop = actionIntent(AppActions.ACTION_STOP, 5);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, AppActions.CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("Virtual Buttons is ready")
                .setContentText("Use the floating button, edges, tile, or notification actions.")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(android.R.drawable.arrow_down_float, "Down", down)
                .addAction(android.R.drawable.arrow_up_float, "Up", up)
                .addAction(android.R.drawable.ic_lock_silent_mode, "Mute", mute)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
                .build();
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, FloatingVolumeService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void remove(View view) { if (view != null) try { windowManager.removeView(view); } catch (IllegalArgumentException ignored) {} }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class BubbleTouch implements View.OnTouchListener {
        private final WindowManager.LayoutParams lp;
        private float downRawX, downRawY, downX, downY;
        private long downAt, lastTap;
        BubbleTouch(WindowManager.LayoutParams lp) { this.lp = lp; }
        @Override public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX(); downRawY = event.getRawY(); downX = lp.x; downY = lp.y; downAt = System.currentTimeMillis(); return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = Math.round(downX + event.getRawX() - downRawX); lp.y = Math.round(downY + event.getRawY() - downRawY); windowManager.updateViewLayout(bubble, lp); return true;
                case MotionEvent.ACTION_UP:
                    float dy = event.getRawY() - downRawY;
                    float dx = event.getRawX() - downRawX;
                    boolean moved = Math.hypot(dx, dy) > dp(settings.gestureSensitivity());
                    if (moved && allowsSwipe()) adjust(dy < 0 ? 1 : -1);
                    else if (allowsDoubleTap() && System.currentTimeMillis() - lastTap < 330) show(volumeController.muteOrRestoreMedia());
                    else if (!moved && System.currentTimeMillis() - downAt < 220) adjust(1);
                    lastTap = System.currentTimeMillis();
                    settings.setButtonPosition(lp.x, lp.y);
                    return true;
            }
            return false;
        }
        private boolean allowsSwipe() { return settings.gestureMode() != SettingsStore.GestureMode.DOUBLE_TAP; }
        private boolean allowsDoubleTap() { return settings.gestureMode() != SettingsStore.GestureMode.SWIPE; }
    }

    private final class EdgeTouch implements View.OnTouchListener {
        private final int sign;
        private float downY;
        EdgeTouch(int sign) { this.sign = sign; }
        @Override public boolean onTouch(View v, MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { downY = event.getRawY(); return true; }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                float dy = event.getRawY() - downY;
                if (Math.abs(dy) > dp(settings.gestureSensitivity())) adjust(dy < 0 ? 1 : -1);
                else adjust(sign > 0 ? 1 : -1);
                return true;
            }
            return true;
        }
    }
}
