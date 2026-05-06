package com.example.virtualbuttons;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;

final class AutoProfileScheduler {
    private AutoProfileScheduler() {}

    static void schedule(Context context) {
        SettingsStore settings = new SettingsStore(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 42, new Intent(context, ProfileReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (!settings.autoNightProfile()) {
            alarmManager.cancel(pendingIntent);
            return;
        }
        LocalDateTime next = LocalDateTime.now().withHour(settings.nightStartHour()).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1);
        long triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }
}
