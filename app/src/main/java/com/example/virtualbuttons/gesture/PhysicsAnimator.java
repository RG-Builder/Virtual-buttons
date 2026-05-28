package com.example.virtualbuttons.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class PhysicsAnimator {
    private final float density;

    public PhysicsAnimator(float density) {
        this.density = density;
    }

    public static SpringAnimation createSpring(View view, DynamicAnimation.ViewProperty property,
                                                float finalPosition, float stiffness, float dampingRatio) {
        SpringAnimation anim = new SpringAnimation(view, property, finalPosition);
        SpringForce force = new SpringForce(finalPosition);
        force.setStiffness(stiffness);
        force.setDampingRatio(dampingRatio);
        anim.setSpring(force);
        anim.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
        return anim;
    }

    public static SpringAnimation createSnapSpring(View view, DynamicAnimation.ViewProperty property,
                                                    float finalPosition) {
        return createSpring(view, property, finalPosition,
            SpringForce.STIFFNESS_MEDIUM, 0.6f);
    }

    public static FlingAnimation createFling(View view, DynamicAnimation.ViewProperty property,
                                              float velocity, float minValue, float maxValue,
                                              float friction) {
        FlingAnimation anim = new FlingAnimation(view, property);
        anim.setStartVelocity(velocity);
        anim.setFriction(friction);
        anim.setMinValue(minValue);
        anim.setMaxValue(maxValue);
        return anim;
    }

    public static FlingAnimation createMomentumFling(View view, DynamicAnimation.ViewProperty property,
                                                      float velocity, float min, float max) {
        return createFling(view, property, velocity, min, max, 1.5f);
    }

    public static ValueAnimator createFadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            view.setAlpha(v);
            view.setScaleX(0.8f + 0.2f * v);
            view.setScaleY(0.8f + 0.2f * v);
        });
        return anim;
    }

    public static ValueAnimator createFadeOut(View view, long duration) {
        ValueAnimator anim = ValueAnimator.ofFloat(view.getAlpha(), 0f);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            view.setAlpha((float) a.getAnimatedValue());
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        return anim;
    }

    public static ValueAnimator createSlideIn(View view, float fromY, float toY, long duration) {
        view.setTranslationY(fromY);
        view.setAlpha(0f);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            view.setTranslationY(fromY + (toY - fromY) * v);
            view.setAlpha(v);
        });
        return anim;
    }

    public static ValueAnimator createBounceScale(View view) {
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(400);
        anim.setInterpolator(new OvershootInterpolator(2f));
        anim.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            view.setScaleX(v);
            view.setScaleY(v);
        });
        return anim;
    }
}
