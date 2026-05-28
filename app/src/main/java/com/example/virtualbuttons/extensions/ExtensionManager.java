package com.example.virtualbuttons.extensions;

import android.util.Log;
import android.view.MotionEvent;

import com.example.virtualbuttons.core.GestureResult;

import java.util.ArrayList;
import java.util.List;

public class ExtensionManager {
    private static final String TAG = "ExtensionManager";
    private final List<GestureExtension> extensions = new ArrayList<>();

    public void register(GestureExtension extension) {
        if (extension != null && !extensions.contains(extension)) {
            extensions.add(extension);
            Log.d(TAG, "Registered extension: " + extension.getName());
        }
    }

    public void unregister(GestureExtension extension) {
        extensions.remove(extension);
    }

    public void unregisterAll() {
        extensions.clear();
    }

    public void onTouchEvent(MotionEvent event) {
        for (int i = extensions.size() - 1; i >= 0; i--) {
            try {
                extensions.get(i).onTouchEvent(event);
            } catch (Exception e) {
                Log.w(TAG, "Extension " + extensions.get(i).getName() + " onTouchEvent failed", e);
            }
        }
    }

    public boolean onGestureDetected(GestureResult gesture) {
        for (int i = extensions.size() - 1; i >= 0; i--) {
            try {
                if (extensions.get(i).onGestureDetected(gesture)) {
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Extension " + extensions.get(i).getName() + " onGestureDetected failed", e);
            }
        }
        return false;
    }

    public void onGestureExecuted(GestureResult gesture, boolean success) {
        for (int i = extensions.size() - 1; i >= 0; i--) {
            try {
                extensions.get(i).onGestureExecuted(gesture, success);
            } catch (Exception e) {
                Log.w(TAG, "Extension " + extensions.get(i).getName() + " onGestureExecuted failed", e);
            }
        }
    }

    public void reset() {
        for (GestureExtension ext : extensions) {
            try {
                ext.reset();
            } catch (Exception e) {
                Log.w(TAG, "Extension " + ext.getName() + " reset failed", e);
            }
        }
    }

    public int getExtensionCount() {
        return extensions.size();
    }
}
