package com.example.virtualbuttons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.example.virtualbuttons.action.ActionExecutor;
import com.example.virtualbuttons.gesture.AppContextDetector;
import com.example.virtualbuttons.gesture.GamingModeController;
import com.example.virtualbuttons.gesture.GestureEngine;
import com.example.virtualbuttons.gesture.GesturePredictor;
import com.example.virtualbuttons.gesture.GestureZoneManager;
import com.example.virtualbuttons.overlay.BrightnessOverlayView;
import com.example.virtualbuttons.overlay.GestureIndicatorView;
import com.example.virtualbuttons.overlay.GesturePillView;
import com.example.virtualbuttons.overlay.RadialMenuView;
import com.example.virtualbuttons.overlay.VolumeOverlayView;

import java.util.ArrayList;
import java.util.List;

public class GestureForegroundService extends Service {
    private static final String CHANNEL_ID = "gesture_service_channel";
    private static final String CHANNEL_ID_STEALTH = "gesture_service_stealth";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "GestureService";
    private static final long ACC_MONITOR_INTERVAL_MS = 5000;
    private static final long CONTEXT_UPDATE_INTERVAL_MS = 300;

    private WindowManager wm;
    private GesturePillView pillView;
    private VolumeOverlayView volumeOverlay;
    private BrightnessOverlayView brightnessOverlay;
    private RadialMenuView radialMenu;
    private GestureIndicatorView indicatorView;
    private WindowManager.LayoutParams pillParams;
    private WindowManager.LayoutParams volumeParams;
    private WindowManager.LayoutParams brightnessParams;
    private WindowManager.LayoutParams radialParams;
    private WindowManager.LayoutParams indicatorParams;

    private GestureEngine gestureEngine;
    private GestureEngine pillGestureEngine;
    private ActionExecutor actionExecutor;
    private HapticFeedbackUtil haptics;
    private SettingsStore settings;
    private float density;
    private int screenWidth;
    private int screenHeight;

    private boolean isRunning;
    private float lastDragTotal;
    private int activeEdge;

    private AppContextDetector contextDetector;
    private GestureZoneManager zoneManager;
    private GesturePredictor predictor;
    private GamingModeController gamingController;

    private final List<View> overlayViews = new ArrayList<>();
    private Handler accMonitorHandler;
    private Runnable accMonitorRunnable;
    private int lastAccCheckState = -1;
    private Handler contextUpdateHandler;
    private int currentContext = AppContextDetector.CONTEXT_NORMAL;

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new SettingsStore(this);

        createNotificationChannels();
        updateNotification();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        density = getResources().getDisplayMetrics().density;
        haptics = new HapticFeedbackUtil(this, settings.getHapticIntensity());
        actionExecutor = new ActionExecutor(this);

        updateScreenSize();
        createIntelligenceSystems();
        createGestureEngines();
        createOverlayViews();
        setupGestureCallbacks();
        addAllOverlays();
        startContextUpdater();

        if (settings.isWatchdogEnabled()) {
            ServiceWatchdog.arm(this);
            ServiceWatchdog.resetRestartCount();
        }

        if (settings.isAccMonitoringEnabled()) {
            startAccMonitoring();
        }

        isRunning = true;
    }

    private void updateScreenSize() {
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    private void createIntelligenceSystems() {
        contextDetector = new AppContextDetector(screenWidth, screenHeight);
        zoneManager = new GestureZoneManager(screenWidth, screenHeight, density);
        zoneManager.setBaseEdgeWidth(settings.getEdgeWidth());
        predictor = new GesturePredictor(this);
        predictor.setLearningEnabled(settings.isAdaptiveLearningEnabled());
        gamingController = new GamingModeController();
        int gamingMode = settings.getGamingModeValue();
        gamingController.setMode(gamingMode == 0 ? GamingModeController.MODE_OFF :
            gamingMode == 2 ? GamingModeController.MODE_ON : GamingModeController.MODE_AUTO);
    }

    private void createGestureEngines() {
        gestureEngine = new GestureEngine(screenWidth, screenHeight, density);
        gestureEngine.setSensitivity(settings.getGestureSensitivity() / 50f);
        gestureEngine.setEdgeWidth(settings.getEdgeWidth(), density);
        gestureEngine.setCooldownMs(settings.getCooldownMs());
        gestureEngine.setContextDetector(contextDetector);
        gestureEngine.setPredictor(predictor);
        gestureEngine.setZoneManager(zoneManager);
        gestureEngine.setAntiAccidentalEnabled(settings.isAntiAccidentalEnabled());

        pillGestureEngine = new GestureEngine(screenWidth, screenHeight, density);
        pillGestureEngine.setSensitivity(settings.getGestureSensitivity() / 50f);
        pillGestureEngine.setEdgeWidth(settings.getEdgeWidth(), density);
        pillGestureEngine.setCooldownMs(settings.getCooldownMs());
        pillGestureEngine.setAntiAccidentalEnabled(settings.isAntiAccidentalEnabled());
        pillGestureEngine.setContextDetector(contextDetector);
        pillGestureEngine.setPredictor(predictor);
    }

    private void startContextUpdater() {
        contextUpdateHandler = new Handler(Looper.getMainLooper());
        contextUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int newContext = contextDetector.getCurrentContext();
                if (newContext != currentContext) {
                    currentContext = newContext;
                    onContextChanged(newContext);
                }
                float multiplier = contextDetector.getActiveSensitivityMultiplier();
                int minSwipe = contextDetector.getMinSwipeDistanceMultiplier();
                gestureEngine.updateContextParams(multiplier, minSwipe);

                if (settings.isEdgeShrinkFullscreenEnabled()) {
                    zoneManager.setActiveContext(newContext);
                }

                if (settings.isPillAutoHideGamingEnabled() && gamingController.isActive()) {
                    if (pillView.getVisibility() == View.VISIBLE) {
                        pillView.animate().alpha(0.3f).setDuration(300);
                    }
                } else if (pillView.getAlpha() < 1f) {
                    pillView.animate().alpha(1f).setDuration(300);
                }

                if (contextUpdateHandler != null) {
                    contextUpdateHandler.postDelayed(this, CONTEXT_UPDATE_INTERVAL_MS);
                }
            }
        }, CONTEXT_UPDATE_INTERVAL_MS);
    }

    private void stopContextUpdater() {
        if (contextUpdateHandler != null) {
            contextUpdateHandler.removeCallbacksAndMessages(null);
        }
    }

    private void onContextChanged(int newContext) {
        if (newContext == AppContextDetector.CONTEXT_GAMING && settings.isGamingModeEnabled()) {
            zoneManager.setActiveContext(AppContextDetector.CONTEXT_GAMING);
            if (settings.isPillAutoHideGamingEnabled()) {
                pillView.animate().alpha(0.2f).setDuration(200);
            }
        } else if (newContext == AppContextDetector.CONTEXT_NORMAL) {
            zoneManager.setActiveContext(AppContextDetector.CONTEXT_NORMAL);
            pillView.animate().alpha(1f).setDuration(200);
        }
    }

    private void createOverlayViews() {
        pillView = new GesturePillView(this,
            settings.getPillColor(),
            settings.getPillOpacity(),
            settings.getPillSize(),
            density);
        pillView.setHaptics(haptics);
        pillView.setGestureEngine(pillGestureEngine);
        pillView.setPillTouchListener(event -> {
            if (gamingController != null) {
                gamingController.onTouchEvent(event);
            }
        });

        volumeOverlay = new VolumeOverlayView(this, density);
        brightnessOverlay = new BrightnessOverlayView(this, density);
        radialMenu = new RadialMenuView(this, density);
        indicatorView = new GestureIndicatorView(this);

        int pillW = pillView.getPillWidth();
        int pillH = pillView.getPillHeight();

        pillParams = new WindowManager.LayoutParams(
            pillW, pillH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        pillParams.gravity = Gravity.TOP | Gravity.START;
        pillParams.x = (int) (12 * density);
        pillParams.y = screenHeight / 2 - pillH / 2;
        pillView.setWindowParams(pillParams);
        pillView.setWindowManager(wm);

        int overlayW = (int) (56 * density);
        int overlayH = (int) (220 * density);

        volumeParams = new WindowManager.LayoutParams(
            overlayW, overlayH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        volumeParams.gravity = Gravity.TOP | Gravity.START;
        volumeOverlay.setWindowManager(wm);
        volumeOverlay.setWindowParams(volumeParams);

        brightnessParams = new WindowManager.LayoutParams(
            overlayW, overlayH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        brightnessParams.gravity = Gravity.TOP | Gravity.START;
        brightnessOverlay.setWindowManager(wm);
        brightnessOverlay.setWindowParams(brightnessParams);

        int radialSize = (int) ((80 + 22 + 20) * 2 * density);
        radialParams = new WindowManager.LayoutParams(
            radialSize, radialSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        radialParams.gravity = Gravity.TOP | Gravity.START;

        indicatorParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        indicatorParams.gravity = Gravity.TOP | Gravity.START;
    }

    private void addAllOverlays() {
        try {
            wm.addView(pillView, pillParams);
            overlayViews.add(pillView);

            wm.addView(volumeOverlay, volumeParams);
            overlayViews.add(volumeOverlay);
            volumeOverlay.setVisibility(View.GONE);

            wm.addView(brightnessOverlay, brightnessParams);
            overlayViews.add(brightnessOverlay);
            brightnessOverlay.setVisibility(View.GONE);

            wm.addView(radialMenu, radialParams);
            overlayViews.add(radialMenu);
            radialMenu.setVisibility(View.GONE);

            wm.addView(indicatorView, indicatorParams);
            overlayViews.add(indicatorView);
            indicatorView.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAccMonitoring() {
        accMonitorHandler = new Handler(Looper.getMainLooper());
        accMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
                boolean available = acc != null;
                int state = available ? 1 : 0;

                if (state != lastAccCheckState) {
                    lastAccCheckState = state;
                    if (!available && lastAccCheckState == 0) {
                    }
                }
                if (accMonitorHandler != null) {
                    accMonitorHandler.postDelayed(this, ACC_MONITOR_INTERVAL_MS);
                }
            }
        };
        accMonitorHandler.postDelayed(accMonitorRunnable, ACC_MONITOR_INTERVAL_MS);
    }

    private void stopAccMonitoring() {
        if (accMonitorHandler != null && accMonitorRunnable != null) {
            accMonitorHandler.removeCallbacks(accMonitorRunnable);
        }
    }

    private void setupGestureCallbacks() {
        gestureEngine.setListener(new GestureEngine.GestureListener() {
            @Override
            public void onGestureDetected(int gestureType, int edge, float x, float y, float velocity) {
                ServiceWatchdog.heartbeat();
                handleEdgeGesture(gestureType, edge, x, y, velocity);
            }

            @Override
            public void onDragProgress(int edge, float delta, float total, float velocity) {
                ServiceWatchdog.heartbeat();
                handleEdgeDrag(edge, delta, total, velocity);
            }

            @Override
            public void onDragEnd(int edge, float total, float velocity) {
                handleEdgeDragEnd(edge, total, velocity);
            }

            @Override
            public void onTap(float x, float y) {
                ServiceWatchdog.heartbeat();
            }

            @Override
            public void onDoubleTap(float x, float y) {
                ServiceWatchdog.heartbeat();
                if (settings.isDoubleTapLockEnabled()) {
                    haptics.heavyTap();
                    GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
                    if (acc != null) acc.performLockScreen();
                }
            }

            @Override
            public void onLongPress(float x, float y) {
                if (settings.isRadialMenuEnabled()) {
                    haptics.mediumTap();
                    showRadialMenu(x, y);
                }
            }
        });

        pillGestureEngine.setListener(new GestureEngine.GestureListener() {
            @Override
            public void onGestureDetected(int gestureType, int edge, float x, float y, float velocity) {
                ServiceWatchdog.heartbeat();
                handlePillGesture(gestureType, edge, x, y, velocity);
            }

            @Override
            public void onDragProgress(int edge, float delta, float total, float velocity) {}

            @Override
            public void onDragEnd(int edge, float total, float velocity) {}

            @Override
            public void onTap(float x, float y) {
                ServiceWatchdog.heartbeat();
                haptics.lightTap();
                showEdgeIndicator();
            }

            @Override
            public void onDoubleTap(float x, float y) {
                ServiceWatchdog.heartbeat();
                if (settings.isDoubleTapLockEnabled()) {
                    haptics.heavyTap();
                    GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
                    if (acc != null) acc.performLockScreen();
                }
            }

            @Override
            public void onLongPress(float x, float y) {
                if (settings.isRadialMenuEnabled()) {
                    haptics.mediumTap();
                    showRadialMenu(pillParams.x + pillView.getPillWidth() / 2f,
                        pillParams.y + pillView.getPillHeight() / 2f);
                }
            }
        });
    }

    private void handleEdgeGesture(int gestureType, int edge, float x, float y, float velocity) {
        boolean isLeft = (edge & GestureEngine.EDGE_LEFT) != 0;
        boolean isRight = (edge & GestureEngine.EDGE_RIGHT) != 0;
        boolean isCorner = (edge & (GestureEngine.CORNER_TOP_LEFT | GestureEngine.CORNER_TOP_RIGHT
            | GestureEngine.CORNER_BOTTOM_LEFT | GestureEngine.CORNER_BOTTOM_RIGHT)) != 0;

        if (isCorner && settings.isCornerGesturesEnabled()) {
            handleCornerGesture(edge);
            return;
        }

        if ((isLeft || isRight) && settings.isEdgeGesturesEnabled()) {
            if (gestureType == GestureEngine.GESTURE_SWIPE_UP) {
                haptics.tick();
                actionExecutor.volumeUp();
                showVolumeOverlay((int) x, (int) y, isLeft ? 1 : 2);
            } else if (gestureType == GestureEngine.GESTURE_SWIPE_DOWN) {
                haptics.tick();
                actionExecutor.volumeDown();
                showVolumeOverlay((int) x, (int) y, isLeft ? 1 : 2);
            }
        }
    }

    private void handleCornerGesture(int edge) {
        haptics.heavyTap();
        if ((edge & GestureEngine.CORNER_TOP_LEFT) != 0) {
            actionExecutor.toggleFlashlight();
        } else if ((edge & GestureEngine.CORNER_TOP_RIGHT) != 0) {
            GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
            if (acc != null) acc.performScreenshot();
        } else if ((edge & GestureEngine.CORNER_BOTTOM_LEFT) != 0) {
            actionExecutor.toggleMute();
        } else if ((edge & GestureEngine.CORNER_BOTTOM_RIGHT) != 0) {
            GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
            if (acc != null) acc.performQuickSettings();
        }
    }

    private void handleEdgeDrag(int edge, float delta, float total, float velocity) {
        boolean isLeft = (edge & GestureEngine.EDGE_LEFT) != 0;
        boolean isRight = (edge & GestureEngine.EDGE_RIGHT) != 0;

        if (isLeft || isRight) {
            activeEdge = isLeft ? 1 : 2;
            lastDragTotal = total;

            float dragSensitivity = 0.005f;
            float brightnessDelta = delta * dragSensitivity;
            actionExecutor.changeBrightness(brightnessDelta);

            haptics.tick();
            showBrightnessOverlay((int) (isLeft ? 12 * density : screenWidth - 12 * density),
                (int) (actionExecutor.getBrightness() * screenHeight));
        } else if ((edge & GestureEngine.EDGE_TOP) != 0) {
            activeEdge = 3;
            lastDragTotal = total;
            float volDelta = delta > 0 ? -1 : 1;
            if (Math.abs(delta) > 20) {
                if (volDelta < 0) actionExecutor.volumeDown();
                else actionExecutor.volumeUp();
                actionExecutor.notifyVolume();
            }
        } else if ((edge & GestureEngine.EDGE_BOTTOM) != 0) {
            activeEdge = 4;
            lastDragTotal = total;
        }
    }

    private void handleEdgeDragEnd(int edge, float total, float velocity) {
        haptics.mediumTap();
    }

    private void handlePillGesture(int gestureType, int edge, float x, float y, float velocity) {
        if (gestureType == GestureEngine.GESTURE_SWIPE_UP) {
            haptics.tick();
            actionExecutor.volumeUp();
            showVolumeOverlay(pillParams.x + pillView.getPillWidth(),
                pillParams.y + pillView.getPillHeight() / 2, 1);
        } else if (gestureType == GestureEngine.GESTURE_SWIPE_DOWN) {
            haptics.tick();
            actionExecutor.volumeDown();
            showVolumeOverlay(pillParams.x + pillView.getPillWidth(),
                pillParams.y + pillView.getPillHeight() / 2, 1);
        }
    }

    private void showVolumeOverlay(int x, int y, int edge) {
        if (volumeOverlay == null) return;
        int vol = actionExecutor.getCurrentVolume();
        int max = actionExecutor.getMaxVolume();
        volumeOverlay.show(vol, max, x, y, edge);
    }

    private void showBrightnessOverlay(int x, int y) {
        if (brightnessOverlay == null) return;
        float b = actionExecutor.getBrightness();
        int edge = activeEdge;
        brightnessOverlay.show(b, x, y, edge);
    }

    private void showRadialMenu(float x, float y) {
        if (radialMenu == null) return;
        radialMenu.setListener(new RadialMenuView.RadialMenuListener() {
            @Override
            public void onItemSelected(int index) {
                haptics.heavyTap();
                switch (index) {
                    case 0: actionExecutor.volumeUp(); break;
                    case 1: actionExecutor.setBrightness(0.8f); break;
                    case 2: actionExecutor.toggleFlashlight(); break;
                    case 3: actionExecutor.toggleMute(); break;
                    case 4:
                        GestureAccessibilityService acc = GestureAccessibilityService.getInstance();
                        if (acc != null) acc.performScreenshot();
                        break;
                    case 5:
                        if (settings.isDoubleTapLockEnabled()) {
                            GestureAccessibilityService acc2 = GestureAccessibilityService.getInstance();
                            if (acc2 != null) acc2.performLockScreen();
                        }
                        break;
                }
            }
            @Override public void onMenuDismissed() {}
        });
        radialMenu.setVisibility(View.VISIBLE);
        radialParams.x = (int) (x - radialMenu.getMeasuredWidth() / 2f);
        radialParams.y = (int) (y - radialMenu.getMeasuredHeight() / 2f);
        try {
            wm.updateViewLayout(radialMenu, radialParams);
        } catch (Exception ignored) {}
        radialMenu.show(x, y);
    }

    private void showEdgeIndicator() {
        if (indicatorView == null || !settings.isShowIndicators()) return;
        int flags = GestureIndicatorView.EDGE_LEFT | GestureIndicatorView.EDGE_RIGHT;
        indicatorView.show(flags);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (settings.isServiceProtectionEnabled()) {
            return START_STICKY_COMPATIBILITY;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        ServiceWatchdog.disarm();
        stopAccMonitoring();
        stopContextUpdater();

        for (View v : overlayViews) {
            try {
                wm.removeView(v);
            } catch (Exception ignored) {}
        }
        overlayViews.clear();

        if (settings.isAutoRestartEnabled()) {
            scheduleRestart();
        }

        super.onDestroy();
    }

    private void scheduleRestart() {
        Intent intent = new Intent(this, GestureForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel normal = new NotificationChannel(
                CHANNEL_ID, "Gesture Controls",
                NotificationManager.IMPORTANCE_LOW);
            normal.setDescription("Shows when gesture controls are active");
            normal.setShowBadge(false);
            normal.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            normal.enableVibration(false);
            normal.setSound(null, null);
            nm.createNotificationChannel(normal);

            NotificationChannel stealth = new NotificationChannel(
                CHANNEL_ID_STEALTH, "Gesture Controls (Stealth)",
                NotificationManager.IMPORTANCE_MIN);
            stealth.setDescription("Minimal notification for background service");
            stealth.setShowBadge(false);
            stealth.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            stealth.enableVibration(false);
            stealth.setSound(null, null);
            nm.createNotificationChannel(stealth);
        }
    }

    private void updateNotification() {
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        int mode = settings.getNotificationMode();
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = (mode == SettingsStore.NOTIF_MODE_STEALTH)
                ? CHANNEL_ID_STEALTH : CHANNEL_ID;
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        switch (mode) {
            case SettingsStore.NOTIF_MODE_STEALTH:
                builder.setContentTitle(" ")
                    .setContentText(" ")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setShowWhen(false);
                break;

            case SettingsStore.NOTIF_MODE_MINIMAL:
                builder.setContentTitle("Gesture Controls")
                    .setContentText("Active")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setShowWhen(false);
                break;

            default:
                builder.setContentTitle("Gesture Controls Active")
                    .setContentText("Swipe edges for volume, brightness, and more")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setShowWhen(false);
                break;
        }

        return builder.build();
    }

}
