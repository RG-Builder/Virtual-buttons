package com.example.virtualbuttons;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;

public class EnhancedGestureService extends Service {
    private static final long ACTION_COOLDOWN = 220;
    private static final long DOUBLE_TAP_TIMEOUT_MS = 280;

    private SettingsStore settings;
    private WindowManager windowManager;
    private View topEdge, bottomEdge, leftEdge, rightEdge;
    private View topTrail, bottomTrail, leftTrail, rightTrail;
    private AudioManager audioManager;
    private int maxVolume;
    private int maxBrightness = 255;
    private long lastActionTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new SettingsStore(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        ActionManager.ensureChannel(this);
        startForeground(10, notification());
        setupEdgeGestures();
    }

    private void setupEdgeGestures() {
        if (!settings.edgeGestures() || !Settings.canDrawOverlays(this)) return;

        int edgeSize = dp(Math.max(8, settings.globalGestureWidth()));
        topEdge = createEdgeView(0);
        bottomEdge = createEdgeView(1);
        leftEdge = createEdgeView(2);
        rightEdge = createEdgeView(3);

        WindowManager.LayoutParams topParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, edgeSize, Gravity.TOP | Gravity.START);
        WindowManager.LayoutParams bottomParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, edgeSize, Gravity.BOTTOM | Gravity.START);
        WindowManager.LayoutParams leftParams = edgeParams(edgeSize, WindowManager.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams rightParams = edgeParams(edgeSize, WindowManager.LayoutParams.MATCH_PARENT, Gravity.END | Gravity.TOP);

        topTrail = createTrailView(Gravity.TOP);
        bottomTrail = createTrailView(Gravity.BOTTOM);
        leftTrail = createTrailView(Gravity.START);
        rightTrail = createTrailView(Gravity.END);

        WindowManager.LayoutParams topTrailParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, dp(2), Gravity.TOP | Gravity.START);
        WindowManager.LayoutParams bottomTrailParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, dp(2), Gravity.BOTTOM | Gravity.START);
        WindowManager.LayoutParams leftTrailParams = edgeParams(dp(2), WindowManager.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams rightTrailParams = edgeParams(dp(2), WindowManager.LayoutParams.MATCH_PARENT, Gravity.END | Gravity.TOP);

        makePassThrough(topTrailParams);
        makePassThrough(bottomTrailParams);
        makePassThrough(leftTrailParams);
        makePassThrough(rightTrailParams);

        windowManager.addView(topEdge, topParams);
        windowManager.addView(bottomEdge, bottomParams);
        windowManager.addView(leftEdge, leftParams);
        windowManager.addView(rightEdge, rightParams);
        topTrail.setAlpha(0f);
        bottomTrail.setAlpha(0f);
        leftTrail.setAlpha(0f);
        rightTrail.setAlpha(0f);
        windowManager.addView(topTrail, topTrailParams);
        windowManager.addView(bottomTrail, bottomTrailParams);
        windowManager.addView(leftTrail, leftTrailParams);
        windowManager.addView(rightTrail, rightTrailParams);
    }

    private WindowManager.LayoutParams edgeParams(int w, int h, int gravity) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT);
        lp.gravity = gravity;
        return lp;
    }

    private void makePassThrough(WindowManager.LayoutParams lp) { lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; }

    private View createEdgeView(int position) {
        View view = new View(this);
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        view.setOnTouchListener(new EdgeTouchListener(position));
        return view;
    }

    private View createTrailView(int gravity) {
        View view = new View(this);
        GradientDrawable gd = new GradientDrawable();
        int hue = settings.bubbleColorHue();
        int color = android.graphics.Color.HSVToColor((int) (120 * 0.78f), new float[]{hue, 0.60f, 0.95f});
        gd.setColor(color);
        gd.setCornerRadius(dp(32));
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gd.setOrientation((gravity == Gravity.TOP || gravity == Gravity.BOTTOM)
                ? GradientDrawable.Orientation.TOP_BOTTOM
                : GradientDrawable.Orientation.LEFT_RIGHT);
        view.setBackground(gd);
        return view;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (ActionManager.ACTION_STOP.equals(intent.getAction())) {
                removeEdgeViews();
                stopSelf();
                return START_NOT_STICKY;
            } else if (ActionManager.ACTION_REFRESH.equals(intent.getAction())) {
                removeEdgeViews();
                setupEdgeGestures();
            }
        }
        return START_STICKY;
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 2, new Intent(this, ModernMainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 7, new Intent(this, EnhancedGestureService.class).setAction(ActionManager.ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, ActionManager.CHANNEL_ID_BUTTONS)
                : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_volume)
                .setContentTitle("Edge Gestures Active")
                .setContentText("Natural button-free controls enabled")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(R.drawable.ic_action_stop, "Stop", stop)
                .build();
    }

    @Override
    public void onDestroy() {
        removeEdgeViews();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void removeEdgeViews() {
        try { if (topEdge != null) windowManager.removeView(topEdge); } catch (Exception ignored) {}
        try { if (bottomEdge != null) windowManager.removeView(bottomEdge); } catch (Exception ignored) {}
        try { if (leftEdge != null) windowManager.removeView(leftEdge); } catch (Exception ignored) {}
        try { if (rightEdge != null) windowManager.removeView(rightEdge); } catch (Exception ignored) {}
        try { if (topTrail != null) windowManager.removeView(topTrail); } catch (Exception ignored) {}
        try { if (bottomTrail != null) windowManager.removeView(bottomTrail); } catch (Exception ignored) {}
        try { if (leftTrail != null) windowManager.removeView(leftTrail); } catch (Exception ignored) {}
        try { if (rightTrail != null) windowManager.removeView(rightTrail); } catch (Exception ignored) {}
    }

    private void haptic(int strength) {
        if (!settings.hapticFeedback()) return;
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            int duration = Math.max(8, Math.min(24, strength));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(duration);
        }
    }

    private class EdgeTouchListener implements View.OnTouchListener {
        private final int position;
        private float startX, startY;
        private float lastY;
        private int moveCount;
        private long downTime;
        private long lastTapAt;
        private VelocityTracker velocityTracker;

        EdgeTouchListener(int position) { this.position = position; }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                executeAction(ActionManager.ACTION_BUTTON_RECENTS);
                haptic(20);
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    lastY = startY;
                    moveCount = 0;
                    downTime = System.currentTimeMillis();
                    velocityTracker = VelocityTracker.obtain();
                    velocityTracker.addMovement(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (velocityTracker != null) velocityTracker.addMovement(event);
                    moveCount++;
                    float dy = event.getRawY() - startY;
                    float dragProgress = Math.min(1f, Math.abs(dy) / dp(120));
                    showTrail(getTrailForPosition(position), dragProgress);
                    if ((position == 2 || position == 3) && moveCount > 2 && Math.abs(event.getRawY() - lastY) > dp(6)) {
                        adjustBrightnessDelta(event.getRawY() < lastY ? +1 : -1);
                        lastY = event.getRawY();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000);
                    }
                    handleGesture(event);
                    resetTrail();
                    if (velocityTracker != null) velocityTracker.recycle();
                    velocityTracker = null;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    resetTrail();
                    if (velocityTracker != null) velocityTracker.recycle();
                    velocityTracker = null;
                    return true;
                default:
                    return false;
            }
        }

        private void handleGesture(MotionEvent event) {
            long now = System.currentTimeMillis();
            if (now - lastActionTime < ACTION_COOLDOWN) return;
            float dx = event.getRawX() - startX;
            float dy = event.getRawY() - startY;
            float absDy = Math.abs(dy);
            float minSwipe = dp(Math.max(18, 40 - settings.globalGestureSensitivity() / 2));
            float vy = velocityTracker != null ? Math.abs(velocityTracker.getYVelocity()) : 0f;

            // double tap for lock screen
            if (absDy < dp(12) && Math.abs(dx) < dp(12) && (now - downTime) < 220) {
                if (now - lastTapAt < DOUBLE_TAP_TIMEOUT_MS) {
                    executeAction(ActionManager.ACTION_BUTTON_POWER);
                    haptic(22);
                    lastTapAt = 0;
                    lastActionTime = now;
                    return;
                }
                lastTapAt = now;
                return;
            }

            if (absDy > minSwipe || vy > 1000f) {
                if (position == 0 || position == 1 || position == 2 || position == 3) {
                    adjustVolume(dy < 0 ? +1 : -1, vy);
                    haptic(14);
                    lastActionTime = now;
                }
            }

            if ((position == 2 || position == 3) && isCornerSwipe(startX, startY, dx, dy)) {
                executeAction(dx > 0 ? ActionManager.ACTION_BUTTON_BACK : ActionManager.ACTION_BUTTON_HOME);
                haptic(20);
                lastActionTime = now;
            }
        }

        private boolean isCornerSwipe(float x, float y, float dx, float dy) {
            int cornerZone = dp(72);
            int height = getResources().getDisplayMetrics().heightPixels;
            boolean nearTop = y < cornerZone;
            boolean nearBottom = y > (height - cornerZone);
            return (nearTop || nearBottom) && Math.abs(dx) > dp(48) && Math.abs(dy) > dp(20);
        }

        private View getTrailForPosition(int pos) {
            switch (pos) {
                case 0: return topTrail;
                case 1: return bottomTrail;
                case 2: return leftTrail;
                case 3: return rightTrail;
                default: return null;
            }
        }

        private void resetTrail() {
            View trail = getTrailForPosition(position);
            if (trail != null) {
                trail.animate().alpha(0f).setDuration(150).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }
        }
    }

    private void showTrail(View trail, float progress) {
        if (trail == null) return;
        float alpha = 0.10f + (0.55f * progress * progress);
        trail.animate().alpha(alpha).setDuration(50).start();
    }

    private void executeAction(String action) {
        switch (action) {
            case ActionManager.ACTION_BUTTON_POWER:
                ActionManager.performAccessibilityAction(ActionManager.ACTION_BUTTON_POWER);
                break;
            case ActionManager.ACTION_BUTTON_HOME:
            case ActionManager.ACTION_BUTTON_RECENTS:
            case ActionManager.ACTION_BUTTON_BACK:
                ActionManager.performAccessibilityAction(action);
                break;
        }
    }

    private void adjustVolume(int direction, float velocity) {
        if (audioManager == null) return;
        int base = Math.max(1, settings.volumeStep());
        int boost = velocity > 2200f ? 2 : velocity > 1400f ? 1 : 0;
        int delta = base + boost;
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int next = Math.max(0, Math.min(maxVolume, current + (direction * delta)));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);
    }

    private void adjustBrightnessDelta(int direction) {
        try {
            int current = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
            int next = Math.max(10, Math.min(maxBrightness, current + (direction * 3)));
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, next);
        } catch (Exception ignored) { }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
