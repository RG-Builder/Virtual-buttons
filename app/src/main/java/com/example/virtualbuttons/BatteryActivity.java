package com.example.virtualbuttons;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
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
        root.setBackgroundColor(getColor(R.color.vb_surface));

        TextView title = new TextView(this);
        title.setText(getString(R.string.battery_title));
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.vb_on_surface));
        title.setPadding(0, dp(8), 0, dp(12));

        TextView detail = new TextView(this);
        detail.setText(getString(R.string.battery_detail));
        detail.setTextSize(15);
        detail.setTextColor(getColor(R.color.vb_outline));
        detail.setLineSpacing(dp(4), 1f);
        detail.setPadding(0, 0, 0, dp(24));

        Button openSettings = new Button(this);
        openSettings.setText(getString(R.string.battery_btn));
        openSettings.setAllCaps(false);
        openSettings.setTextSize(14);
        openSettings.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getColor(R.color.vb_primary));
        bg.setCornerRadius(dp(12));
        openSettings.setBackground(bg);
        openSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$BatteryOptimizationActivity"));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
            finish();
        });

        Button dismiss = new Button(this);
        dismiss.setText(getString(R.string.tutorial_skip));
        dismiss.setAllCaps(false);
        dismiss.setTextSize(14);
        dismiss.setTextColor(getColor(R.color.vb_primary));
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
