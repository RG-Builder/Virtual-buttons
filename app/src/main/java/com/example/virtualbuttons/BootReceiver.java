package com.example.virtualbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
            !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        Log.i(TAG, "Boot/update received: " + action);
        SettingsStore settings = new SettingsStore(context);

        if (!settings.isAutoStart()) {
            Log.i(TAG, "Auto-start disabled");
            return;
        }

        startServiceWithRetry(context, 0);
    }

    private void startServiceWithRetry(Context context, int attempt) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission not granted, cannot start");
            return;
        }

        try {
            Intent serviceIntent = new Intent(context, GestureForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.i(TAG, "Service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service (attempt " + (attempt + 1) + ")", e);
            if (attempt < MAX_RETRIES - 1) {
                final int nextAttempt = attempt + 1;
                new Handler(Looper.getMainLooper()).postDelayed(
                    () -> startServiceWithRetry(context, nextAttempt),
                    RETRY_DELAY_MS * (1L << attempt));
            }
        }
    }
}
