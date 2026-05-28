package com.example.virtualbuttons;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticFeedbackUtil {
    private final Vibrator vibrator;
    private final int intensity; // 0-100

    public HapticFeedbackUtil(Context context, int intensity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = context.getSystemService(VibratorManager.class);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        this.intensity = intensity;
    }

    public void lightTap() {
        if (vibrator == null || intensity < 10) return;
        float amp = Math.min(1f, intensity / 100f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15,
                (int) (amp * VibrationEffect.DEFAULT_AMPLITUDE)));
        }
    }

    public void mediumTap() {
        if (vibrator == null || intensity < 10) return;
        float amp = Math.min(1f, intensity / 100f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30,
                (int) (amp * VibrationEffect.DEFAULT_AMPLITUDE)));
        }
    }

    public void heavyTap() {
        if (vibrator == null || intensity < 10) return;
        float amp = Math.min(1f, intensity / 100f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50,
                (int) (amp * VibrationEffect.DEFAULT_AMPLITUDE)));
        }
    }

    public void tick() {
        if (vibrator == null || intensity < 10) return;
        float amp = Math.min(1f, intensity / 100f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(8,
                (int) (amp * VibrationEffect.DEFAULT_AMPLITUDE)));
        }
    }

    public void doubleTap() {
        if (vibrator == null || intensity < 10) return;
        float amp = Math.min(1f, intensity / 100f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] pattern = {0, 20, 30, 20};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern,
                new int[]{(int)(amp*128), 0, (int)(amp*128)},
                -1));
        }
    }

    public static void performViewHaptic(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
    }
}
