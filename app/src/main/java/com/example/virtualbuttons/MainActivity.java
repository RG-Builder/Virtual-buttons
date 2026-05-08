package com.example.virtualbuttons;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SettingsStore settings;
    private LinearLayout content;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsStore(this);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(Color.rgb(254, 247, 255));
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Virtual Buttons", 30, true);
        title.setLetterSpacing(0.02f);
        TextView subtitle = text("Fast software controls for broken or hard-to-reach volume buttons.", 16, false);
        subtitle.setTextColor(Color.rgb(96, 90, 102));
        subtitle.setLineSpacing(dp(4), 1f);
        content.addView(title);
        content.addView(subtitle);
        addSpacer(8);
        refreshStatus();
        addControls();
    }

    private void refreshStatus() {
        if (content == null) return;
        if (content.getTag() instanceof View) content.removeView((View) content.getTag());

        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean running = isServiceRunning();

        LinearLayout card = card();
        int cardBg;
        String headline, detail;
        if (!overlayGranted) {
            cardBg = Color.rgb(255, 235, 238);
            headline = getString(R.string.status_permission);
            detail = getString(R.string.status_permission_detail);
        } else if (running) {
            cardBg = Color.rgb(232, 245, 233);
            headline = getString(R.string.status_active);
            detail = getString(R.string.status_active_detail);
        } else {
            cardBg = Color.rgb(255, 243, 224);
            headline = getString(R.string.status_ready);
            detail = getString(R.string.status_ready_detail);
        }
        card.setBackground(new RoundRectDrawable(cardBg, dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(4));
        }

        TextView status = text(headline, 18, true);
        status.setPadding(0, dp(4), 0, 0);
        TextView desc = text(detail, 14, false);
        desc.setTextColor(Color.rgb(72, 68, 78));
        desc.setLineSpacing(dp(2), 1f);
        status.setTextColor(running ? Color.rgb(27, 94, 32) : !overlayGranted ? Color.rgb(183, 28, 28) : Color.rgb(230, 81, 0));
        desc.setPadding(0, dp(4), 0, dp(12));

        Button primary = button(overlayGranted ? (running ? getString(R.string.btn_restart) : getString(R.string.btn_start)) : getString(R.string.btn_grant_permission));
        primary.setTextSize(14);
        stylePrimary(primary);

        Button stop = button(getString(R.string.btn_stop));
        stop.setTextSize(14);
        styleSecondary(stop);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(-2, dp(40));
        primaryParams.rightMargin = dp(8);
        primary.setLayoutParams(primaryParams);
        stop.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(40)));
        btnRow.addView(primary);
        btnRow.addView(stop);

        primary.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (!Settings.canDrawOverlays(this)) {
                startActivity(AppActions.overlaySettingsIntent(this));
            } else {
                settings.setOverlayEnabled(true);
                AppActions.startFloatingService(this);
                refreshStatus();
            }
        });

        stop.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            settings.setOverlayEnabled(false);
            stopService(new Intent(this, FloatingVolumeService.class));
            refreshStatus();
        });

        card.addView(status);
        card.addView(desc);
        card.addView(btnRow);
        content.addView(card, Math.min(2, content.getChildCount()));
        content.setTag(card);
    }

    private void addControls() {
        content.addView(section(getString(R.string.section_gestures)));
        addSpinner("Gesture type", new String[]{"BOTH", "SWIPE", "DOUBLE_TAP"}, settings.gestureMode().name(), value -> settings.putString("gesture_mode", value));
        addSeek("Gesture sensitivity", "dp", 16, 96, settings.gestureSensitivity(), value -> settings.putInt("gesture_sensitivity", value));
        addCheck("Edge gestures", "Swipe from either screen edge to adjust volume without aiming for the bubble.", settings.edgeGestures(), (button, checked) -> restartable("edge_gestures", checked));
        addSeek("Edge strip width", "dp", 4, 24, settings.edgeWidthDp(), value -> restartable("edge_width", value));
        addCheck("Shake to mute/unmute", "Uses the accelerometer only while the service is active to reduce battery impact.", settings.shakeToMute(), (button, checked) -> restartable("shake_to_mute", checked));

        content.addView(section(getString(R.string.section_floating_button)));
        addSeek("Button size", "dp", 44, 112, settings.buttonSizeDp(), value -> restartable("button_size", value));
        addSeek("Button opacity", "%", 30, 100, settings.buttonOpacity(), value -> restartable("button_opacity", value));

        content.addView(section(getString(R.string.section_volume_behavior)));
        addSeek("Volume step", " level(s)", 1, 5, settings.volumeStep(), value -> settings.putInt("volume_step", value));
        addSpinner("Controlled stream", new String[]{"ACTIVE", "MEDIA", "SYSTEM"}, settings.streamMode().name(), value -> settings.putString("stream_mode", value));
        addCheck("Haptic feedback", "Short vibration confirms each adjustment.", settings.haptics(), (button, checked) -> settings.putBoolean("haptics", checked));
        addCheck("Visual indicator", "Show a compact Material-style volume pill after changes.", settings.visualIndicator(), (button, checked) -> settings.putBoolean("visual_indicator", checked));

        content.addView(section(getString(R.string.section_reliability)));
        addCheck("Start on boot", "Restarts the enabled overlay after reboot when overlay permission is still granted.", settings.startOnBoot(), (button, checked) -> settings.putBoolean("start_on_boot", checked));
        addCheck("Night auto-profile", "Lower media volume each night at the selected hour.", settings.autoNightProfile(), (button, checked) -> { settings.putBoolean("auto_night_profile", checked); AutoProfileScheduler.schedule(this); });
        addSeek("Night volume", "%", 0, 60, settings.nightVolumePercent(), value -> settings.putInt("night_volume", value));
        addSeek("Night starts", ":00", 18, 23, settings.nightStartHour(), value -> { settings.putInt("night_start", value); AutoProfileScheduler.schedule(this); });

        TextView footer = text(getString(R.string.tip_tile), 13, false);
        footer.setTextColor(Color.rgb(120, 116, 126));
        footer.setPadding(0, dp(24), 0, dp(8));
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(footer);
    }

    private void restartable(String key, boolean value) { settings.putBoolean(key, value); restartIfRunning(); refreshStatus(); }
    private void restartable(String key, int value) { settings.putInt(key, value); restartIfRunning(); refreshStatus(); }
    private void restartIfRunning() {
        if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            AppActions.refreshService(this);
        }
    }

    private void addSpinner(String label, String[] values, String selected, ValueSetter setter) {
        LinearLayout card = card();
        card.addView(text(label, 16, true));
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) spinner.setSelection(i);
        spinner.setOnItemSelectedListener(new SimpleSelectedListener(position -> setter.set(values[position])));
        card.addView(spinner);
        content.addView(card);
    }

    private void addSeek(String label, String suffix, int min, int max, int current, IntSetter setter) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl = text(label, 15, true);
        TextView value = text(current + suffix, 15, false);
        value.setTextColor(Color.rgb(103, 80, 164));
        header.addView(lbl);
        header.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(value);
        card.addView(header);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(current - min);
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.rgb(103, 80, 164)));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(103, 80, 164)));
        seek.setPadding(dp(6), dp(4), dp(6), dp(4));
        seek.setOnSeekBarChangeListener(new SimpleSeekListener(
            progress -> value.setText((progress + min) + suffix),
            progress -> setter.set(progress + min)
        ));
        card.addView(seek);
        content.addView(card);
    }

    private void addCheck(String label, String description, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout card = card();
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(15);
        box.setTextColor(Color.rgb(29, 27, 32));
        box.setChecked(checked);
        box.setOnCheckedChangeListener(listener);
        card.addView(box);
        TextView desc = text(description, 13, false);
        desc.setTextColor(Color.rgb(96, 90, 102));
        desc.setLineSpacing(dp(2), 1f);
        desc.setPadding(0, dp(2), 0, 0);
        card.addView(desc);
        content.addView(card);
    }

    private TextView section(String label) {
        TextView view = text(label, 14, true);
        view.setAllCaps(true);
        view.setLetterSpacing(0.1f);
        view.setPadding(0, dp(24), 0, dp(4));
        return view;
    }
    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.rgb(29, 27, 32));
        view.setFontFeatureSettings("smcp");
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }
    private Button button(String value) { Button button = new Button(this); button.setText(value); button.setAllCaps(false); return button; }

    private void stylePrimary(Button button) {
        int primary = getResources().getColor(R.color.vb_primary);
        button.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary);
        bg.setCornerRadius(dp(12));
        button.setBackground(bg);
    }

    private void styleSecondary(Button button) {
        int primary = getResources().getColor(R.color.vb_primary);
        button.setTextColor(primary);
        GradientDrawable bg = new GradientDrawable();
        bg.setStroke(dp(1), primary);
        bg.setColor(Color.TRANSPARENT);
        bg.setCornerRadius(dp(12));
        button.setBackground(bg);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        card.setPadding(p, dp(12), p, dp(12));
        card.setBackground(new RoundRectDrawable(Color.rgb(255, 255, 255), dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);
        card.setGravity(Gravity.CENTER_VERTICAL);
        return card;
    }

    private void addSpacer(int dp) { SpaceView space = new SpaceView(this); content.addView(space, new LinearLayout.LayoutParams(1, dp(dp))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingVolumeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    interface ValueSetter { void set(String value); }
    interface IntSetter { void set(int value); }
}
