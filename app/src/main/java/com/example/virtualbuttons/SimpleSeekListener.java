package com.example.virtualbuttons;

import android.widget.SeekBar;

final class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
    interface Callback { void onProgress(int progress); }
    private final Callback callback;
    SimpleSeekListener(Callback callback) { this.callback = callback; }
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) callback.onProgress(progress); }
    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) { callback.onProgress(seekBar.getProgress()); }
}
