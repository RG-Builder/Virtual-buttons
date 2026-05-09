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
            startActivityAndCollapse(ActionManager.overlaySettingsIntent(this));
            return;
        }
        boolean enabled = !settings.overlayEnabled();
        settings.setOverlayEnabled(enabled);
        if (enabled) {
            ActionManager.startFloatingService(this);
            ActionManager.showBubble(this);
            updateTile();
        } else {
            ActionManager.showBubble(this);
            Intent hide = new Intent(this, FloatingVolumeService.class).setAction(ActionManager.ACTION_HIDE_BUBBLE);
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
