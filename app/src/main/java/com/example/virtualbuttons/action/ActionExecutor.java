package com.example.virtualbuttons.action;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

public class ActionExecutor {
    private static final String TAG = "ActionExecutor";
    private final Context context;
    private final AudioManager audioManager;
    private final PowerManager powerManager;
    private CameraManager cameraManager;
    private String flashlightCameraId;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private static final float BRIGHTNESS_MIN = 0.01f;
    private static final float BRIGHTNESS_MAX = 1.0f;
    private static final float BRIGHTNESS_STEP = 0.02f;

    public interface ActionCallback {
        void onActionExecuted(String actionName);
        void onVolumeChanged(int currentVolume, int maxVolume);
        void onBrightnessChanged(float brightness);
    }

    private ActionCallback callback;

    public ActionExecutor(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

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

        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(context, "com.example.virtualbuttons.DeviceAdminReceiver");
    }

    public void setCallback(ActionCallback callback) {
        this.callback = callback;
    }

    public void volumeUp() {
        audioManager.adjustStreamVolume(
            getStreamType(), AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        notifyVolume();
    }

    public void volumeDown() {
        audioManager.adjustStreamVolume(
            getStreamType(), AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
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
        return audioManager.getStreamVolume(getStreamType());
    }

    public int getMaxVolume() {
        return audioManager.getStreamMaxVolume(getStreamType());
    }

    public int getStreamType() {
        return AudioManager.STREAM_MUSIC;
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
            int mode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                brightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            }
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
        try {
            boolean isOn = isFlashlightOn();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(flashlightCameraId, !isOn);
            }
        } catch (CameraAccessException e) {
            Log.w(TAG, "Flashlight toggle failed", e);
        }
    }

    private boolean isFlashlightOn() {
        try {
            if (cameraManager != null && flashlightCameraId != null) {
                return cameraManager.getCameraCharacteristics(flashlightCameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            }
        } catch (CameraAccessException ignored) {}
        return false;
    }

    public void takeScreenshot() {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(intent);
        SystemClock.sleep(100);
        try {
            Process process = Runtime.getRuntime().exec("screencap -p /sdcard/screenshot_" +
                System.currentTimeMillis() + ".png");
            process.waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Screenshot failed", e);
        }
    }

    public void lockScreen() {
        if (devicePolicyManager != null && adminComponent != null) {
            try {
                devicePolicyManager.lockNow();
                return;
            } catch (SecurityException ignored) {}
        }
        try {
            Process process = Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_POWER);
            process.waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Lock screen failed", e);
        }
    }

    public void goHome() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_HOME);
            process.waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Home failed", e);
        }
    }

    public void goBack() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
            process.waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Back failed", e);
        }
    }

    public void showRecentApps() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_APP_SWITCH);
            process.waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Recent apps failed", e);
        }
    }

    public void mediaPlayPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    public void mediaNext() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public void mediaPrevious() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    private void sendMediaKeyEvent(int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
        audioManager.dispatchMediaKeyEvent(down);
        audioManager.dispatchMediaKeyEvent(up);
    }

    public boolean isFlashlightAvailable() {
        return flashlightCameraId != null;
    }

    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
