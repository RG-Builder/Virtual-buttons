package com.example.virtualbuttons;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public class ServiceWatchdog {
    private static final String TAG = "ServiceWatchdog";
    private static final String ACTION_HEARTBEAT = "com.example.virtualbuttons.WATCHDOG_HEARTBEAT";
    private static final String ACTION_RESTART = "com.example.virtualbuttons.WATCHDOG_RESTART";
    private static final int HEARTBEAT_INTERVAL_MS = 30000;
    private static final int MISSED_HEARTBEATS_BEFORE_RESTART = 3;
    private static final int MAX_RESTART_ATTEMPTS = 5;
    private static final long BACKOFF_BASE_MS = 10000;

    private static int restartAttempts = 0;
    private static long lastHeartbeat = 0;
    private static int missedBeats = 0;
    private static boolean watchdogArmed = false;
    private static Handler watchdogHandler;
    private static Runnable watchdogRunnable;

    public static void arm(Context context) {
        if (watchdogArmed) return;
        watchdogArmed = true;
        Log.i(TAG, "Watchdog armed");
        lastHeartbeat = SystemClock.elapsedRealtime();
        missedBeats = 0;

        final Handler handler = new Handler(Looper.getMainLooper());
        watchdogHandler = handler;
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                long elapsed = now - lastHeartbeat;
                if (elapsed > HEARTBEAT_INTERVAL_MS * 2) {
                    missedBeats++;
                    Log.w(TAG, "Missed heartbeat #" + missedBeats + " (elapsed: " + elapsed + "ms)");
                    if (missedBeats >= MISSED_HEARTBEATS_BEFORE_RESTART) {
                        Log.e(TAG, "Service appears dead, attempting restart");
                        attemptRestart(context);
                        missedBeats = 0;
                    }
                } else {
                    missedBeats = 0;
                }
                if (watchdogHandler != null) {
                    watchdogHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
                }
            }
        };
        handler.postDelayed(watchdogRunnable, HEARTBEAT_INTERVAL_MS);
    }

    public static void disarm() {
        watchdogArmed = false;
        if (watchdogHandler != null && watchdogRunnable != null) {
            watchdogHandler.removeCallbacks(watchdogRunnable);
        }
        Log.i(TAG, "Watchdog disarmed");
    }

    public static void heartbeat() {
        lastHeartbeat = SystemClock.elapsedRealtime();
        missedBeats = 0;
    }

    private static void attemptRestart(Context context) {
        if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
            Log.e(TAG, "Max restart attempts reached (" + MAX_RESTART_ATTEMPTS + ")");
            return;
        }

        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission missing, cannot restart");
            return;
        }

        restartAttempts++;
        long delay = BACKOFF_BASE_MS * (1L << (restartAttempts - 1));
        Log.i(TAG, "Restart attempt #" + restartAttempts + " in " + delay + "ms");

        Intent intent = new Intent(context, GestureForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void resetRestartCount() {
        restartAttempts = 0;
    }

    public static int getRestartAttempts() {
        return restartAttempts;
    }

    public static class HeartbeatReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_HEARTBEAT.equals(intent.getAction())) {
                heartbeat();
            } else if (ACTION_RESTART.equals(intent.getAction())) {
                attemptRestart(context);
            }
        }
    }
}
