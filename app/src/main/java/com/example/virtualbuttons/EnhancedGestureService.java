package com.example.virtualbuttons;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
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

public class EnhancedGestureService extends Service {
    private SettingsStore settings;
    private WindowManager windowManager;
    private View topEdge, bottomEdge, leftEdge, rightEdge;
    private View topTrail, bottomTrail, leftTrail, rightTrail;
    private Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager powerManager;
    private android.media.AudioManager audioManager;
    private int maxVolume;
    private long lastActionTime = 0;
    private static final long ACTION_COOLDOWN = 300;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            settings = new SettingsStore(this);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            audioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
            if (audioManager != null) {
                maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            }
            ActionManager.ensureChannel(this);
            startForeground(10, notification());
            setupEdgeGestures();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEdgeGestures() {
        if (!settings.edgeGestures() || !Settings.canDrawOverlays(this)) return;

        int edgeSize = dp(settings.edgeWidthDp());

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

        WindowManager.LayoutParams topTrailParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, 1, Gravity.TOP | Gravity.START);
        WindowManager.LayoutParams bottomTrailParams = edgeParams(WindowManager.LayoutParams.MATCH_PARENT, 1, Gravity.BOTTOM | Gravity.START);
        WindowManager.LayoutParams leftTrailParams = edgeParams(1, WindowManager.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams rightTrailParams = edgeParams(1, WindowManager.LayoutParams.MATCH_PARENT, Gravity.END | Gravity.TOP);
        makePassThrough(topTrailParams);
        makePassThrough(bottomTrailParams);
        makePassThrough(leftTrailParams);
        makePassThrough(rightTrailParams);

        windowManager.addView(topEdge, topParams);
        windowManager.addView(bottomEdge, bottomParams);
        windowManager.addView(leftEdge, leftParams);
        windowManager.addView(rightEdge, rightParams);

        topTrail.setAlpha(0);
        bottomTrail.setAlpha(0);
        leftTrail.setAlpha(0);
        rightTrail.setAlpha(0);

        windowManager.addView(topTrail, topTrailParams);
        windowManager.addView(bottomTrail, bottomTrailParams);
        windowManager.addView(leftTrail, leftTrailParams);
        windowManager.addView(rightTrail, rightTrailParams);
    }

    private WindowManager.LayoutParams edgeParams(int w, int h, int gravity) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = gravity;
        return lp;
    }

    private void makePassThrough(WindowManager.LayoutParams lp) {
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    }

    private View createEdgeView(int position) {
        View view = new View(this);
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        view.setTag(position);
        view.setOnTouchListener(new EdgeTouchListener(position));
        String[] descriptions = {"Top edge - Swipe down for volume up", "Bottom edge - Swipe up for volume down",
            "Left edge - Swipe right for back, tap for home", "Right edge - Swipe left for recents, tap for power"};
        view.setContentDescription(descriptions[position]);
        return view;
    }

    private View createTrailView(int gravity) {
        View view = new View(this);
        GradientDrawable gd = new GradientDrawable();
        int hue = settings.bubbleColorHue();
        int color = android.graphics.Color.HSVToColor(new float[]{hue, 0.6f, 0.85f});
        gd.setColor(color);
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        if (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) {
            gd.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        } else {
            gd.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        }
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
                .setContentText("Swipe screen edges for virtual controls")
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
        try { if (topEdge != null && windowManager != null) windowManager.removeView(topEdge); } catch (Exception ignored) {}
        try { if (bottomEdge != null && windowManager != null) windowManager.removeView(bottomEdge); } catch (Exception ignored) {}
        try { if (leftEdge != null && windowManager != null) windowManager.removeView(leftEdge); } catch (Exception ignored) {}
        try { if (rightEdge != null && windowManager != null) windowManager.removeView(rightEdge); } catch (Exception ignored) {}
        try { if (topTrail != null && windowManager != null) windowManager.removeView(topTrail); } catch (Exception ignored) {}
        try { if (bottomTrail != null && windowManager != null) windowManager.removeView(bottomTrail); } catch (Exception ignored) {}
        try { if (leftTrail != null && windowManager != null) windowManager.removeView(leftTrail); } catch (Exception ignored) {}
        try { if (rightTrail != null && windowManager != null) windowManager.removeView(rightTrail); } catch (Exception ignored) {}
    }

    private void haptic() {
        if (!settings.hapticFeedback()) return;
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(25);
        }
    }

    private void showTrail(View trail, float progress) {
        if (trail == null) return;
        trail.setAlpha(Math.min(0.7f, Math.abs(progress)));
    }

    private class EdgeTouchListener implements View.OnTouchListener {
        private final int position;
        private float startX, startY;
        private boolean hasMoved = false;
        private int dragDistance = 0;

        EdgeTouchListener(int position) { this.position = position; }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    hasMoved = false;
                    dragDistance = 0;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - startX;
                    float dy = event.getRawY() - startY;
                    dragDistance = (int) Math.hypot(dx, dy);
                    if (!hasMoved && dragDistance > dp(8)) hasMoved = true;
                    if (hasMoved) {
                        View trail = getTrailForPosition(position);
                        if (trail != null) {
                            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) trail.getLayoutParams();
                            if (position == 0 || position == 1) lp.height = Math.max(dp(1), Math.round(Math.abs(dy)));
                            else lp.width = Math.max(dp(1), Math.round(Math.abs(dx)));
                            windowManager.updateViewLayout(trail, lp);
                            trail.setAlpha(0.5f);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    float endX = event.getRawX();
                    float endY = event.getRawY();
                    float totalDx = endX - startX;
                    float totalDy = endY - startY;
                    handleGesture(position, totalDx, totalDy, hasMoved);
                    resetTrails();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    resetTrails();
                    return true;
            }
            return false;
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

        private void resetTrails() {
            View trail = getTrailForPosition(position);
            if (trail != null) {
                trail.animate().alpha(0).setDuration(200).start();
            }
        }

        private void handleGesture(int pos, float dx, float dy, boolean moved) {
            long now = System.currentTimeMillis();
            if (now - lastActionTime < ACTION_COOLDOWN) return;

            if (!moved || dragDistance < dp(20)) {
                handleTap(pos);
            } else {
                handleSwipe(pos, dx, dy);
            }
            lastActionTime = now;
        }

        private void handleTap(int pos) {
            haptic();
            switch (pos) {
                case 2: executeAction(ActionManager.ACTION_BUTTON_HOME); break;
                case 3: executeAction(ActionManager.ACTION_BUTTON_POWER); break;
                default: break;
            }
        }

        private void handleSwipe(int pos, float dx, float dy) {
            haptic();
            switch (pos) {
                case 0:
                    if (dy > dp(30)) executeAction(ActionManager.ACTION_VOLUME_UP);
                    else if (dy < -dp(30)) executeAction(ActionManager.ACTION_VOLUME_DOWN);
                    break;
                case 1:
                    if (dy < -dp(30)) executeAction(ActionManager.ACTION_VOLUME_UP);
                    else if (dy > dp(30)) executeAction(ActionManager.ACTION_VOLUME_DOWN);
                    break;
                case 2:
                    // Left edge supports a single, predictable inward swipe action (Back).
                    if (dx > dp(30)) executeAction(ActionManager.ACTION_BUTTON_BACK);
                    break;
                case 3:
                    // Right edge supports a single, predictable inward swipe action (Recents).
                    if (dx < -dp(30)) executeAction(ActionManager.ACTION_BUTTON_RECENTS);
                    break;
            }
        }
    }

    private void executeAction(String action) {
        switch (action) {
            case ActionManager.ACTION_VOLUME_UP:
                adjustVolume(1);
                break;
            case ActionManager.ACTION_VOLUME_DOWN:
                adjustVolume(-1);
                break;
            case ActionManager.ACTION_BUTTON_POWER:
                try { Runtime.getRuntime().exec("input keyevent KEYCODE_POWER"); } catch (Exception ignored) {}
                break;
            case ActionManager.ACTION_BUTTON_HOME:
                if (!ActionManager.performAccessibilityAction(action)) {
                    Intent homeIntent = new Intent(android.content.Intent.ACTION_MAIN);
                    homeIntent.addCategory(android.content.Intent.CATEGORY_HOME);
                    homeIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                }
                break;
            case ActionManager.ACTION_BUTTON_RECENTS:
                if (!ActionManager.performAccessibilityAction(action)) {
                    try {
                        Intent recents = new Intent("com.android.systemui.recents.TOGGLE_RECENTS");
                        recents.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(recents);
                    } catch (Exception ignored) {}
                }
                break;
            case ActionManager.ACTION_BUTTON_BACK:
                if (!ActionManager.performAccessibilityAction(action)) {
                    try { Runtime.getRuntime().exec("input keyevent 4"); } catch (Exception ignored) {}
                }
                break;
        }
    }

    private void adjustVolume(int direction) {
        if (audioManager == null) return;
        int step = Math.max(1, settings.volumeStep());
        int current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        int next = Math.max(0, Math.min(maxVolume, current + (direction * step)));
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, next, 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}