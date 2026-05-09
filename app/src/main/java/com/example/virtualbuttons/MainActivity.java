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
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.HapticFeedbackConstants;
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

import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private SettingsStore settings;
    private LinearLayout content;
    private boolean darkMode;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private static final int COLOR_PRIMARY = Color.rgb(103, 80, 164);
    private static final int COLOR_PRIMARY_DARK = Color.rgb(69, 49, 120);
    private static final int COLOR_SURFACE = 0xFFFEF7FF;
    private static final int COLOR_SURFACE_DARK = Color.rgb(18, 18, 24);
    private static final int COLOR_CARD_DARK = Color.rgb(28, 28, 36);
    private static final int COLOR_TEXT_DARK = Color.rgb(240, 238, 245);
    private static final int COLOR_TEXT_SEC_DARK = Color.rgb(160, 158, 170);
    private static final int COLOR_BG_DARK = Color.rgb(12, 12, 16);

    @Override protected void onCreate(Bundle savedInstanceState) {
        settings = new SettingsStore(this);
        darkMode = settings.darkMode();
        if (darkMode) setTheme(android.R.style.Theme_DeviceDefault);
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
        if (!settings.onboardingDone() && Settings.canDrawOverlays(this)) {
            startActivity(new Intent(this, TutorialActivity.class));
        }
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            ttsReady = true;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    private int bgColor() { return darkMode ? COLOR_BG_DARK : COLOR_SURFACE; }
    private int cardColor() { return darkMode ? COLOR_CARD_DARK : Color.WHITE; }
    private int textColor() { return darkMode ? COLOR_TEXT_DARK : Color.rgb(29, 27, 32); }
    private int textSecColor() { return darkMode ? COLOR_TEXT_SEC_DARK : Color.rgb(96, 90, 102); }
    private int primaryColor() { return COLOR_PRIMARY; }
    private int cardBgColor() { return darkMode ? Color.rgb(255, 255, 255) : Color.WHITE; }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(bgColor());
        scroll.addView(content);
        setContentView(scroll);

        addHeader();
        addSpacer(8);
        refreshStatus();
        addControls();
    }

    private void addHeader() {
        TextView title = text("Virtual Buttons", 30, true);
        title.setLetterSpacing(0.02f);
        TextView subtitle = text("Fast software controls for broken or hard-to-reach volume buttons.", 16, false);
        subtitle.setTextColor(textSecColor());
        subtitle.setLineSpacing(dp(4), 1f);
        content.addView(title);
        content.addView(subtitle);
    }

    private void refreshStatus() {
        if (content == null) return;
        if (content.getTag() instanceof View) content.removeView((View) content.getTag());

        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean running = isServiceRunning();

        int cardBg, headColor, detailColor;
        String headline, detail;
        if (!overlayGranted) {
            cardBg = darkMode ? Color.rgb(80, 20, 20) : Color.rgb(255, 235, 238);
            headline = getString(R.string.status_permission);
            detail = getString(R.string.status_permission_detail);
            headColor = darkMode ? Color.rgb(255, 100, 100) : Color.rgb(183, 28, 28);
            detailColor = textSecColor();
        } else if (running) {
            cardBg = darkMode ? Color.rgb(20, 60, 30) : Color.rgb(232, 245, 233);
            headline = getString(R.string.status_active);
            detail = getString(R.string.status_active_detail);
            headColor = darkMode ? Color.rgb(100, 255, 130) : Color.rgb(27, 94, 32);
            detailColor = textSecColor();
        } else {
            cardBg = darkMode ? Color.rgb(60, 45, 10) : Color.rgb(255, 243, 224);
            headline = getString(R.string.status_ready);
            detail = getString(R.string.status_ready_detail);
            headColor = darkMode ? Color.rgb(255, 200, 80) : Color.rgb(230, 81, 0);
            detailColor = textSecColor();
        }

        LinearLayout card = card();
        card.setBackground(new RoundRectDrawable(cardBg, dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) card.setElevation(dp(4));

        TextView status = text(headline, 18, true);
        status.setPadding(0, dp(4), 0, 0);
        status.setTextColor(headColor);

        TextView desc = text(detail, 14, false);
        desc.setTextColor(detailColor);
        desc.setLineSpacing(dp(2), 1f);
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
                startActivity(ActionManager.overlaySettingsIntent(this));
            } else {
                checkBatteryOptimization();
                settings.setOverlayEnabled(true);
                ActionManager.startFloatingService(this);
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

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            startActivity(new Intent(this, BatteryActivity.class));
        }
    }

    private void addControls() {
        content.addView(section(getString(R.string.section_gestures)));
        addSpinnerDesc(getString(R.string.gesture_type_title), getString(R.string.gesture_type_desc),
            new String[]{"BOTH", "SWIPE", "DOUBLE_TAP"}, settings.gestureMode().name(),
            value -> settings.putString("gesture_mode", value));
        addSeek("Gesture sensitivity", "dp", 16, 96, settings.gestureSensitivity(), value -> { settings.putInt("gesture_sensitivity", value); restartIfRunning(); });
        addCheck("Edge gestures", "Swipe from either screen edge to adjust volume.", settings.edgeGestures(), (b, c) -> restartable("edge_gestures", c));
        addSeek("Edge strip width", "dp", 4, 24, settings.edgeWidthDp(), value -> restartable("edge_width", value));
        addCheck("Shake to mute", "Shake your device to toggle mute.", settings.shakeToMute(), (b, c) -> { restartable("shake_to_mute", c); });
        addSeekDesc(getString(R.string.shake_sensitivity), getString(R.string.shake_sensitivity_desc), 150, 400, settings.shakeThreshold(), value -> { settings.putInt("shake_threshold", value); restartIfRunning(); });

        content.addView(section(getString(R.string.section_floating_button)));
        addSeek("Button size", "dp", 44, 112, settings.buttonSizeDp(), value -> restartable("button_size", value));
        addSeek("Button opacity", "%", 30, 100, settings.buttonOpacity(), value -> restartable("button_opacity", value));
        addColorPicker();

        content.addView(section(getString(R.string.section_volume_behavior)));
        addSeek("Volume step", value -> settings.putInt("volume_step", value));
        addSpinnerDesc(getString(R.string.controlled_stream_title), getString(R.string.stream_desc),
            new String[]{getString(R.string.stream_active), getString(R.string.stream_media), getString(R.string.stream_system)},
            streamLabelToMode(settings.streamMode()),
            value -> settings.putString("stream_mode", streamModeToLabel(value)));
        addCheck("Haptic feedback", "Short vibration confirms each adjustment.", settings.haptics(), (b, c) -> settings.putBoolean("haptics", c));
        addCheck("Visual indicator", "Show a compact volume pill after changes.", settings.visualIndicator(), (b, c) -> settings.putBoolean("visual_indicator", c));
        addCheck(getString(R.string.accessibility_title), getString(R.string.accessibility_desc), settings.accessibilitySpeech(), (b, c) -> settings.putBoolean("accessibility_speech", c));

        content.addView(section(getString(R.string.section_reliability)));
        addCheck("Start on boot", "Restarts overlay after reboot.", settings.startOnBoot(), (b, c) -> settings.putBoolean("start_on_boot", c));
        addCheck("Night auto-profile", "Lower volume each night automatically.", settings.autoNightProfile(), (b, c) -> { settings.putBoolean("auto_night_profile", c); AutoProfileScheduler.schedule(this); });
        addSeekDesc("Night volume", "Reduce media volume to this level (%)", 0, 60, settings.nightVolumePercent(), value -> settings.putInt("night_volume", value));
        addSeekDesc("Night starts", ":00", 18, 23, settings.nightStartHour(), value -> { settings.putInt("night_start", value); AutoProfileScheduler.schedule(this); });
        addSeekDesc("Night ends", ":00", 4, 10, settings.nightEndHour(), value -> { settings.putInt("night_end", value); AutoProfileScheduler.schedule(this); });

        content.addView(section("Presets"));
        addPresetButtons();

        content.addView(section("Settings"));
        addCheck(getString(R.string.dark_mode), getString(R.string.dark_mode_desc), settings.darkMode(), (b, c) -> {
            settings.setDarkMode(c);
            recreate();
        });
        addButtonCard(getString(R.string.training_title), "Test your gestures in real time.", v -> startActivity(new Intent(this, TrainingActivity.class)));

        content.addView(section(getString(R.string.section_about)));
        addAboutCard();

        TextView footer = text(getString(R.string.tip_tile), 13, false);
        footer.setTextColor(textSecColor());
        footer.setPadding(0, dp(24), 0, dp(8));
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(footer);
    }

    private String streamLabelToMode(SettingsStore.StreamMode mode) {
        if (mode == SettingsStore.StreamMode.MEDIA) return getString(R.string.stream_media);
        if (mode == SettingsStore.StreamMode.SYSTEM) return getString(R.string.stream_system);
        return getString(R.string.stream_active);
    }

    private String streamModeToLabel(String value) {
        if (value.equals(getString(R.string.stream_media))) return "MEDIA";
        if (value.equals(getString(R.string.stream_system))) return "SYSTEM";
        return "ACTIVE";
    }

    private void addColorPicker() {
        LinearLayout card = card();
        TextView lbl = text(getString(R.string.bubble_color), 15, true);
        TextView colorVal = text("\u2B24", 20, false);
        colorVal.setTextColor(Color.HSVToColor(new float[]{settings.bubbleColorHue(), 0.65f, 0.85f}));
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(lbl);
        header.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(colorVal);
        card.addView(header);

        String[] hueLabels = {"Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Pink"};
        int[] hueValues = {0, 30, 60, 120, 180, 240, 265, 330};
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < hueValues.length; i++) {
            Button colorBtn = new Button(this);
            colorBtn.setText("\u25CF");
            colorBtn.setTextSize(18);
            colorBtn.setAllCaps(false);
            int hue = hueValues[i];
            colorBtn.setTextColor(Color.HSVToColor(new float[]{hue, 0.65f, 0.85f}));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            bg.setColor(Color.HSVToColor(new float[]{hue, 0.3f, 0.9f}));
            colorBtn.setBackground(bg);
            int finalI = i;
            colorBtn.setOnClickListener(v -> {
                settings.putInt("bubble_color_hue", hue);
                colorVal.setTextColor(Color.HSVToColor(new float[]{hue, 0.65f, 0.85f}));
                restartIfRunning();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.setMargins(dp(4), dp(8), dp(4), dp(8));
            colorBtn.setLayoutParams(lp);
            colorRow.addView(colorBtn);
        }
        card.addView(colorRow);
        content.addView(card);
    }

    private void addPresetButtons() {
        LinearLayout card = card();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        String[] presets = {"Silent", "Normal", "Vibrate"};
        int[] presetValues = {0, -2, -1};
        for (int i = 0; i < presets.length; i++) {
            Button presetBtn = new Button(this);
            presetBtn.setText(presets[i]);
            presetBtn.setAllCaps(false);
            presetBtn.setTextSize(13);
            int idx = i;
            presetBtn.setOnClickListener(v -> {
                int target = presetValues[idx];
                VolumeController vc = new VolumeController(this, settings);
                if (target == -1) {
                    vc.muteOrRestoreMedia();
                } else {
                    android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    int max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                    int vol = target == -2 ? Math.round(max * 0.5f) : target;
                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, vol, 0);
                }
                if (settings.accessibilitySpeech() && ttsReady) {
                    tts.speak("Preset applied: " + presets[idx], TextToSpeech.QUEUE_FLUSH, null, "preset");
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(4), 0, dp(4), 0);
            presetBtn.setLayoutParams(lp);
            row.addView(presetBtn);
        }
        card.addView(row);
        content.addView(card);
    }

    private void addButtonCard(String title, String desc, View.OnClickListener listener) {
        LinearLayout card = card();
        Button btn = new Button(this);
        btn.setText(title);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(listener);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primaryColor());
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        card.addView(btn);
        content.addView(card);
    }

    private void addAboutCard() {
        LinearLayout card = card();
        TextView versionLabel = text("Version", 13, false);
        versionLabel.setTextColor(textSecColor());
        TextView versionVal = text("1.0", 13, false);

        LinearLayout versionRow = new LinearLayout(this);
        versionRow.setOrientation(LinearLayout.HORIZONTAL);
        versionRow.addView(versionLabel);
        versionRow.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        versionRow.addView(versionVal);

        TextView desc = text(getString(R.string.about_desc), 13, false);
        desc.setTextColor(textSecColor());
        desc.setLineSpacing(dp(2), 1f);

        LinearLayout linkRow = new LinearLayout(this);
        linkRow.setOrientation(LinearLayout.HORIZONTAL);
        linkRow.addView(linkBtn("Rate this app", v -> {}));
        linkRow.addView(new SpaceView(this), new LinearLayout.LayoutParams(dp(12), 1));
        linkRow.addView(linkBtn("Feedback", v -> {}));

        card.addView(versionRow);
        card.addView(desc);
        card.addView(linkRow);
        content.addView(card);
    }

    private Button linkBtn(String label, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setTextColor(primaryColor());
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setOnClickListener(listener);
        return btn;
    }

    private void restartable(String key, boolean value) { settings.putBoolean(key, value); restartIfRunning(); refreshStatus(); }
    private void restartable(String key, int value) { settings.putInt(key, value); restartIfRunning(); refreshStatus(); }
    private void restartIfRunning() {
        if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            ActionManager.refreshService(this);
        }
    }

    private void addSpinnerDesc(String label, String desc, String[] values, String selected, ValueSetter setter) {
        LinearLayout card = card();
        card.addView(text(label, 16, true));
        TextView descView = text(desc, 12, false);
        descView.setTextColor(textSecColor());
        descView.setLineSpacing(dp(2), 1f);
        descView.setPadding(0, 0, 0, dp(4));
        card.addView(descView);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) spinner.setSelection(i);
        spinner.setOnItemSelectedListener(new SimpleSelectedListener(position -> setter.set(values[position])));
        card.addView(spinner);
        content.addView(card);
    }

    private void addSeekDesc(String label, String desc, int min, int max, int current, IntSetter setter) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl = text(label, 15, true);
        TextView value = text(current + "", 15, false);
        value.setTextColor(primaryColor());
        header.addView(lbl);
        header.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(value);
        card.addView(header);
        TextView descView = text(desc, 12, false);
        descView.setTextColor(textSecColor());
        card.addView(descView);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(current - min);
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(primaryColor()));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(primaryColor()));
        seek.setPadding(dp(6), dp(4), dp(6), dp(4));
        seek.setOnSeekBarChangeListener(new SimpleSeekListener(
            progress -> value.setText((progress + min) + ""),
            progress -> setter.set(progress + min)
        ));
        card.addView(seek);
        content.addView(card);
    }

    private void addSeek(String label, IntSetter setter) {
        addSeekDesc(label, "", 1, 5, settings.volumeStep(), setter);
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
        addSeekDesc(label, "", min, max, current, setter);
    }

    private void addCheck(String label, String description, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout card = card();
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(15);
        box.setTextColor(textColor());
        box.setChecked(checked);
        box.setOnCheckedChangeListener(listener);
        card.addView(box);
        TextView descView = text(description, 13, false);
        descView.setTextColor(textSecColor());
        descView.setLineSpacing(dp(2), 1f);
        descView.setPadding(0, dp(2), 0, 0);
        card.addView(descView);
        content.addView(card);
    }

    private TextView section(String label) {
        TextView view = text(label, 14, true);
        view.setAllCaps(true);
        view.setLetterSpacing(0.1f);
        view.setPadding(0, dp(24), 0, dp(4));
        view.setTextColor(textSecColor());
        return view;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(textColor());
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }

    private void stylePrimary(Button button) {
        button.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primaryColor());
        bg.setCornerRadius(dp(12));
        button.setBackground(bg);
    }

    private void styleSecondary(Button button) {
        button.setTextColor(primaryColor());
        GradientDrawable bg = new GradientDrawable();
        bg.setStroke(dp(1), primaryColor());
        bg.setColor(Color.TRANSPARENT);
        bg.setCornerRadius(dp(12));
        button.setBackground(bg);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        card.setPadding(p, dp(12), p, dp(12));
        card.setBackground(new RoundRectDrawable(cardColor(), dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) card.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);
        card.setGravity(Gravity.CENTER_VERTICAL);
        return card;
    }

    private void addSpacer(int dpVal) {
        SpaceView space = new SpaceView(this);
        content.addView(space, new LinearLayout.LayoutParams(1, dp(dpVal)));
    }

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
