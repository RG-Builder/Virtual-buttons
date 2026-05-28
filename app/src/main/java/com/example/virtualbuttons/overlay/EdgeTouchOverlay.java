package com.example.virtualbuttons.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.example.virtualbuttons.core.SimpleGestureEngine;

public class EdgeTouchOverlay extends View {
    private SimpleGestureEngine gestureEngine;
    private final boolean isLeftEdge;

    public EdgeTouchOverlay(Context context, boolean isLeftEdge) {
        super(context);
        this.isLeftEdge = isLeftEdge;
    }

    public void setGestureEngine(SimpleGestureEngine engine) {
        this.gestureEngine = engine;
    }

    public WindowManager.LayoutParams createLayoutParams(int edgeWidthPx) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        return new WindowManager.LayoutParams(
            edgeWidthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT);
    }

    public void setLayoutGravity(WindowManager.LayoutParams params) {
        params.gravity = isLeftEdge
            ? Gravity.LEFT | Gravity.FILL_VERTICAL
            : Gravity.RIGHT | Gravity.FILL_VERTICAL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureEngine != null) {
            gestureEngine.onTouchEvent(event);
        }
        return true;
    }
}
