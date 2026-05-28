package com.example.virtualbuttons;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class VolumeTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!Settings.canDrawOverlays(this)) {
            startActivityAndCollapse(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            return;
        }

        Intent serviceIntent = new Intent(this, GestureForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(Tile.STATE_ACTIVE);
        tile.setSubtitle("Gesture controls active");
        tile.updateTile();
    }
}
