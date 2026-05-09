package com.example.virtualbuttons;

import android.content.Context;
import android.media.AudioManager;

final class VolumeController {
    private final AudioManager audioManager;
    private final SettingsStore settings;

    VolumeController(Context context, SettingsStore settings) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.settings = settings;
    }

    int resolveStream() {
        SettingsStore.StreamMode mode = settings.streamMode();
        if (mode == SettingsStore.StreamMode.MEDIA) return AudioManager.STREAM_MUSIC;
        if (mode == SettingsStore.StreamMode.SYSTEM) return AudioManager.STREAM_SYSTEM;
        return audioManager != null && audioManager.isMusicActive() ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_SYSTEM;
    }

    VolumeState changeBySteps(int direction) {
        int stream = resolveStream();
        int steps = Math.max(1, settings.volumeStep()) * (direction < 0 ? -1 : 1);
        int current = audioManager.getStreamVolume(stream);
        int next = clamp(current + steps, 0, audioManager.getStreamMaxVolume(stream));
        audioManager.setStreamVolume(stream, next, 0);
        return state(stream);
    }

    VolumeState muteOrRestoreMedia() {
        int stream = AudioManager.STREAM_MUSIC;
        int current = audioManager.getStreamVolume(stream);
        if (current > 0) {
            settings.setLastAudibleMedia(current);
            audioManager.setStreamVolume(stream, 0, 0);
        } else {
            int restore = settings.lastAudibleMedia() > 0 ? settings.lastAudibleMedia() : Math.max(1, audioManager.getStreamMaxVolume(stream) / 2);
            audioManager.setStreamVolume(stream, restore, 0);
        }
        return state(stream);
    }

    VolumeState applyNightProfile() {
        int stream = AudioManager.STREAM_MUSIC;
        int max = audioManager.getStreamMaxVolume(stream);
        int target = clamp(Math.round(max * settings.nightVolumePercent() / 100f), 0, max);
        settings.setPreNightVolume(audioManager.getStreamVolume(stream));
        audioManager.setStreamVolume(stream, target, 0);
        return state(stream);
    }

    VolumeState restoreDayProfile() {
        int stream = AudioManager.STREAM_MUSIC;
        int preNight = settings.preNightVolume();
        int current = audioManager.getStreamVolume(stream);
        int restore = preNight >= 0 ? preNight : current;
        audioManager.setStreamVolume(stream, restore, 0);
        return state(stream);
    }

    VolumeState state(int stream) {
        int max = audioManager.getStreamMaxVolume(stream);
        int value = audioManager.getStreamVolume(stream);
        return new VolumeState(stream, value, max);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    static final class VolumeState {
        final int stream;
        final int value;
        final int max;
        VolumeState(int stream, int value, int max) { this.stream = stream; this.value = value; this.max = max; }
        int percent() { return max == 0 ? 0 : Math.round(value * 100f / max); }
        boolean isMuted() { return value == 0; }
    }
}
