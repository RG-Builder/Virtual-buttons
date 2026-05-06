package com.example.virtualbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SettingsStore settings = new SettingsStore(context);
        AutoProfileScheduler.schedule(context);
        if (settings.startOnBoot() && settings.overlayEnabled() && Settings.canDrawOverlays(context)) {
            AppActions.startFloatingService(context);
        }
    }
}
