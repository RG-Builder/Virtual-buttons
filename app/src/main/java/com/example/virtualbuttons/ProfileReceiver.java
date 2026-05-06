package com.example.virtualbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ProfileReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SettingsStore settings = new SettingsStore(context);
        if (!settings.autoNightProfile()) return;
        new VolumeController(context, settings).applyNightProfile();
        AutoProfileScheduler.schedule(context);
    }
}
