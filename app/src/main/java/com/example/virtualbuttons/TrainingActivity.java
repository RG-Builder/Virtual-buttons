package com.example.virtualbuttons;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class TrainingActivity extends Activity {
    private SettingsStore settings;
    private TextView feedback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.4f);
    private static final AccelerateDecelerateInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsStore(this);
        setContentView(makeLayout());
        ActionManager.startFloatingService(this);
        ActionManager.showBubble(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private LinearLayout makeLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFFEF7FF);

        TextView title = new TextView(this);
        title.setText(getString(R.string.training_title));
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(16), 0, dp(8));

        TextView hint = new TextView(this);
        hint.setText(getString(R.string.training_hint));
        hint.setTextSize(15);
        hint.setTextColor(Color.rgb(72, 68, 78));
        hint.setLineSpacing(dp(4), 1f);
        hint.setPadding(0, 0, 0, dp(24));

        LinearLayout seekCard = new LinearLayout(this);
        seekCard.setOrientation(LinearLayout.VERTICAL);
        seekCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        seekCard.setBackground(new RoundRectDrawable(Color.WHITE, dp(12)));

        LinearLayout seekHeader = new LinearLayout(this);
        seekHeader.setOrientation(LinearLayout.HORIZONTAL);
        seekHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView seekLabel = new TextView(this);
        seekLabel.setText("Gesture sensitivity");
        seekLabel.setTextSize(15);
        seekLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        TextView seekVal = new TextView(this);
        seekVal.setText(settings.gestureSensitivity() + "dp");
        seekVal.setTextSize(15);
        seekVal.setTextColor(Color.rgb(103, 80, 164));
        seekVal.setPadding(dp(8), 0, 0, 0);

        seekHeader.addView(seekLabel);
        seekHeader.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        seekHeader.addView(seekVal);

        SeekBar seek = new SeekBar(this);
        seek.setMax(80);
        seek.setProgress(settings.gestureSensitivity() - 16);
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.rgb(103, 80, 164)));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(103, 80, 164)));
        seek.setOnSeekBarChangeListener(new SimpleSeekListener(
            p -> seekVal.setText((p + 16) + "dp"),
            p -> { settings.putInt("gesture_sensitivity", p + 16); ActionManager.refreshService(this); }
        ));

        seekCard.addView(seekHeader);
        seekCard.addView(seek);

        feedback = new TextView(this);
        feedback.setTextSize(18);
        feedback.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        feedback.setTextColor(Color.rgb(27, 94, 32));
        feedback.setPadding(0, dp(16), 0, dp(16));
        feedback.setVisibility(TextView.GONE);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        Button done = makePrimaryBtn(getString(R.string.training_done));
        done.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            handler.postDelayed(this::finish, 100);
        });

        Button reset = makeTextBtn(getString(R.string.training_reset));
        reset.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            feedback.setVisibility(TextView.GONE);
            ActionManager.refreshService(this);
        });

        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        sp.setMargins(dp(6), 0, dp(6), 0);
        done.setLayoutParams(sp);
        reset.setLayoutParams(sp);

        btnRow.addView(done);
        btnRow.addView(reset);

        root.addView(title);
        root.addView(hint);
        root.addView(seekCard);
        root.addView(feedback);
        root.addView(btnRow);

        title.setAlpha(0f); title.setTranslationY(40f);
        title.animate().alpha(1f).translationY(0f).setDuration(350).setInterpolator(DECEL).start();
        hint.setAlpha(0f); hint.setTranslationY(30f);
        hint.animate().alpha(1f).translationY(0f).setDuration(350).setStartDelay(60).setInterpolator(DECEL).start();
        seekCard.setAlpha(0f); seekCard.setTranslationY(20f);
        seekCard.animate().alpha(1f).translationY(0f).setDuration(350).setStartDelay(120).setInterpolator(DECEL).start();
        btnRow.setAlpha(0f); btnRow.setTranslationY(20f);
        btnRow.animate().alpha(1f).translationY(0f).setDuration(350).setStartDelay(200).setInterpolator(DECEL).start();

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

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
