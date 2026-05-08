package com.example.virtualbuttons;

import android.widget.SeekBar;

final class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
    interface Callback { void onProgress(int progress); }
    private final Callback onChange;
    private final Callback onCommit;

    SimpleSeekListener(Callback onChange) {
        this(onChange, onChange);
    }

    SimpleSeekListener(Callback onChange, Callback onCommit) {
        this.onChange = onChange;
        this.onCommit = onCommit;
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && onChange != null) onChange.onProgress(progress);
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override public void onStopTrackingTouch(SeekBar seekBar) {
        if (onCommit != null) onCommit.onProgress(seekBar.getProgress());
    }
}
