package com.example.virtualbuttons;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TutorialActivity extends Activity {
    private int currentStep = 0;
    private TextView titleView;
    private TextView descView;
    private TextView stepNum;
    private Button nextBtn;
    private LinearLayout contentRoot;
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

    private int bg() { return getColor(R.color.vb_surface); }
    private int text() { return getColor(R.color.vb_on_surface); }
    private int textSec() { return getColor(R.color.vb_outline); }
    private int primary() { return getColor(R.color.vb_primary); }

    private LinearLayout makeLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(32);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(bg());

        stepNum = new TextView(this);
        stepNum.setText((currentStep + 1) + " / " + steps.length);
        stepNum.setTextSize(13);
        stepNum.setTextColor(textSec());
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
        titleView.setTextColor(text());
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, dp(12));

        descView = new TextView(this);
        descView.setText(getString(steps[currentStep].descRes));
        descView.setTextSize(16);
        descView.setTextColor(textSec());
        descView.setLineSpacing(dp(6), 1f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(0, 0, 0, dp(32));

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);

        Button skip = makeTextBtn(getString(R.string.tutorial_skip));
        skip.setOnClickListener(v -> { animatePress(v); finishTutorial(false); });

        nextBtn = makePrimaryBtn(currentStep < steps.length - 1 ? getString(R.string.tutorial_next) : getString(R.string.tutorial_got_it));
        nextBtn.setOnClickListener(v -> {
            animatePress(v);
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
        nextBtn.setLayoutParams(sp);

        navRow.addView(skip);
        navRow.addView(nextBtn);

        root.addView(stepNum);
        root.addView(icon);
        root.addView(titleView);
        root.addView(descView);
        root.addView(navRow);

        contentRoot = root;
        return root;
    }

    private void animateToNext() {
        Animation slideLeft = new TranslateAnimation(0, -contentRoot.getWidth() * 0.4f, 0, 0);
        slideLeft.setDuration(200);
        Animation slideRight = new TranslateAnimation(contentRoot.getWidth() * 0.4f, 0, 0, 0);
        slideRight.setDuration(200);
        slideRight.setStartOffset(200);
        titleView.startAnimation(slideLeft);
        descView.startAnimation(slideLeft);
        titleView.postDelayed(() -> titleView.startAnimation(slideRight), 220);
        descView.postDelayed(() -> descView.startAnimation(slideRight), 220);
    }

    private void updateContent() {
        stepNum.setText((currentStep + 1) + " / " + steps.length);
        titleView.setText(getString(steps[currentStep].titleRes));
        descView.setText(getString(steps[currentStep].descRes));
        nextBtn.setText(currentStep < steps.length - 1 ? getString(R.string.tutorial_next) : getString(R.string.tutorial_got_it));
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(new Runnable() {
            @Override public void run() {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
            }
        }).start();
    }

    private Button makeTextBtn(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(primary());
        GradientDrawable bg = new GradientDrawable();
        bg.setStroke(dp(1), primary());
        bg.setCornerRadius(dp(12));
        bg.setColor(Color.TRANSPARENT);
        btn.setBackground(bg);
        return btn;
    }

    private Button makePrimaryBtn(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary());
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        return btn;
    }

    private void finishTutorial(boolean completed) {
        if (completed) new SettingsStore(this).setOnboardingDone(true);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static class TutorialStep {
        final int titleRes;
        final int descRes;
        TutorialStep(int titleRes, int descRes) { this.titleRes = titleRes; this.descRes = descRes; }
    }
}
