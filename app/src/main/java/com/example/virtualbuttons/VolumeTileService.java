package com.example.virtualbuttons;

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
        if (enabled) AppActions.startFloatingService(this);
        else stopService(new Intent(this, FloatingVolumeService.class));
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(new SettingsStore(this).overlayEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle("Tap to toggle overlay");
        tile.updateTile();
    }
}
