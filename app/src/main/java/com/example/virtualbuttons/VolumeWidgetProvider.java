package com.example.virtualbuttons;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViews;

public class VolumeWidgetProvider extends AppWidgetProvider {
    private static android.content.BroadcastReceiver volumeReceiver;

    @Override public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = buildViews(ctx);
            manager.updateAppWidget(id, views);
        }
        registerVolumeReceiver(ctx, manager);
    }

    private void registerVolumeReceiver(Context ctx, AppWidgetManager manager) {
        if (volumeReceiver != null) return;
        volumeReceiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                if (ActionManager.ACTION_VOLUME_CHANGED.equals(intent.getAction())) {
                    AppWidgetManager mgr = AppWidgetManager.getInstance(c);
                    ComponentName cn = new ComponentName(c, VolumeWidgetProvider.class);
                    for (int id : mgr.getAppWidgetIds(cn)) {
                        mgr.updateAppWidget(id, buildViews(c));
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ActionManager.ACTION_VOLUME_CHANGED);
        ctx.registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private RemoteViews buildViews(Context ctx) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_volume);

        Intent toggleIntent = new Intent(ctx, FloatingVolumeService.class);
        views.setOnClickPendingIntent(R.id.widget_toggle,
            PendingIntent.getService(ctx, 10, toggleIntent.setAction(ActionManager.ACTION_TOGGLE_MUTE),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent upIntent = new Intent(ctx, FloatingVolumeService.class);
        views.setOnClickPendingIntent(R.id.widget_up,
            PendingIntent.getService(ctx, 11, upIntent.setAction(ActionManager.ACTION_VOLUME_UP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent downIntent = new Intent(ctx, FloatingVolumeService.class);
        views.setOnClickPendingIntent(R.id.widget_down,
            PendingIntent.getService(ctx, 12, downIntent.setAction(ActionManager.ACTION_VOLUME_DOWN),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent openIntent = new Intent(ctx, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_root,
            PendingIntent.getActivity(ctx, 13, openIntent, PendingIntent.FLAG_IMMUTABLE));

        return views;
    }
}
