package com.example.virtualbuttons;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TutorialActivity extends Activity {
    private int currentStep = 0;
    private final TutorialStep[] steps = {
        new TutorialStep(R.string.tutorial_bubble_title, R.string.tutorial_bubble_detail, "Swipe up \u2191 and down \u2193 to adjust volume.\nDouble-tap to mute.\nLong-press to hide."),
        new TutorialStep(R.string.tutorial_edge_title, R.string.tutorial_edge_detail, "Swipe from either screen edge for quick volume control."),
        new TutorialStep(R.string.tutorial_tile_title, R.string.tutorial_tile_detail, "Add \u201cVolume Button\u201d tile to Quick Settings for instant access."),
        new TutorialStep(R.string.tutorial_shake_title, R.string.tutorial_shake_detail, "Enable \u201cShake to mute\u201d in settings for hands-free control."),
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentStep = 0;
        showStep();
    }

    private void showStep() {
        setContentView(makeLayout());
    }

    private LinearLayout makeLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(32);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView stepNum = new TextView(this);
        stepNum.setText((currentStep + 1) + " / " + steps.length);
        stepNum.setTextSize(13);
        stepNum.setTextColor(Color.rgb(120, 116, 126));
        stepNum.setPadding(0, 0, 0, dp(16));

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDD14");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 0, 0, dp(24));

        TextView title = new TextView(this);
        title.setText(getString(steps[currentStep].titleRes));
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(29, 27, 32));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));

        TextView desc = new TextView(this);
        desc.setText(getString(steps[currentStep].descRes));
        desc.setTextSize(16);
        desc.setTextColor(Color.rgb(72, 68, 78));
        desc.setLineSpacing(dp(6), 1f);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, dp(32));

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);

        Button skip = makeTextBtn(getString(R.string.tutorial_skip));
        skip.setOnClickListener(v -> finishTutorial(false));

        Button next = makePrimaryBtn(currentStep < steps.length - 1 ? getString(R.string.tutorial_next) : getString(R.string.tutorial_got_it));
        next.setOnClickListener(v -> {
            if (currentStep < steps.length - 1) {
                currentStep++;
                showStep();
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
        root.addView(title);
        root.addView(desc);
        root.addView(navRow);

        return root;
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
        return btn;
    }

    private void finishTutorial(boolean completed) {
        if (completed) {
            new SettingsStore(this).setOnboardingDone(true);
        }
        finish();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static class TutorialStep {
        final int titleRes;
        final int descRes;
        TutorialStep(int titleRes, int descRes, String placeholder) {
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }
}
