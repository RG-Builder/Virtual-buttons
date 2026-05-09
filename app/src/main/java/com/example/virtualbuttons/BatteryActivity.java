package com.example.virtualbuttons;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BatteryActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(28);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFFEF7FF);

        TextView title = new TextView(this);
        title.setText(getString(R.string.battery_title));
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(29, 27, 32));
        title.setPadding(0, dp(8), 0, dp(12));

        TextView detail = new TextView(this);
        detail.setText(getString(R.string.battery_detail));
        detail.setTextSize(15);
        detail.setTextColor(Color.rgb(72, 68, 78));
        detail.setLineSpacing(dp(4), 1f);
        detail.setPadding(0, 0, 0, dp(24));

        Button openSettings = new Button(this);
        openSettings.setText(getString(R.string.battery_btn));
        openSettings.setAllCaps(false);
        openSettings.setTextSize(14);
        openSettings.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(103, 80, 164));
        bg.setCornerRadius(dp(12));
        openSettings.setBackground(bg);
        openSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings$BatteryOptimizationActivity"));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
            finish();
        });

        Button dismiss = new Button(this);
        dismiss.setText(getString(R.string.tutorial_skip));
        dismiss.setAllCaps(false);
        dismiss.setTextSize(14);
        dismiss.setTextColor(Color.rgb(103, 80, 164));
        dismiss.setBackgroundColor(Color.TRANSPARENT);
        dismiss.setOnClickListener(v -> finish());

        root.addView(title);
        root.addView(detail);
        root.addView(openSettings);
        root.addView(dismiss);
        setContentView(root);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
