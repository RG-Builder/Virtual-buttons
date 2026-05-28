package com.example.virtualbuttons;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.RemoteViews;

public class VolumeWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_VOLUME_UP = "com.example.virtualbuttons.WIDGET_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "com.example.virtualbuttons.WIDGET_VOLUME_DOWN";
    public static final String ACTION_TOGGLE_MUTE = "com.example.virtualbuttons.WIDGET_TOGGLE_MUTE";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = buildViews(ctx);
            manager.updateAppWidget(id, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return;

        String action = intent.getAction();
        if (ACTION_VOLUME_UP.equals(action)) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        } else if (ACTION_VOLUME_DOWN.equals(action)) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        } else if (ACTION_TOGGLE_MUTE.equals(action)) {
            int ringer = audio.getRingerMode();
            if (ringer == AudioManager.RINGER_MODE_NORMAL) {
                audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }
    }

    private RemoteViews buildViews(Context ctx) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_volume);

        Intent toggleIntent = new Intent(ctx, VolumeWidgetProvider.class);
        toggleIntent.setAction(ACTION_TOGGLE_MUTE);
        views.setOnClickPendingIntent(R.id.widget_toggle,
            PendingIntent.getBroadcast(ctx, 10, toggleIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent upIntent = new Intent(ctx, VolumeWidgetProvider.class);
        upIntent.setAction(ACTION_VOLUME_UP);
        views.setOnClickPendingIntent(R.id.widget_up,
            PendingIntent.getBroadcast(ctx, 11, upIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent downIntent = new Intent(ctx, VolumeWidgetProvider.class);
        downIntent.setAction(ACTION_VOLUME_DOWN);
        views.setOnClickPendingIntent(R.id.widget_down,
            PendingIntent.getBroadcast(ctx, 12, downIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent openIntent = new Intent(ctx, ModernMainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_root,
            PendingIntent.getActivity(ctx, 13, openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return views;
    }
}
