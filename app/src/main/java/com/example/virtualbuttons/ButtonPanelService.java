package com.example.virtualbuttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ButtonPanelService extends Service implements SensorEventListener {
    private SettingsStore settings;
    private WindowManager windowManager;
    private FrameLayout panel;
    private View[] buttons;
    private SensorManager sensorManager;
    private long lastShake;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean panelVisible = false;
    private WindowManager.LayoutParams panelLp;
    private PowerManager powerManager;
    private AudioManager audioManager;
    private int maxVolume;

    private static final int[] BUTTON_ICONS = {
        R.drawable.ic_power,
        R.drawable.ic_volume_up,
        R.drawable.ic_volume_down,
        R.drawable.ic_home,
        R.drawable.ic_recents,
        R.drawable.ic_back
    };

    private static final String[] BUTTON_ACTIONS = {
        ActionManager.ACTION_BUTTON_POWER,
        ActionManager.ACTION_VOLUME_UP,
        ActionManager.ACTION_VOLUME_DOWN,
        ActionManager.ACTION_BUTTON_HOME,
        ActionManager.ACTION_BUTTON_RECENTS,
        ActionManager.ACTION_BUTTON_BACK
    };

    private static final SettingsStore.ButtonType[] BUTTON_TYPES = {
        SettingsStore.ButtonType.POWER,
        SettingsStore.ButtonType.VOLUME_UP,
        SettingsStore.ButtonType.VOLUME_DOWN,
        SettingsStore.ButtonType.HOME,
        SettingsStore.ButtonType.RECENTS,
        SettingsStore.ButtonType.BACK
    };

    @Override public void onCreate() {
        super.onCreate();
        settings = new SettingsStore(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        ActionManager.ensureChannel(this);
        startForeground(9, notification());
        try {
            if (Settings.canDrawOverlays(this)) {
                showPanel();
            }
        } catch (Exception e) { e.printStackTrace(); }
        registerShakeSensor();
    }

    private void createPanel() {
        if (!settings.showButtonPanel()) return;
        if (panel != null) return;

        panel = new FrameLayout(this);
        int panelSize = dp(settings.buttonPanelSize());
        int buttonSize = panelSize / 3;
        int padding = dp(4);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setGravity(Gravity.CENTER);

        int hue = settings.bubbleColorHue();
        int bgColor = Color.HSVToColor(Math.round(settings.buttonPanelOpacity() * 2.55f), new float[]{hue, 0.4f, 0.25f});
        int btnColor = Color.HSVToColor(230, new float[]{hue, 0.5f, 0.85f});

        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setCornerRadius(dp(20));
        panelBg.setColor(bgColor);
        panel.setBackground(panelBg);

        buttons = new View[6];
        int idx = 0;
        for (int row = 0; row < 2; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setGravity(Gravity.CENTER);
            for (int col = 0; col < 3; col++) {
                View btn = createButton(idx, buttonSize, btnColor);
                buttons[idx] = btn;
                rowLayout.addView(btn);
                idx++;
            }
            rows.addView(rowLayout);
        }

        panel.addView(rows, new FrameLayout.LayoutParams(-1, -1));
        panelLp = new WindowManager.LayoutParams(panelSize, panelSize * 2 / 3 + dp(20),
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        panelLp.alpha = 1f;
        panelLp.gravity = getPanelGravity(settings.buttonPanelPosition());

        int pos = settings.buttonPanelPosition();
        if (pos == 0) { panelLp.x = dp(16); panelLp.y = dp(40); }
        else if (pos == 1) { panelLp.x = dp(16); panelLp.y = dp(400); }
        else { panelLp.x = dp(16); panelLp.y = dp(200); }

        panel.setOnTouchListener(new PanelTouchListener());
    }

    private int getPanelGravity(int pos) {
        if (pos == 0) return Gravity.TOP | Gravity.START;
        if (pos == 1) return Gravity.BOTTOM | Gravity.START;
        return Gravity.CENTER | Gravity.START;
    }

    private View createButton(int idx, int size, int color) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(4), dp(4), dp(4), dp(4));

        ImageView icon = new ImageView(this);
        icon.setImageResource(BUTTON_ICONS[idx]);
        icon.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(size / 2, size / 2);
        icon.setLayoutParams(iconLp);
        btn.addView(icon);

        boolean enabled = settings.buttonEnabled(BUTTON_TYPES[idx]);
        btn.setContentDescription(getButtonDescription(idx) + (enabled ? "" : " disabled"));
        btn.setEnabled(enabled);
        btn.setAlpha(enabled ? 1f : 0.35f);
        btn.setTag(BUTTON_ACTIONS[idx]);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.argb(80, 255, 255, 255));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size - dp(4), size - dp(4));
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            if (settings.buttonEnabled(BUTTON_TYPES[idx])) handleButtonClick(idx);
        });

        return btn;
    }

    private String getButtonDescription(int idx) {
        switch (idx) {
            case 0: return "Power button";
            case 1: return "Volume up";
            case 2: return "Volume down";
            case 3: return "Home button";
            case 4: return "Recents button";
            case 5: return "Back button";
            default: return "Button";
        }
    }

    private void handleButtonClick(int idx) {
        haptic();
        animateButtonPress(buttons[idx]);
        String action = BUTTON_ACTIONS[idx];
        executeAction(action);
    }

    private void executeAction(String action) {
        switch (action) {
            case ActionManager.ACTION_BUTTON_POWER:
                triggerPowerButton();
                break;
            case ActionManager.ACTION_VOLUME_UP:
                adjustVolume(1);
                break;
            case ActionManager.ACTION_VOLUME_DOWN:
                adjustVolume(-1);
                break;
            case ActionManager.ACTION_BUTTON_HOME:
                if (!ActionManager.performAccessibilityAction(action)) {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                }
                break;
            case ActionManager.ACTION_BUTTON_RECENTS:
                if (!ActionManager.performAccessibilityAction(action)) {
                    try {
                        Intent recents = new Intent("com.android.systemui.recents.TOGGLE_RECENTS");
                        recents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(recents);
                    } catch (Exception e) {
                        try {
                            Intent overview = new Intent(Intent.ACTION_ASSIST);
                            overview.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(overview);
                        } catch (Exception ex) {}
                    }
                }
                break;
            case ActionManager.ACTION_BUTTON_BACK:
                if (!ActionManager.performAccessibilityAction(action)) {
                    try {
                        Runtime.getRuntime().exec("input keyevent KEYCODE_BACK");
                    } catch (Exception e) {
                        try {
                            Runtime.getRuntime().exec("input keyevent 4");
                        } catch (Exception ex) {}
                    }
                }
                break;
        }
    }

    private void adjustVolume(int direction) {
        if (audioManager == null) return;
        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int step = Math.max(1, settings.volumeStep());
            int next = Math.max(0, Math.min(maxVolume, current + (direction * step)));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void animateButtonPress(View btn) {
        if (btn == null) return;
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(btn, "scaleX", 1f, 0.85f, 1f),
            ObjectAnimator.ofFloat(btn, "scaleY", 1f, 0.85f, 1f)
        );
        set.setDuration(150).start();
    }

    private void haptic() {
        if (!settings.hapticFeedback()) return;
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(20);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (ActionManager.ACTION_STOP.equals(intent.getAction())) {
                hidePanel();
                stopSelf();
            } else if (ActionManager.ACTION_REFRESH.equals(intent.getAction())) {
                refreshPanel();
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        removePanel();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void removePanel() {
        if (panel != null && panel.getParent() != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
    }

    private void hidePanel() {
        if (panel != null && panel.getParent() != null) {
            panel.animate().alpha(0f).setDuration(200).withEndAction(() -> removePanel()).start();
        }
        panelVisible = false;
    }

    private void showPanel() {
        if (panel == null) createPanel();
        if (panel != null && panel.getParent() == null && windowManager != null) {
            windowManager.addView(panel, panelLp);
            panel.setAlpha(0f);
            panel.animate().alpha(1f).setDuration(200).start();
        }
        panelVisible = true;
    }

    private void refreshPanel() {
        removePanel();
        if (settings.showButtonPanel()) showPanel();
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
            haptic();
            adjustVolume(-1);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, ModernMainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 6, new Intent(this, ButtonPanelService.class).setAction(ActionManager.ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ActionManager.CHANNEL_ID_BUTTONS) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_volume)
            .setContentTitle("Virtual Buttons Active")
            .setContentText("Tap to open settings")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_action_stop, "Stop", stop)
            .build();
    }

    private void triggerPowerButton() {
        try {
            Runtime.getRuntime().exec("input keyevent KEYCODE_POWER");
        } catch (Exception e) {
            try {
                Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ex) {}
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class PanelTouchListener implements View.OnTouchListener {
        private float downRawX, downRawY;
        private boolean isDragging = false;

        @Override public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!isDragging && Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY) > dp(8)) {
                        isDragging = true;
                    }
                    if (isDragging && panelLp != null) {
                        panelLp.x = (int) (event.getRawX() - dp(48));
                        panelLp.y = (int) (event.getRawY() - dp(40));
                        panelLp.x = Math.max(0, Math.min(windowManager.getDefaultDisplay().getWidth() - dp(120), panelLp.x));
                        panelLp.y = Math.max(0, Math.min(windowManager.getDefaultDisplay().getHeight() - dp(120), panelLp.y));
                        if (windowManager != null) windowManager.updateViewLayout(panel, panelLp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        showPanel();
                    } else {
                        settings.setButtonPanelPosition(determinePosition());
                    }
                    return true;
            }
            return false;
        }

        private int determinePosition() {
            int h = windowManager.getDefaultDisplay().getHeight();
            return panelLp.y > h / 2 ? 1 : 0;
        }
    }
}