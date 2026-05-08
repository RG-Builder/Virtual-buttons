package com.example.virtualbuttons;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class VolumeTileService extends TileService {
    @Override public void onStartListening() { super.onStartListening(); updateTile(); }

    @Override public void onClick() {
        super.onClick();
        SettingsStore settings = new SettingsStore(this);
        if (!Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startActivityAndCollapse(AppActions.overlaySettingsIntent(this));
            else startActivityAndCollapse(AppActions.overlaySettingsIntent(this));
            return;
        }
        boolean enabled = !settings.overlayEnabled();
        settings.setOverlayEnabled(enabled);
        if (enabled) {
            AppActions.startFloatingService(this);
            AppActions.showBubble(this);
            updateTile();
        } else {
            AppActions.showBubble(this);
            Intent hide = new Intent(this, FloatingVolumeService.class).setAction(AppActions.ACTION_HIDE_BUBBLE);
            startService(hide);
            updateTile();
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean active = new SettingsStore(this).overlayEnabled();
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(active ? "Active" : "Tap to start");
        tile.updateTile();
    }
}
