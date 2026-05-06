package com.example.virtualbuttons;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
        content.setPadding(dp(24), dp(28), dp(24), dp(28));
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Virtual Buttons", 30, true);
        TextView subtitle = text("Fast software controls for broken or hard-to-reach volume buttons.", 16, false);
        content.addView(title);
        content.addView(subtitle);
        addSpacer(16);
        refreshStatus();
        addControls();
    }

    private void refreshStatus() {
        if (content == null) return;
        if (content.getTag() instanceof View) content.removeView((View) content.getTag());
        LinearLayout card = card();
        TextView status = text(Settings.canDrawOverlays(this) ? "Overlay permission granted" : "Overlay permission required", 18, true);
        TextView detail = text("The floating button changes volume instantly without opening this app. A persistent notification provides lock-screen-friendly controls.", 14, false);
        Button overlay = button(Settings.canDrawOverlays(this) ? "Start floating volume button" : "Grant overlay permission");
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) startActivity(AppActions.overlaySettingsIntent(this));
            else {
                settings.setOverlayEnabled(true);
                AppActions.startFloatingService(this);
            }
        });
        Button stop = button("Stop service");
        stop.setOnClickListener(v -> { settings.setOverlayEnabled(false); stopService(new Intent(this, FloatingVolumeService.class)); });
        card.addView(status);
        card.addView(detail);
        card.addView(overlay);
        card.addView(stop);
        content.addView(card, Math.min(2, content.getChildCount()));
        content.setTag(card);
    }

    private void addControls() {
        content.addView(section("Gestures"));
        addSpinner("Gesture type", new String[]{"BOTH", "SWIPE", "DOUBLE_TAP"}, settings.gestureMode().name(), value -> settings.putString("gesture_mode", value));
        addSeek("Gesture sensitivity", "dp", 16, 96, settings.gestureSensitivity(), value -> settings.putInt("gesture_sensitivity", value));
        addCheck("Edge gestures", "Swipe from either screen edge to adjust volume without aiming for the bubble.", settings.edgeGestures(), (button, checked) -> restartable("edge_gestures", checked));
        addCheck("Shake to mute/unmute", "Uses the accelerometer only while the service is active to reduce battery impact.", settings.shakeToMute(), (button, checked) -> restartable("shake_to_mute", checked));

        content.addView(section("Floating button"));
        addSeek("Button size", "dp", 44, 112, settings.buttonSizeDp(), value -> restartable("button_size", value));
        addSeek("Button opacity", "%", 30, 100, settings.buttonOpacity(), value -> restartable("button_opacity", value));

        content.addView(section("Volume behavior"));
        addSeek("Volume step", " level(s)", 1, 5, settings.volumeStep(), value -> settings.putInt("volume_step", value));
        addSpinner("Controlled stream", new String[]{"ACTIVE", "MEDIA", "SYSTEM"}, settings.streamMode().name(), value -> settings.putString("stream_mode", value));
        addCheck("Haptic feedback", "Short vibration confirms each adjustment.", settings.haptics(), (button, checked) -> settings.putBoolean("haptics", checked));
        addCheck("Visual indicator", "Show a compact Material-style volume pill after changes.", settings.visualIndicator(), (button, checked) -> settings.putBoolean("visual_indicator", checked));

        content.addView(section("Reliability and profiles"));
        addCheck("Start on boot", "Restarts the enabled overlay after reboot when overlay permission is still granted.", settings.startOnBoot(), (button, checked) -> settings.putBoolean("start_on_boot", checked));
        addCheck("Night auto-profile", "Lower media volume each night at the selected hour.", settings.autoNightProfile(), (button, checked) -> { settings.putBoolean("auto_night_profile", checked); AutoProfileScheduler.schedule(this); });
        addSeek("Night volume", "%", 0, 60, settings.nightVolumePercent(), value -> settings.putInt("night_volume", value));
        addSeek("Night starts", ":00", 18, 23, settings.nightStartHour(), value -> { settings.putInt("night_start", value); AutoProfileScheduler.schedule(this); });

        TextView footer = text("Tip: add the Quick Settings tile named “Volume Button” for one-tap access from the notification shade.", 14, false);
        footer.setPadding(0, dp(16), 0, 0);
        content.addView(footer);
    }

    private void restartable(String key, boolean value) { settings.putBoolean(key, value); restartIfRunning(); }
    private void restartable(String key, int value) { settings.putInt(key, value); restartIfRunning(); }
    private void restartIfRunning() {
        if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            stopService(new Intent(this, FloatingVolumeService.class));
            AppActions.startFloatingService(this);
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
        TextView value = text(label + ": " + current + suffix, 16, true);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(current - min);
        seek.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> {
            int actual = progress + min;
            value.setText(label + ": " + actual + suffix);
            setter.set(actual);
        }));
        card.addView(value);
        card.addView(seek);
        content.addView(card);
    }

    private void addCheck(String label, String description, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout card = card();
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(16);
        box.setChecked(checked);
        box.setOnCheckedChangeListener(listener);
        card.addView(box);
        card.addView(text(description, 14, false));
        content.addView(card);
    }

    private TextView section(String label) { TextView view = text(label, 22, true); view.setPadding(0, dp(20), 0, dp(8)); return view; }
    private TextView text(String value, int sp, boolean bold) { TextView view = new TextView(this); view.setText(value); view.setTextSize(sp); view.setTextColor(Color.rgb(29, 27, 32)); if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return view; }
    private Button button(String value) { Button button = new Button(this); button.setText(value); button.setAllCaps(false); return button; }
    private LinearLayout card() { LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(14), dp(16), dp(14)); card.setBackground(new RoundRectDrawable(Color.rgb(243, 237, 247), dp(20))); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(8), 0, dp(8)); card.setLayoutParams(lp); card.setGravity(Gravity.CENTER_VERTICAL); return card; }
    private void addSpacer(int dp) { SpaceView space = new SpaceView(this); content.addView(space, new LinearLayout.LayoutParams(1, dp(dp))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    interface ValueSetter { void set(String value); }
    interface IntSetter { void set(int value); }
}
