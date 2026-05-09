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
        if (settings.backgroundRunning()) {
            if (settings.overlayEnabled()) {
                Intent hide = new Intent(this, FloatingVolumeService.class).setAction(ActionManager.ACTION_HIDE_BUBBLE);
                startService(hide);
            } else {
                settings.setOverlayEnabled(true);
                ActionManager.startFloatingService(this);
            }
            updateTile();
            return;
        }
        boolean enabled = !settings.overlayEnabled();
        settings.setOverlayEnabled(enabled);
        if (enabled) {
            ActionManager.startFloatingService(this);
            ActionManager.showBubble(this);
            updateTile();
        } else {
            if (settings.backgroundRunning()) settings.setBackgroundRunning(false);
            settings.setOverlayEnabled(false);
            Intent stop = new Intent(this, FloatingVolumeService.class).setAction(ActionManager.ACTION_STOP);
            startService(stop);
            updateTile();
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        SettingsStore settings = new SettingsStore(this);
        boolean bg = settings.backgroundRunning();
        boolean active = settings.overlayEnabled();
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (bg) {
            tile.setSubtitle(active ? "Background" : "Tap to show");
        } else {
            tile.setSubtitle(active ? "Active" : "Tap to start");
        }
        tile.updateTile();
    }
}
