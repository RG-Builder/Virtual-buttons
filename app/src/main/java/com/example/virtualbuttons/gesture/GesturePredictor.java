package com.example.virtualbuttons.gesture;

import android.content.Context;
import android.content.SharedPreferences;

public class GesturePredictor {
    private static final String PREFS_NAME = "gesture_learning";
    private static final String KEY_SUCCESS_RATE = "success_rate";
    private static final String KEY_TOTAL_ATTEMPTS = "total_attempts";
    private static final String KEY_SUCCESSFUL = "successful";
    private static final String KEY_ACCIDENTAL = "accidental";
    private static final String KEY_AVG_SWIPE_DIST = "avg_swipe_dist";
    private static final String KEY_AVG_SWIPE_VEL = "avg_swipe_vel";
    private static final String KEY_ADAPTED_SENSITIVITY = "adapted_sensitivity";
    private static final String KEY_LEARNING_ENABLED = "learning_enabled";

    private final SharedPreferences prefs;
    private float successRate = 0.8f;
    private int totalAttempts = 0;
    private int successfulGestures = 0;
    private int accidentalTriggers = 0;
    private float avgSwipeDistance = 200f;
    private float avgSwipeVelocity = 500f;
    private float adaptedSensitivity = 1.0f;
    private boolean learningEnabled = true;

    private static final int HISTORY_SIZE = 50;
    private static final float LEARNING_RATE = 0.1f;
    private static final float MIN_SENSITIVITY = 0.5f;
    private static final float MAX_SENSITIVITY = 2.0f;
    private static final float TARGET_SUCCESS_RATE = 0.85f;

    public GesturePredictor(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadState();
    }

    private void loadState() {
        successRate = prefs.getFloat(KEY_SUCCESS_RATE, 0.8f);
        totalAttempts = prefs.getInt(KEY_TOTAL_ATTEMPTS, 0);
        successfulGestures = prefs.getInt(KEY_SUCCESSFUL, 0);
        accidentalTriggers = prefs.getInt(KEY_ACCIDENTAL, 0);
        avgSwipeDistance = prefs.getFloat(KEY_AVG_SWIPE_DIST, 200f);
        avgSwipeVelocity = prefs.getFloat(KEY_AVG_SWIPE_VEL, 500f);
        adaptedSensitivity = prefs.getFloat(KEY_ADAPTED_SENSITIVITY, 1.0f);
        learningEnabled = prefs.getBoolean(KEY_LEARNING_ENABLED, true);
    }

    private void saveState() {
        prefs.edit()
            .putFloat(KEY_SUCCESS_RATE, successRate)
            .putInt(KEY_TOTAL_ATTEMPTS, totalAttempts)
            .putInt(KEY_SUCCESSFUL, successfulGestures)
            .putInt(KEY_ACCIDENTAL, accidentalTriggers)
            .putFloat(KEY_AVG_SWIPE_DIST, avgSwipeDistance)
            .putFloat(KEY_AVG_SWIPE_VEL, avgSwipeVelocity)
            .putFloat(KEY_ADAPTED_SENSITIVITY, adaptedSensitivity)
            .apply();
    }

    public void recordSuccessfulGesture(float distance, float velocity) {
        if (!learningEnabled) return;
        totalAttempts++;
        successfulGestures++;

        avgSwipeDistance = avgSwipeDistance * (1 - LEARNING_RATE) + distance * LEARNING_RATE;
        avgSwipeVelocity = avgSwipeVelocity * (1 - LEARNING_RATE) + velocity * LEARNING_RATE;

        updateSuccessRate();
        adaptSensitivity();
        saveState();
    }

    public void recordAccidentalTrigger() {
        if (!learningEnabled) return;
        totalAttempts++;
        accidentalTriggers++;

        updateSuccessRate();
        adaptSensitivity();
        saveState();
    }

    private void updateSuccessRate() {
        if (totalAttempts > 0) {
            successRate = (float) successfulGestures / totalAttempts;
        }
    }

    private void adaptSensitivity() {
        if (totalAttempts < 5) return;

        if (successRate < TARGET_SUCCESS_RATE - 0.1f) {
            adaptedSensitivity = Math.max(MIN_SENSITIVITY, adaptedSensitivity - 0.05f);
        } else if (successRate > TARGET_SUCCESS_RATE + 0.05f && totalAttempts > 20) {
            adaptedSensitivity = Math.min(MAX_SENSITIVITY, adaptedSensitivity + 0.03f);
        }
    }

    public boolean shouldAcceptGesture(float distance, float velocity, float angle) {
        if (!learningEnabled) return true;
        if (totalAttempts < 3) return true;

        float distanceRatio = distance / avgSwipeDistance;
        float velocityRatio = velocity / Math.max(1f, avgSwipeVelocity);

        float verticalBias = Math.abs((float) Math.sin(angle));
        boolean isVertical = verticalBias > 0.7f;

        if (distanceRatio < 0.4f && velocityRatio < 0.3f) return false;
        if (!isVertical && distanceRatio < 0.6f) return false;
        if (velocityRatio < 0.2f) return false;

        return true;
    }

    public float getAdaptedSensitivity() {
        return Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, adaptedSensitivity));
    }

    public void resetLearning() {
        totalAttempts = 0;
        successfulGestures = 0;
        accidentalTriggers = 0;
        successRate = 0.8f;
        avgSwipeDistance = 200f;
        avgSwipeVelocity = 500f;
        adaptedSensitivity = 1.0f;
        saveState();
    }

    public void setLearningEnabled(boolean enabled) {
        this.learningEnabled = enabled;
        prefs.edit().putBoolean(KEY_LEARNING_ENABLED, enabled).apply();
    }

    public boolean isLearningEnabled() {
        return learningEnabled;
    }

    public float getSuccessRate() {
        return successRate;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public float getAvgSwipeDistance() {
        return avgSwipeDistance;
    }

    public float getAvgSwipeVelocity() {
        return avgSwipeVelocity;
    }
}
