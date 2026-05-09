package com.example.virtualbuttons;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.HapticFeedbackConstants;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.TranslateAnimation;
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
    private ScrollView scrollView;

    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.4f);
    private static final AccelerateDecelerateInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();

    @Override protected void onCreate(Bundle savedInstanceState) {
        settings = new SettingsStore(this);
        darkMode = settings.darkMode();
        if (darkMode) setTheme(R.style.AppTheme_Dark);
        else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);
        buildUi();
        animateEntrance();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
        if (!settings.onboardingDone() && Settings.canDrawOverlays(this)) {
            startActivity(new Intent(this, TutorialActivity.class));
        }
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.US); ttsReady = true; }
    }

    @Override protected void onResume() { super.onResume(); refreshStatus(); }

    @Override protected void onDestroy() {
        if (animateEntranceHandler != null && animateEntranceRunnable != null) animateEntranceHandler.removeCallbacks(animateEntranceRunnable);
        if (tts != null) tts.shutdown(); super.onDestroy();
    }

    private int primary() { return darkMode ? getColor(R.color.vb_primary_dark) : getColor(R.color.vb_primary); }
    private int bg() { return darkMode ? getColor(R.color.vb_bg_dark) : getColor(R.color.vb_surface); }
    private int cardBg() { return darkMode ? getColor(R.color.vb_card_dark) : Color.WHITE; }
    private int text() { return darkMode ? getColor(R.color.vb_on_surface_dark) : getColor(R.color.vb_on_surface); }
    private int textSec() { return darkMode ? getColor(R.color.vb_text_sec_dark) : getColor(R.color.vb_outline); }
    private int statusActiveBg() { return darkMode ? getColor(R.color.vb_status_success_bg_dark) : getColor(R.color.vb_status_success_bg); }
    private int statusActiveText() { return darkMode ? getColor(R.color.vb_status_success_dark) : getColor(R.color.vb_success); }
    private int statusReadyBg() { return darkMode ? getColor(R.color.vb_status_warning_bg_dark) : getColor(R.color.vb_status_warning_bg); }
    private int statusReadyText() { return darkMode ? getColor(R.color.vb_status_warning_dark) : getColor(R.color.vb_warning); }
    private int statusErrorBg() { return darkMode ? getColor(R.color.vb_status_error_bg_dark) : getColor(R.color.vb_status_error_bg); }
    private int statusErrorText() { return darkMode ? getColor(R.color.vb_status_error_dark) : getColor(R.color.vb_error); }

    private Handler animateEntranceHandler;
    private Runnable animateEntranceRunnable;

    private void animateEntrance() {
        animateEntranceHandler = new Handler(Looper.getMainLooper());
        animateEntranceRunnable = new Runnable() {
            @Override public void run() {
                if (content == null) return;
                for (int i = 1; i < content.getChildCount(); i++) {
                    View child = content.getChildAt(i);
                    child.setTranslationY(80f);
                    child.setAlpha(0f);
                    child.animate().translationY(0f).alpha(1f).setDuration(400).setStartDelay(i * 60L).setInterpolator(DECEL).start();
                }
            }
        };
        animateEntranceHandler.postDelayed(animateEntranceRunnable, 50);
    }

    private void buildUi() {
        scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(bg());
        scrollView.addView(content);
        setContentView(scrollView);
        addHeader();
        addSpacer(8);
        refreshStatus();
        addControls();
    }

    private void addHeader() {
        content.addView(text(getString(R.string.app_name), 30, true));
        TextView subtitle = text(getString(R.string.app_description), 16, false);
        subtitle.setTextColor(textSec());
        subtitle.setLineSpacing(dp(4), 1f);
        content.addView(subtitle);
    }

    private void refreshStatus() {
        if (content == null) return;
        if (content.getTag() instanceof View) content.removeView((View) content.getTag());

        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean running = isServiceRunning();

        int cardBg, headColor;
        String headline, detail;
        if (!overlayGranted) {
            cardBg = statusErrorBg(); headline = getString(R.string.status_permission); detail = getString(R.string.status_permission_detail); headColor = statusErrorText();
        } else if (running) {
            cardBg = statusActiveBg(); headline = getString(R.string.status_active); detail = getString(R.string.status_active_detail); headColor = statusActiveText();
        } else {
            cardBg = statusReadyBg(); headline = getString(R.string.status_ready); detail = getString(R.string.status_ready_detail); headColor = statusReadyText();
        }

        LinearLayout card = mkCard();
        card.setBackground(new RoundRectDrawable(cardBg, dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) card.setElevation(dp(4));

        TextView status = text(headline, 18, true);
        status.setPadding(0, dp(4), 0, 0);
        status.setTextColor(headColor);

        TextView desc = text(detail, 14, false);
        desc.setTextColor(textSec());
        desc.setLineSpacing(dp(2), 1f);
        desc.setPadding(0, dp(4), 0, dp(12));

        Button primaryBtn = mkButton(overlayGranted ? (running ? getString(R.string.btn_restart) : getString(R.string.btn_start)) : getString(R.string.btn_grant_permission));
        primaryBtn.setTextSize(14);
        stylePrimary(primaryBtn);
        Button stopBtn = mkButton(getString(R.string.btn_stop));
        stopBtn.setTextSize(14);
        styleSecondary(stopBtn);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(48));
        p.rightMargin = dp(8);
        primaryBtn.setLayoutParams(p);
        stopBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(48)));
        btnRow.addView(primaryBtn);
        btnRow.addView(stopBtn);

        primaryBtn.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); animatePress(v); if (!Settings.canDrawOverlays(this)) { startActivity(ActionManager.overlaySettingsIntent(this)); } else { checkBatteryOptimization(); settings.setOverlayEnabled(true); ActionManager.startFloatingService(this); refreshStatus(); } });
        stopBtn.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); animatePress(v); if (settings.backgroundRunning()) settings.setBackgroundRunning(false); settings.setOverlayEnabled(false); ActionManager.stopFloatingService(this); refreshStatus(); });

        card.addView(status);
        card.addView(desc);
        card.addView(btnRow);
        content.addView(card, Math.min(2, content.getChildCount()));
        content.setTag(card);

        card.setScaleX(0.9f); card.setScaleY(0.9f); card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(OVERSHOOT).start();
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            startActivity(new Intent(this, BatteryActivity.class));
        }
    }

    private void addControls() {
        content.addView(section(getString(R.string.section_gestures)));
        addSpinnerDesc(getString(R.string.gesture_type_title), getString(R.string.gesture_type_desc), new String[]{getString(R.string.gesture_both), getString(R.string.gesture_swipe), getString(R.string.gesture_double_tap)}, gestureModeToLabel(settings.gestureMode()), v -> settings.putString("gesture_mode", gestureModeToEnum(v)));
        addSeek(getString(R.string.gesture_sensitivity), getString(R.string.unit_dp), 16, 96, settings.gestureSensitivity(), v -> { settings.putInt("gesture_sensitivity", v); restartIfRunning(); });
        addCheck(getString(R.string.desc_edge_gestures), "", settings.edgeGestures(), (b, c) -> restartable("edge_gestures", c));
        addSeek(getString(R.string.edge_strip_width), getString(R.string.unit_dp), 4, 24, settings.edgeWidthDp(), v -> restartable("edge_width", v));
        addCheck(getString(R.string.shake_to_mute), "", settings.shakeToMute(), (b, c) -> restartable("shake_to_mute", c));
        addSeek(getString(R.string.shake_sensitivity), getString(R.string.unit_threshold), 150, 400, settings.shakeThreshold(), v -> { settings.putInt("shake_threshold", v); restartIfRunning(); });

        content.addView(section(getString(R.string.section_floating_button)));
        addSeek(getString(R.string.button_size), getString(R.string.unit_dp), 44, 112, settings.buttonSizeDp(), v -> restartable("button_size", v));
        addSeek(getString(R.string.button_opacity), getString(R.string.unit_percent), 30, 100, settings.buttonOpacity(), v -> restartable("button_opacity", v));
        addColorPicker();

        content.addView(section(getString(R.string.section_volume_behavior)));
        addSeek(getString(R.string.volume_step), "", 1, 5, settings.volumeStep(), v -> settings.putInt("volume_step", v));
        addSpinnerDesc(getString(R.string.controlled_stream_title), getString(R.string.stream_desc), new String[]{getString(R.string.stream_active), getString(R.string.stream_media), getString(R.string.stream_system)}, streamModeToLabel(settings.streamMode()), v -> settings.putString("stream_mode", streamModeToEnum(v)));
        addCheck(getString(R.string.desc_haptic), "", settings.haptics(), (b, c) -> settings.putBoolean("haptics", c));
        addCheck(getString(R.string.desc_visual_indicator), "", settings.visualIndicator(), (b, c) -> settings.putBoolean("visual_indicator", c));
        addCheck(getString(R.string.accessibility_title), getString(R.string.accessibility_desc), settings.accessibilitySpeech(), (b, c) -> settings.putBoolean("accessibility_speech", c));

        content.addView(section(getString(R.string.section_reliability)));
        addCheck(getString(R.string.desc_start_on_boot), "", settings.startOnBoot(), (b, c) -> settings.putBoolean("start_on_boot", c));
        addCheck(getString(R.string.hide_notification_title), getString(R.string.hide_notification_desc), settings.hideNotification(), (b, c) -> { settings.setHideNotification(c); restartIfRunning(); });
        addCheck(getString(R.string.background_running_title), getString(R.string.background_running_desc), settings.backgroundRunning(), (b, c) -> {
            settings.setBackgroundRunning(c);
            if (c) {
                if (!Settings.canDrawOverlays(this)) startActivity(ActionManager.overlaySettingsIntent(this));
                settings.setOverlayEnabled(true);
                ActionManager.startBackground(this);
            } else {
                ActionManager.stopFloatingService(this);
            }
            refreshStatus();
        });
        addCheck(getString(R.string.desc_night_profile), "", settings.autoNightProfile(), (b, c) -> {
            if (c && !ActionManager.canScheduleExactAlarms(this)) {
                startActivity(ActionManager.exactAlarmIntent(this));
            }
            settings.putBoolean("auto_night_profile", c);
            AutoProfileScheduler.schedule(this);
        });
        addSeek(getString(R.string.night_volume), getString(R.string.unit_percent), 0, 60, settings.nightVolumePercent(), v -> settings.putInt("night_volume", v));
        addSeek(getString(R.string.night_starts), getString(R.string.unit_hour), 18, 23, settings.nightStartHour(), v -> { settings.putInt("night_start", v); AutoProfileScheduler.schedule(this); });
        addSeek(getString(R.string.night_ends), getString(R.string.unit_hour), 4, 10, settings.nightEndHour(), v -> { settings.putInt("night_end", v); AutoProfileScheduler.schedule(this); });

        content.addView(section(getString(R.string.preset_title)));
        addPresetButtons();

        content.addView(section(getString(R.string.section_settings)));
        addCheck(getString(R.string.dark_mode), getString(R.string.dark_mode_desc), settings.darkMode(), (b, c) -> { settings.setDarkMode(c); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); recreate(); });
        addActionCard(getString(R.string.training_title), v -> startActivity(new Intent(this, TrainingActivity.class)));

        content.addView(section(getString(R.string.section_about)));
        addAboutCard();

        TextView footer = text(getString(R.string.tip_tile), 13, false);
        footer.setTextColor(textSec());
        footer.setPadding(0, dp(24), 0, dp(8));
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(footer);
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).setInterpolator(ACCEL_DECEL)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private String streamModeToLabel(SettingsStore.StreamMode m) {
        if (m == SettingsStore.StreamMode.MEDIA) return getString(R.string.stream_media);
        if (m == SettingsStore.StreamMode.SYSTEM) return getString(R.string.stream_system);
        return getString(R.string.stream_active);
    }
    private String streamModeToEnum(String v) {
        if (v.equals(getString(R.string.stream_media))) return "MEDIA";
        if (v.equals(getString(R.string.stream_system))) return "SYSTEM";
        return "ACTIVE";
    }

    private String gestureModeToLabel(SettingsStore.GestureMode m) {
        if (m == SettingsStore.GestureMode.SWIPE) return getString(R.string.gesture_swipe);
        if (m == SettingsStore.GestureMode.DOUBLE_TAP) return getString(R.string.gesture_double_tap);
        return getString(R.string.gesture_both);
    }

    private String gestureModeToEnum(String v) {
        if (v.equals(getString(R.string.gesture_swipe))) return "SWIPE";
        if (v.equals(getString(R.string.gesture_double_tap))) return "DOUBLE_TAP";
        return "BOTH";
    }

    private void addColorPicker() {
        LinearLayout card = mkCard();
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

        int[] hues = {0, 30, 60, 120, 180, 240, 265, 330};
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int hue : hues) {
            Button btn = new Button(this);
            btn.setText("\u25CF");
            btn.setTextSize(18);
            btn.setAllCaps(false);
            btn.setTextColor(Color.HSVToColor(new float[]{hue, 0.65f, 0.85f}));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(24));
            bg.setColor(Color.HSVToColor(new float[]{hue, 0.3f, 0.9f}));
            btn.setBackground(bg);
            btn.setOnClickListener(v -> {
                animatePress(v);
                settings.putInt("bubble_color_hue", hue);
                colorVal.setTextColor(Color.HSVToColor(new float[]{hue, 0.65f, 0.85f}));
                restartIfRunning();
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).setInterpolator(OVERSHOOT)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
            lp.setMargins(dp(3), dp(8), dp(3), dp(8));
            btn.setLayoutParams(lp);
            row.addView(btn);
        }
        card.addView(row);
        content.addView(card);
    }

    private void addPresetButtons() {
        LinearLayout card = mkCard();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        String[] names = {getString(R.string.preset_silent), getString(R.string.preset_normal), getString(R.string.preset_vibrate)};
        int[] vals = {0, -2, -1};
        for (int i = 0; i < names.length; i++) {
            Button btn = new Button(this);
            btn.setText(names[i]);
            btn.setAllCaps(false);
            btn.setTextSize(13);
            final int target = vals[i];
            final String name = names[i];
            btn.setOnClickListener(v -> {
                animatePress(v);
                android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                if (target == -1) { new VolumeController(this, settings).muteOrRestoreMedia(); }
                else { am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target == -2 ? Math.round(max * 0.5f) : target, 0); }
                if (settings.accessibilitySpeech() && ttsReady) tts.speak(String.format(getString(R.string.preset_applied), name), TextToSpeech.QUEUE_FLUSH, null, "preset");
                v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).setInterpolator(OVERSHOOT)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(4), 0, dp(4), 0);
            btn.setLayoutParams(lp);
            row.addView(btn);
        }
        card.addView(row);
        content.addView(card);
    }

    private void addActionCard(String title, View.OnClickListener listener) {
        LinearLayout card = mkCard();
        Button btn = new Button(this);
        btn.setText(title);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(v -> { animatePress(v); listener.onClick(v); });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary());
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        card.addView(btn);
        content.addView(card);
    }

    private void addAboutCard() {
        LinearLayout card = mkCard();
        LinearLayout verRow = new LinearLayout(this);
        verRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView vl = text(getString(R.string.about_version), 13, false);
        vl.setTextColor(textSec());
        TextView vv = text("1.0.0", 13, false);
        vv.setTextColor(primary());
        verRow.addView(vl);
        verRow.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        verRow.addView(vv);
        TextView desc = text(getString(R.string.about_desc), 13, false);
        desc.setTextColor(textSec());
        desc.setLineSpacing(dp(2), 1f);
        LinearLayout linkRow = new LinearLayout(this);
        linkRow.setOrientation(LinearLayout.HORIZONTAL);
        Button rate = linkBtn(getString(R.string.about_rate));
        Button fb = linkBtn(getString(R.string.about_feedback));
        linkRow.addView(rate);
        linkRow.addView(new SpaceView(this), new LinearLayout.LayoutParams(dp(16), 1));
        linkRow.addView(fb);
        card.addView(verRow);
        card.addView(desc);
        card.addView(linkRow);
        content.addView(card);
    }

    private Button linkBtn(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setTextColor(primary());
        btn.setBackgroundColor(Color.TRANSPARENT);
        return btn;
    }

    private void restartable(String k, boolean v) { settings.putBoolean(k, v); restartIfRunning(); refreshStatus(); }
    private void restartable(String k, int v) { settings.putInt(k, v); restartIfRunning(); refreshStatus(); }
    private void restartIfRunning() { if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) ActionManager.refreshService(this); }

    private void addSpinnerDesc(String label, String desc, String[] vals, String sel, ValueSetter s) {
        LinearLayout card = mkCard();
        card.addView(text(label, 16, true));
        if (!desc.isEmpty()) { TextView d = text(desc, 12, false); d.setTextColor(textSec()); d.setLineSpacing(dp(2), 1f); d.setPadding(0, 0, 0, dp(4)); card.addView(d); }
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vals));
        for (int i = 0; i < vals.length; i++) if (vals[i].equals(sel)) sp.setSelection(i);
        sp.setOnItemSelectedListener(new SimpleSelectedListener(pos -> s.set(vals[pos])));
        card.addView(sp);
        content.addView(card);
    }

    private void addSeek(String label, String suffix, int min, int max, int cur, IntSetter s) {
        LinearLayout card = mkCard();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl = text(label, 15, true);
        TextView val = text(cur + suffix, 15, false);
        val.setTextColor(primary());
        header.addView(lbl);
        header.addView(new SpaceView(this), new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(val);
        card.addView(header);
        SeekBar sk = new SeekBar(this);
        sk.setMax(max - min);
        sk.setProgress(cur - min);
        sk.setThumbTintList(ColorStateList.valueOf(primary()));
        sk.setProgressTintList(ColorStateList.valueOf(primary()));
        sk.setPadding(dp(6), dp(4), dp(6), dp(4));
        sk.setOnSeekBarChangeListener(new SimpleSeekListener(p -> val.setText((p + min) + suffix), p -> s.set(p + min)));
        card.addView(sk);
        content.addView(card);
    }

    private void addCheck(String label, String desc, boolean checked, CompoundButton.OnCheckedChangeListener l) {
        LinearLayout card = mkCard();
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(15);
        box.setTextColor(text());
        box.setChecked(checked);
        box.setOnCheckedChangeListener(l);
        card.addView(box);
        if (!desc.isEmpty()) { TextView d = text(desc, 13, false); d.setTextColor(textSec()); d.setLineSpacing(dp(2), 1f); d.setPadding(0, dp(2), 0, 0); card.addView(d); }
        content.addView(card);
    }

    private TextView section(String label) {
        TextView v = text(label, 13, true);
        v.setAllCaps(true);
        v.setLetterSpacing(0.08f);
        v.setPadding(0, dp(28), 0, dp(6));
        v.setTextColor(textSec());
        return v;
    }

    private TextView text(String v, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(v);
        tv.setTextSize(sp);
        tv.setTextColor(text());
        if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return tv;
    }

    private Button mkButton(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); return b; }

    private void stylePrimary(Button b) {
        b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary());
        g.setCornerRadius(dp(12));
        b.setBackground(g);
    }

    private void styleSecondary(Button b) {
        b.setTextColor(primary());
        GradientDrawable g = new GradientDrawable();
        g.setStroke(dp(1), primary());
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
    }

    private LinearLayout mkCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        card.setPadding(p, dp(14), p, dp(14));
        card.setBackground(new RoundRectDrawable(cardBg(), dp(16)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) card.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);
        card.setGravity(Gravity.CENTER_VERTICAL);
        return card;
    }

    private void addSpacer(int dpv) { content.addView(new SpaceView(this), new LinearLayout.LayoutParams(1, dp(dpv))); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private boolean isServiceRunning() {
        ActivityManager m = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (m == null) return false;
        for (ActivityManager.RunningServiceInfo s : m.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingVolumeService.class.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    interface ValueSetter { void set(String v); }
    interface IntSetter { void set(int v); }
}
