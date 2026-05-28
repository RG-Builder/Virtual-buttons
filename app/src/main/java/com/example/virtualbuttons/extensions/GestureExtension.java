package com.example.virtualbuttons.extensions;

import android.view.MotionEvent;

import com.example.virtualbuttons.core.GestureResult;

public interface GestureExtension {
    String getName();
    boolean isEnabled();

    void onTouchEvent(MotionEvent event);
    boolean onGestureDetected(GestureResult gesture);
    void onGestureExecuted(GestureResult gesture, boolean success);
    void reset();
}
