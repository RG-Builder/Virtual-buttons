package com.example.virtualbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ProfileReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : (intent.getAction() == null ? "" : intent.getAction());
        SettingsStore settings = new SettingsStore(context);

        if (ActionManager.ACTION_APPLY_NIGHT_PROFILE.equals(action)) {
            if (settings.autoNightProfile()) {
                new VolumeController(context, settings).applyNightProfile();
            }
        } else if (ActionManager.ACTION_RESTORE_DAY_PROFILE.equals(action)) {
            new VolumeController(context, settings).restoreDayProfile();
        } else {
            if (!settings.autoNightProfile()) return;
            new VolumeController(context, settings).applyNightProfile();
        }
        AutoProfileScheduler.schedule(context);
    }
}
