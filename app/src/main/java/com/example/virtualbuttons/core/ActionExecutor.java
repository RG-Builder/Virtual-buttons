package com.example.virtualbuttons.core;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

public class ActionExecutor {
    private static final String TAG = "ActionExecutor";
    private final Context context;
    private final AudioManager audioManager;
    private CameraManager cameraManager;
    private String flashlightCameraId;

    private static final float BRIGHTNESS_MIN = 0.01f;
    private static final float BRIGHTNESS_MAX = 1.0f;

    public interface ActionCallback {
        void onVolumeChanged(int currentVolume, int maxVolume);
        void onBrightnessChanged(float brightness);
    }

    private ActionCallback callback;

    public ActionExecutor(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                    Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        flashlightCameraId = id;
                        break;
                    }
                }
                if (flashlightCameraId == null && cameraManager.getCameraIdList().length > 0) {
                    flashlightCameraId = cameraManager.getCameraIdList()[0];
                }
            }
        } catch (CameraAccessException e) {
            Log.w(TAG, "Camera not available", e);
        }
    }

    public void setCallback(ActionCallback callback) {
        this.callback = callback;
    }

    public void volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        notifyVolume();
    }

    public void volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        notifyVolume();
    }

    public void toggleMute() {
        int ringer = audioManager.getRingerMode();
        if (ringer == AudioManager.RINGER_MODE_NORMAL) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else if (ringer == AudioManager.RINGER_MODE_SILENT) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } else {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    public int getCurrentVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public int getMaxVolume() {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public void notifyVolume() {
        if (callback != null) {
            callback.onVolumeChanged(getCurrentVolume(), getMaxVolume());
        }
    }

    public float getBrightness() {
        try {
            int brightness = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS);
            return brightness / 255f;
        } catch (Settings.SettingNotFoundException e) {
            return 0.5f;
        }
    }

    public void setBrightness(float brightness) {
        brightness = Math.max(BRIGHTNESS_MIN, Math.min(BRIGHTNESS_MAX, brightness));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, (int) (brightness * 255));
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                if (callback != null) callback.onBrightnessChanged(brightness);
            }
        }
    }

    public void changeBrightness(float delta) {
        float current = getBrightness();
        setBrightness(current + delta);
    }

    public void toggleFlashlight() {
        if (cameraManager == null || flashlightCameraId == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        try {
            cameraManager.setTorchMode(flashlightCameraId, !isFlashlightOn());
        } catch (CameraAccessException e) {
            Log.w(TAG, "Flashlight toggle failed", e);
        }
    }

    private boolean isFlashlightOn() {
        try {
            return cameraManager != null && flashlightCameraId != null &&
                cameraManager.getCameraCharacteristics(flashlightCameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        } catch (CameraAccessException e) {
            return false;
        }
    }

    public boolean isFlashlightAvailable() {
        return flashlightCameraId != null;
    }

    public void sendMediaKeyEvent(int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
        audioManager.dispatchMediaKeyEvent(down);
        audioManager.dispatchMediaKeyEvent(up);
    }
}
