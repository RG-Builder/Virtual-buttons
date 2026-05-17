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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class ModernMainActivity extends Activity implements TextToSpeech.OnInitListener {
    private SettingsStore settings;
    private LinearLayout container;
    private boolean darkMode;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private LinearLayout statusCard;
    private ImageView statusIcon;

    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator(1.5f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.5f);
    private static final AccelerateDecelerateInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();

    @Override protected void onCreate(Bundle savedInstanceState) {
        try {
            settings = new SettingsStore(this);
            darkMode = settings.darkMode();
            if (darkMode) setTheme(R.style.AppTheme_Dark);
            else setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
            tts = new TextToSpeech(this, this);
            buildModernUI();
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            showStartupError(e);
        }
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.US); ttsReady = true; }
    }

    @Override protected void onResume() { super.onResume(); updateStatus(); }

    @Override protected void onDestroy() { if (tts != null) tts.shutdown(); super.onDestroy(); }

    private int primary() { return darkMode ? getColor(R.color.vb_primary_dark) : getColor(R.color.vb_primary); }
    private int bg() { return darkMode ? getColor(R.color.vb_bg_dark) : getColor(R.color.vb_surface); }
    private int cardBg() { return darkMode ? getColor(R.color.vb_card_dark) : Color.WHITE; }
    private int text() { return darkMode ? getColor(R.color.vb_on_surface_dark) : getColor(R.color.vb_on_surface); }
    private int textSec() { return darkMode ? getColor(R.color.vb_text_sec_dark) : getColor(R.color.vb_outline); }
    private int accent() { return darkMode ? getColor(R.color.vb_accent_dark) : getColor(R.color.vb_accent); }

    private void buildModernUI() {
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(bg());

        addHeader();
        addStatusCard();
        addActionButtons();
        addSettingsSections();

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);
        scrollView.setVerticalScrollBarEnabled(false);
        setContentView(scrollView);
    }

    private void addHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(24), dp(40), dp(24), dp(16));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_foreground);
        logo.setColorFilter(primary());
        logo.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titleArea = new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.VERTICAL);
        titleArea.setPadding(dp(16), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("Virtual Buttons");
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(text());

        TextView subtitle = new TextView(this);
        subtitle.setText("Control your device effortlessly");
        subtitle.setTextSize(14);
        subtitle.setTextColor(textSec());

        titleArea.addView(title);
        titleArea.addView(subtitle);

        header.addView(logo);
        header.addView(titleArea);
        container.addView(header);
    }

    private void addStatusCard() {
        statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(20), dp(16), dp(20), dp(16));
        statusCard.setBackground(createCardBg());

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        statusIcon = new ImageView(this);
        statusIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView statusTitle = new TextView(this);
        statusTitle.setTextSize(18);
        statusTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        statusTitle.setTextColor(text());
        statusTitle.setId(View.generateViewId());

        TextView statusDesc = new TextView(this);
        statusDesc.setTextSize(14);
        statusDesc.setTextColor(textSec());
        statusDesc.setId(View.generateViewId());
        statusDesc.setPadding(0, dp(4), 0, 0);

        statusRow.addView(statusIcon);
        statusRow.addView(createSpacer(16, 0));
        statusRow.addView(titleView(statusTitle));

        statusCard.addView(statusRow);
        statusCard.addView(statusDesc);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.setMargins(dp(20), 0, dp(20), dp(8));
        container.addView(statusCard, statusLp);

        updateStatus();
    }

    private void addActionButtons() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(20), dp(16), dp(20), dp(8));

        Button startBtn = createActionButton("Start All", true);
        startBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startAllServices();
        });

        Button stopBtn = createActionButton("Stop", false);
        stopBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            stopAllServices();
        });

        actions.addView(startBtn, new LinearLayout.LayoutParams(0, dp(52), 1f));
        actions.addView(createSpacer(12, 0));
        actions.addView(stopBtn, new LinearLayout.LayoutParams(0, dp(52), 1f));

        container.addView(actions);
    }

    private Button createActionButton(String text, boolean primary) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(16);
        btn.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (primary) {
            bg.setColor(primary());
            btn.setTextColor(Color.WHITE);
        } else {
            bg.setStroke(dp(2), primary());
            btn.setTextColor(primary());
            bg.setColor(Color.TRANSPARENT);
        }
        btn.setBackground(bg);
        btn.setElevation(0);
        return btn;
    }

    private void addSettingsSections() {
        addSection("Button Panel", new String[]{"Show Panel", "Compact Mode"});
        addSection("Edge Gestures", new String[]{"Enable Edge Gestures"});
        addSection("Volume", new String[]{"Visual Indicator", "Haptic Feedback"});
        addSection("Reliability", new String[]{"Start on Boot", "Background Running"});

        addButtonTogglesSection();
        addSlidersSection();

        addAboutSection();
    }

    private void addSection(String title, String[] items) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(title.toUpperCase());
        sectionTitle.setTextSize(12);
        sectionTitle.setLetterSpacing(0.1f);
        sectionTitle.setTextColor(textSec());
        sectionTitle.setPadding(dp(20), dp(24), dp(20), dp(8));
        container.addView(sectionTitle);

        LinearLayout card = createSettingsCard();
        for (String item : items) {
            CheckBox cb = new CheckBox(this);
            cb.setText(item);
            cb.setTextSize(15);
            cb.setTextColor(text());
            cb.setChecked(isToggleEnabled(item));
            cb.setPadding(dp(16), dp(12), dp(16), dp(12));
            cb.setOnCheckedChangeListener((b, c) -> {
                b.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                handleToggle(item, c);
            });
            card.addView(cb);
        }
        container.addView(card);
    }

    private void addButtonTogglesSection() {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("BUTTONS");
        sectionTitle.setTextSize(12);
        sectionTitle.setLetterSpacing(0.1f);
        sectionTitle.setTextColor(textSec());
        sectionTitle.setPadding(dp(20), dp(24), dp(20), dp(8));
        container.addView(sectionTitle);

        LinearLayout card = createSettingsCard();

        String[] buttons = {"Power", "Volume Up", "Volume Down", "Home", "Recents", "Back"};
        SettingsStore.ButtonType[] types = {
            SettingsStore.ButtonType.POWER, SettingsStore.ButtonType.VOLUME_UP,
            SettingsStore.ButtonType.VOLUME_DOWN, SettingsStore.ButtonType.HOME,
            SettingsStore.ButtonType.RECENTS, SettingsStore.ButtonType.BACK
        };

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(3);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            CheckBox cb = new CheckBox(this);
            cb.setText(buttons[i]);
            cb.setTextSize(14);
            cb.setTextColor(text());
            cb.setChecked(settings.buttonEnabled(types[i]));
            cb.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            cb.setOnCheckedChangeListener((b, c) -> {
                settings.setButtonEnabled(types[idx], c);
                refreshServices();
            });
            row1.addView(cb);
        }
        card.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(3);
        for (int i = 3; i < 6; i++) {
            final int idx = i;
            CheckBox cb = new CheckBox(this);
            cb.setText(buttons[i]);
            cb.setTextSize(14);
            cb.setTextColor(text());
            cb.setChecked(settings.buttonEnabled(types[i]));
            cb.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            cb.setOnCheckedChangeListener((b, c) -> {
                settings.setButtonEnabled(types[idx], c);
                refreshServices();
            });
            row2.addView(cb);
        }
        card.addView(row2);

        container.addView(card);
    }

    private void addSlidersSection() {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("CUSTOMIZATION");
        sectionTitle.setTextSize(12);
        sectionTitle.setLetterSpacing(0.1f);
        sectionTitle.setTextColor(textSec());
        sectionTitle.setPadding(dp(20), dp(24), dp(20), dp(8));
        container.addView(sectionTitle);

        LinearLayout card = createSettingsCard();
        card.addView(createSlider("Sensitivity", settings.globalGestureSensitivity(), 10, 90, v -> {
            settings.setGlobalGestureSensitivity(v);
            refreshServices();
        }));
        card.addView(createSlider("Panel Size", settings.buttonPanelSize(), 40, 80, v -> {
            settings.setButtonPanelSize(v);
            refreshServices();
        }));
        card.addView(createSlider("Panel Opacity", settings.buttonPanelOpacity(), 40, 100, v -> {
            settings.setButtonPanelOpacity(v);
            refreshServices();
        }));
        card.addView(createSlider("Volume Step", settings.volumeStep(), 1, 5, v -> settings.putInt("volume_step", v)));
        container.addView(card);
    }

    private LinearLayout createSlider(String label, int value, int min, int max, SliderCallback callback) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(15);
        labelView.setTextColor(text());

        TextView valueView = new TextView(this);
        valueView.setText(String.valueOf(value));
        valueView.setTextSize(15);
        valueView.setTextColor(accent());

        header.addView(labelView);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
        header.addView(createSpacer(0, 0), sp);
        header.addView(valueView);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(value - min);
        seekBar.setThumbTintList(ColorStateList.valueOf(primary()));
        seekBar.setProgressTintList(ColorStateList.valueOf(primary()));
        seekBar.setOnSeekBarChangeListener(new SimpleSeekListener(p -> {
            int newVal = p + min;
            valueView.setText(String.valueOf(newVal));
            callback.onChange(newVal);
        }));

        layout.addView(header);
        layout.addView(seekBar);
        return layout;
    }

    private void addAboutSection() {
        TextView version = new TextView(this);
        version.setText("Version 2.0.0");
        version.setTextSize(13);
        version.setTextColor(textSec());
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(32), 0, dp(16));
        container.addView(version);
    }

    private LinearLayout createSettingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardBg());
        card.setPadding(0, dp(8), 0, dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(20), 0, dp(20), dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private GradientDrawable createCardBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(cardBg());
        return bg;
    }

    private View titleView(TextView tv) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(tv);
        return ll;
    }

    private View createSpacer(int w, int h) {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(w), dp(h));
        v.setLayoutParams(lp);
        return v;
    }

    private void updateStatus() {
        if (statusCard == null) return;

        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean running = isServiceRunning();

        int bgColor, iconTint;
        String title, desc;
        int iconRes;

        if (!overlayGranted) {
            bgColor = getColor(R.color.vb_status_error_bg);
            iconTint = getColor(R.color.vb_error);
            title = "Permission Required";
            desc = "Grant overlay permission to enable virtual buttons";
            iconRes = R.drawable.ic_volume;
        } else if (running) {
            bgColor = getColor(R.color.vb_status_success_bg);
            iconTint = getColor(R.color.vb_success);
            title = "Active";
            desc = "All buttons and gestures are ready";
            iconRes = R.drawable.ic_volume;
        } else {
            bgColor = getColor(R.color.vb_status_warning_bg);
            iconTint = getColor(R.color.vb_warning);
            title = "Ready to Start";
            desc = "Tap 'Start All' to activate virtual buttons";
            iconRes = R.drawable.ic_volume;
        }

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(bgColor);
        statusCard.setBackground(cardBg);

        if (statusIcon != null) {
            statusIcon.setImageResource(iconRes);
            statusIcon.setColorFilter(iconTint);
        }

        LinearLayout row = (LinearLayout) statusCard.getChildAt(0);
        if (row.getChildCount() > 2) {
            TextView tv = (TextView) ((LinearLayout) row.getChildAt(2)).getChildAt(0);
            tv.setText(title);
        }

        TextView descView = (TextView) statusCard.getChildAt(1);
        if (descView != null) descView.setText(desc);
    }

    private boolean isServiceRunning() {
        ActivityManager m = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (m == null) return false;
        for (ActivityManager.RunningServiceInfo s : m.getRunningServices(Integer.MAX_VALUE)) {
            String name = s.service.getClassName();
            if (name.contains("ButtonPanelService") || name.contains("FloatingVolumeService") || name.contains("EnhancedGestureService")) {
                return true;
            }
        }
        return false;
    }

    private void startAllServices() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(ActionManager.overlaySettingsIntent(this));
            return;
        }
        settings.setOverlayEnabled(true);
        boolean started = ActionManager.refreshAllServices(this);
        if (started) animateSuccess();
        updateStatus();
    }

    private void stopAllServices() {
        settings.setOverlayEnabled(false);
        ActionManager.stopFloatingService(this);
        ActionManager.stopButtonPanelService(this);
        ActionManager.stopEnhancedGestureService(this);
        updateStatus();
    }

    private void refreshServices() {
        if (settings.overlayEnabled() && Settings.canDrawOverlays(this)) {
            ActionManager.refreshAllServices(this);
        }
    }

    private boolean isToggleEnabled(String item) {
        switch (item) {
            case "Show Panel": return settings.showButtonPanel();
            case "Compact Mode": return settings.compactMode();
            case "Enable Edge Gestures": return settings.edgeGestures();
            case "Visual Indicator": return settings.visualIndicator();
            case "Haptic Feedback": return settings.haptics();
            case "Start on Boot": return settings.startOnBoot();
            case "Background Running": return settings.backgroundRunning();
            default: return false;
        }
    }

    private void handleToggle(String item, boolean enabled) {
        switch (item) {
            case "Show Panel": settings.setShowButtonPanel(enabled); break;
            case "Compact Mode": settings.setCompactMode(enabled); break;
            case "Enable Edge Gestures": settings.putBoolean("edge_gestures", enabled); break;
            case "Visual Indicator": settings.putBoolean("visual_indicator", enabled); break;
            case "Haptic Feedback": settings.setHapticFeedback(enabled); break;
            case "Start on Boot": settings.putBoolean("start_on_boot", enabled); break;
            case "Background Running": settings.setBackgroundRunning(enabled); break;
        }
        refreshServices();
    }

    private void animateSuccess() {
        View root = getWindow().getDecorView();
        root.setAlpha(0.8f);
        root.animate().alpha(1f).setDuration(300).start();
    }

    private void showStartupError(Throwable error) {
        TextView message = new TextView(this);
        message.setText("Virtual Buttons could not load. Try clearing app data or reinstalling.\n\n" + error.getClass().getSimpleName());
        message.setTextSize(16);
        message.setTextColor(Color.WHITE);
        message.setPadding(dp(24), dp(48), dp(24), dp(24));
        message.setBackgroundColor(Color.rgb(80, 20, 20));
        setContentView(message);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface SliderCallback { void onChange(int value); }
}