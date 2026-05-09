package com.example.virtualbuttons;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TutorialActivity extends Activity {
    private int currentStep = 0;
    private LinearLayout contentRoot;
    private TextView stepNum;
    private TextView titleView;
    private TextView descView;
    private final TutorialStep[] steps = {
        new TutorialStep(R.string.tutorial_bubble_title, R.string.tutorial_bubble_detail),
        new TutorialStep(R.string.tutorial_edge_title, R.string.tutorial_edge_detail),
        new TutorialStep(R.string.tutorial_tile_title, R.string.tutorial_tile_detail),
        new TutorialStep(R.string.tutorial_shake_title, R.string.tutorial_shake_detail),
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentStep = 0;
        setContentView(makeLayout());
    }

    private LinearLayout makeLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(32);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        stepNum = new TextView(this);
        stepNum.setText((currentStep + 1) + " / " + steps.length);
        stepNum.setTextSize(13);
        stepNum.setTextColor(Color.rgb(120, 116, 126));
        stepNum.setPadding(0, 0, 0, dp(16));

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDD14");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 0, 0, dp(24));

        titleView = new TextView(this);
        titleView.setText(getString(steps[currentStep].titleRes));
        titleView.setTextSize(22);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Color.rgb(29, 27, 32));
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, dp(12));

        descView = new TextView(this);
        descView.setText(getString(steps[currentStep].descRes));
        descView.setTextSize(16);
        descView.setTextColor(Color.rgb(72, 68, 78));
        descView.setLineSpacing(dp(6), 1f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(0, 0, 0, dp(32));

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);

        Button skip = makeTextBtn(getString(R.string.tutorial_skip));
        skip.setOnClickListener(v -> finishTutorial(false));

        Button next = makePrimaryBtn(currentStep < steps.length - 1 ? getString(R.string.tutorial_next) : getString(R.string.tutorial_got_it));
        next.setOnClickListener(v -> {
            if (currentStep < steps.length - 1) {
                animateToNext();
                currentStep++;
                updateContent();
            } else {
                finishTutorial(true);
            }
        });

        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        sp.setMargins(dp(6), 0, dp(6), 0);
        skip.setLayoutParams(sp);
        next.setLayoutParams(sp);

        navRow.addView(skip);
        navRow.addView(next);

        root.addView(stepNum);
        root.addView(icon);
        root.addView(titleView);
        root.addView(descView);
        root.addView(navRow);

        contentRoot = root;
        return root;
    }

    private void animateToNext() {
        DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
        OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.2f);

        Animation slideLeft = new TranslateAnimation(0, -contentRoot.getWidth(), 0, 0);
        slideLeft.setDuration(250);
        slideLeft.setInterpolator(DECEL);
        Animation fadeOut = new android.view.animation.AlphaAnimation(1f, 0f);
        fadeOut.setDuration(200);

        Animation slideRight = new TranslateAnimation(contentRoot.getWidth(), 0, 0, 0);
        slideRight.setDuration(250);
        slideRight.setInterpolator(DECEL);
        Animation fadeIn = new android.view.animation.AlphaAnimation(0f, 1f);
        fadeIn.setDuration(200);

        titleView.startAnimation(slideLeft);
        descView.startAnimation(fadeOut);
        titleView.postDelayed(() -> {
            titleView.startAnimation(slideRight);
            descView.startAnimation(fadeIn);
        }, 200);
    }

    private void updateContent() {
        stepNum.setText((currentStep + 1) + " / " + steps.length);
        titleView.setText(getString(steps[currentStep].titleRes));
        descView.setText(getString(steps[currentStep].descRes));
    }

    private Button makeTextBtn(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.rgb(103, 80, 164));
        GradientDrawable bg = new GradientDrawable();
        bg.setStroke(dp(1), Color.rgb(103, 80, 164));
        bg.setCornerRadius(dp(12));
        bg.setColor(Color.TRANSPARENT);
        btn.setBackground(bg);
        btn.setOnClickListener(v -> btn.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
            .withEndAction(() -> btn.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start());
        return btn;
    }

    private Button makePrimaryBtn(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(103, 80, 164));
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> btn.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
            .withEndAction(() -> btn.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start());
        return btn;
    }

    private void finishTutorial(boolean completed) {
        if (completed) {
            new SettingsStore(this).setOnboardingDone(true);
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static class TutorialStep {
        final int titleRes;
        final int descRes;
        TutorialStep(int titleRes, int descRes) {
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }
}
