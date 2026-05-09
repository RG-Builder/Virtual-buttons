package com.example.virtualbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SettingsStore settings = new SettingsStore(context);
        AutoProfileScheduler.schedule(context);
        if (!Settings.canDrawOverlays(context)) return;
        if (settings.backgroundRunning()) {
            if (!settings.overlayEnabled()) settings.setOverlayEnabled(true);
            ActionManager.startFloatingService(context);
        } else if (settings.startOnBoot() && settings.overlayEnabled()) {
            ActionManager.startFloatingService(context);
        }
    }
}
