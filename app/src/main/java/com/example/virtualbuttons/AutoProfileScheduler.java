package com.example.virtualbuttons;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;

final class AutoProfileScheduler {
    private static final int NIGHT_START_REQUEST = 42;
    private static final int NIGHT_END_REQUEST = 43;

    private AutoProfileScheduler() {}

    static void schedule(Context context) {
        SettingsStore settings = new SettingsStore(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(PendingIntent.getBroadcast(context, NIGHT_START_REQUEST,
            new Intent(context, ProfileReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        alarmManager.cancel(PendingIntent.getBroadcast(context, NIGHT_END_REQUEST,
            new Intent(context, ProfileReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        if (!settings.autoNightProfile()) return;

        LocalDateTime now = LocalDateTime.now();
        int startHour = settings.nightStartHour();
        int endHour = settings.nightEndHour();

        LocalDateTime nextStart = now.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        if (!nextStart.isAfter(now)) nextStart = nextStart.plusDays(1);

        LocalDateTime nextEnd = now.withHour(endHour).withMinute(0).withSecond(0).withNano(0);
        if (!nextEnd.isAfter(now)) nextEnd = nextEnd.plusDays(1);

        PendingIntent nightStartIntent = PendingIntent.getBroadcast(context, NIGHT_START_REQUEST,
            new Intent(context, ProfileReceiver.class).setAction(ActionManager.ACTION_APPLY_NIGHT_PROFILE),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nightEndIntent = PendingIntent.getBroadcast(context, NIGHT_END_REQUEST,
            new Intent(context, ProfileReceiver.class).setAction(ActionManager.ACTION_RESTORE_DAY_PROFILE),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        scheduleAlarm(alarmManager, nextStart, nightStartIntent);
        scheduleAlarm(alarmManager, nextEnd, nightEndIntent);
    }

    private static void scheduleAlarm(AlarmManager alarmManager, LocalDateTime time, PendingIntent pendingIntent) {
        long triggerAt = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }
}
