package com.example.virtualbuttons;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class ActionManager {
    static final String CHANNEL_ID = "virtual_buttons_controls";
    static final String ACTION_VOLUME_UP = "com.example.virtualbuttons.ACTION_VOLUME_UP";
    static final String ACTION_VOLUME_DOWN = "com.example.virtualbuttons.ACTION_VOLUME_DOWN";
    static final String ACTION_TOGGLE_MUTE = "com.example.virtualbuttons.ACTION_TOGGLE_MUTE";
    static final String ACTION_STOP = "com.example.virtualbuttons.ACTION_STOP";
    static final String ACTION_HIDE_BUBBLE = "com.example.virtualbuttons.ACTION_HIDE_BUBBLE";
    static final String ACTION_SHOW_BUBBLE = "com.example.virtualbuttons.ACTION_SHOW_BUBBLE";
    static final String ACTION_REFRESH = "com.example.virtualbuttons.ACTION_REFRESH";
    static final String ACTION_RESTORE_DAY_PROFILE = "com.example.virtualbuttons.ACTION_RESTORE_DAY_PROFILE";
    static final String ACTION_SHOW_BUBBLE_PERMANENT = "com.example.virtualbuttons.ACTION_SHOW_BUBBLE_PERMANENT";
    static final String ACTION_APPLY_NIGHT_PROFILE = "com.example.virtualbuttons.ACTION_APPLY_NIGHT_PROFILE";
    static final String ACTION_VOLUME_CHANGED = "com.example.virtualbuttons.ACTION_VOLUME_CHANGED";

    private ActionManager() {}

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Volume controls", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Persistent controls for the virtual volume button");
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    static Intent overlaySettingsIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
    }

    static Intent exactAlarmIntent(Context context) {
        return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.getPackageName()));
    }

    static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    static void startFloatingService(Context context) {
        Intent intent = new Intent(context, FloatingVolumeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    static void showBubble(Context context) {
        Intent intent = new Intent(context, FloatingVolumeService.class).setAction(ACTION_SHOW_BUBBLE);
        context.startService(intent);
    }

    static void refreshService(Context context) {
        Intent intent = new Intent(context, FloatingVolumeService.class).setAction(ACTION_REFRESH);
        context.startService(intent);
    }
}
