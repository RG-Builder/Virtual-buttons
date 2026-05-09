package com.example.virtualbuttons;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class VolumeWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            android.widget.RemoteViews views = new android.widget.RemoteViews(ctx.getPackageName(), R.layout.widget_volume);

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

            manager.updateAppWidget(id, views);
        }
    }
}
