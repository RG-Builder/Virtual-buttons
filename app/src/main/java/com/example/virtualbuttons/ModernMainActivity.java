package com.example.virtualbuttons;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.function.IntConsumer;

public class ModernMainActivity extends android.app.Activity {
    private SettingsStore settings;
    private LinearLayout root;
    private TextView statusText;
    private TextView statusDetail;
    private View statusDot;
    private boolean isServiceRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsStore(this);
        applyTheme();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (48 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density));
        int bgColor = settings.isDarkMode() ? 0xFF0C0C16 : 0xFFFFFBFE;
        root.setBackgroundColor(bgColor);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        setContentView(scrollView);

        buildHeaderSection();
        buildStatusSection();
        buildPermissionsSection();
        buildStartStopSection();
        buildGestureSettingsSection();
        buildReliabilitySection();
        buildNotificationSection();
        buildAppearanceSection();

        boolean showAccPrompt = getIntent().getBooleanExtra("show_accessibility_prompt", false);
        if (showAccPrompt) {
            statusText.setText("Accessibility Stopped");
            statusDetail.setText("Tap to re-enable accessibility service");
            statusDot.setBackgroundDrawable(new CircleDrawable(0xFFE65100));
            Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void applyTheme() {
        if (settings.isDarkMode()) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void buildHeaderSection() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        header.setPadding(0, 0, 0, (int) (24 * getResources().getDisplayMetrics().density));

        ImageView icon = new ImageView(this);
        int iconSize = (int) (56 * getResources().getDisplayMetrics().density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        icon.setImageResource(R.drawable.ic_volume);
        icon.setColorFilter(settings.isDarkMode() ? 0xFFD0BCFF : 0xFF6750A4);
        icon.setPadding((int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density));
        int iconBg = settings.isDarkMode() ? 0xFF4F378B : 0xFFEADDFF;
        icon.setBackgroundDrawable(new CircleDrawable(iconBg));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("Virtual Buttons");
        title.setTextSize(22);
        int titleColor = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        title.setTextColor(titleColor);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("Gesture Controls  v2.0");
        int subColor = settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E;
        subtitle.setTextColor(subColor);
        subtitle.setTextSize(14);

        textCol.addView(title);
        textCol.addView(subtitle);
        header.addView(icon);
        header.addView(textCol);
        root.addView(header);
    }

    private void buildStatusSection() {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int cardMargin = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, cardMargin);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density));

        statusDot = new View(this);
        int dotSize = (int) (12 * getResources().getDisplayMetrics().density);
        statusDot.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
        statusDot.setBackgroundDrawable(new CircleDrawable(0xFFB3261E));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding((int) (12 * getResources().getDisplayMetrics().density), 0, 0, 0);

        statusText = new TextView(this);
        statusText.setText("Service Status");
        int onSurface = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        statusText.setTextColor(onSurface);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setTextSize(16);

        statusDetail = new TextView(this);
        statusDetail.setText("Checking...");
        int textSec = settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E;
        statusDetail.setTextColor(textSec);
        statusDetail.setTextSize(13);

        textCol.addView(statusText);
        textCol.addView(statusDetail);
        row.addView(statusDot);
        row.addView(textCol);
        card.addView(row);

        TextView oemLabel = new TextView(this);
        oemLabel.setText("Device: " + BatteryOptimizationHelper.getOemName());
        oemLabel.setTextSize(11);
        oemLabel.setTextColor(settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E);
        oemLabel.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            0, (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density));
        card.addView(oemLabel);

        root.addView(card);
    }

    private void buildPermissionsSection() {
        addSectionHeader("Permissions & Setup");

        addPermissionItem("Overlay Permission",
            "Required to show controls over other apps",
            Settings.canDrawOverlays(this),
            () -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));

        addPermissionItem("Accessibility Service",
            "Required for lock screen, screenshot, system actions",
            isAccessibilityServiceEnabled(),
            () -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addPermissionItem("Write Settings",
                "Required for brightness control",
                Settings.System.canWrite(this),
                () -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                });
        }

        addPermissionItem("Battery Optimization",
            "Disable to keep service running reliably",
            isBatteryOptimizationDisabled(),
            () -> BatteryOptimizationHelper.openBatterySettings(this));

        if (!isBatteryOptimizationDisabled() && !settings.isBatteryOptDismissed()) {
            TextView oemGuide = new TextView(this);
            oemGuide.setText(BatteryOptimizationHelper.getOemBatteryGuidance(this));
            oemGuide.setTextSize(11);
            int textSec2 = settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E;
            oemGuide.setTextColor(textSec2);
            oemGuide.setPadding((int) (32 * getResources().getDisplayMetrics().density),
                (int) (4 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density));
            root.addView(oemGuide);
        }
    }

    private void buildStartStopSection() {
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        LinearLayout card = createCard(cardBg, margin);

        Button startBtn = new Button(new ContextThemeWrapper(this,
            settings.isDarkMode() ? android.R.style.Theme_Material :
                android.R.style.Theme_Material_Light));
        startBtn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (52 * getResources().getDisplayMetrics().density)));
        startBtn.setText("Start Gesture Controls");
        startBtn.setTextSize(16);
        startBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        startBtn.setTextColor(0xFFFFFFFF);
        startBtn.setBackgroundColor(0xFF6750A4);
        startBtn.setOnClickListener(v -> toggleService());

        card.addView(startBtn);
        root.addView(card);
    }

    private void buildGestureSettingsSection() {
        addSectionHeader("Gesture Controls");

        addSeekbarSetting("Sensitivity", settings.getGestureSensitivity(),
            v -> { settings.setGestureSensitivity(v); updateStatus(); }, 10, 100);
        addSeekbarSetting("Edge Zone Width", settings.getEdgeWidth(),
            v -> settings.setEdgeWidth(v), 8, 48);
        addSeekbarSetting("Cooldown (ms)", settings.getCooldownMs(),
            v -> settings.setCooldownMs(v), 50, 600);

        addSwitchSetting("Edge Swipe (Volume)", settings.isEdgeGesturesEnabled(),
            (b, c) -> settings.setEdgeGesturesEnabled(c));
        addSwitchSetting("Edge Drag (Brightness)", true,
            (b, c) -> {});
        addSwitchSetting("Double Tap to Lock", settings.isDoubleTapLockEnabled(),
            (b, c) -> settings.setDoubleTapLockEnabled(c));
        addSwitchSetting("Corner Gestures", settings.isCornerGesturesEnabled(),
            (b, c) -> settings.setCornerGesturesEnabled(c));
        addSwitchSetting("Radial Quick Menu", settings.isRadialMenuEnabled(),
            (b, c) -> settings.setRadialMenuEnabled(c));
        addSwitchSetting("Two-Finger Swipe (Media)", settings.isTwoFingerMediaEnabled(),
            (b, c) -> settings.setTwoFingerMediaEnabled(c));
        addSwitchSetting("Show Edge Indicators", settings.isShowIndicators(),
            (b, c) -> settings.setShowIndicators(c));
        addSwitchSetting("Auto Start on Boot", settings.isAutoStart(),
            (b, c) -> settings.setAutoStart(c));
    }

    private void buildReliabilitySection() {
        addSectionHeader("Reliability");

        addSwitchSetting("Auto-Restart on Crash",
            settings.isAutoRestartEnabled(),
            (b, c) -> settings.setAutoRestartEnabled(c));
        addDetailText("Restarts gesture service if the app crashes or is killed.");

        addSwitchSetting("Service Watchdog",
            settings.isWatchdogEnabled(),
            (b, c) -> settings.setWatchdogEnabled(c));
        addDetailText("Monitors service health and restarts if unresponsive.");

        addSwitchSetting("Accessibility Monitor",
            settings.isAccMonitoringEnabled(),
            (b, c) -> settings.setAccMonitoringEnabled(c));
        addDetailText("Detects if accessibility service stops unexpectedly.");

        addSwitchSetting("Enhanced Service Protection",
            settings.isServiceProtectionEnabled(),
            (b, c) -> settings.setServiceProtectionEnabled(c));
        addDetailText("START_STICKY_COMPATIBILITY for better OEM reliability.");

        addButtonSetting("Battery Optimization Guide",
            v -> BatteryOptimizationHelper.openBatterySettings(this));

        String oemName = BatteryOptimizationHelper.getOemName();
        addDetailText("Detected: " + oemName + ". " +
            BatteryOptimizationHelper.getOemBatteryGuidance(this));

        int restartAttempts = ServiceWatchdog.getRestartAttempts();
        if (restartAttempts > 0) {
            addDetailText("Auto-restarts since install: " + restartAttempts);
        }
    }

    private void buildNotificationSection() {
        addSectionHeader("Notification");

        addSelectSetting("Notification Mode",
            getNotifModeLabel(settings.getNotificationMode()),
            () -> cycleNotificationMode());

        addDetailText("Normal: Shows status. Minimal: Short text. Stealth: Barely visible.");
    }

    private void buildAppearanceSection() {
        addSectionHeader("Appearance");
        addSwitchSetting("Dark Mode", settings.isDarkMode(),
            (b, c) -> { settings.setDarkMode(c); recreate(); });
        addSeekbarSetting("Pill Opacity", settings.getPillOpacity(),
            v -> settings.setPillOpacity(v), 20, 100);
        addSeekbarSetting("Pill Size", settings.getPillSize(),
            v -> settings.setPillSize(v), 1, 3);
        addSeekbarSetting("Haptic Intensity", settings.getHapticIntensity(),
            v -> settings.setHapticIntensity(v), 0, 100);
    }

    private String getNotifModeLabel(int mode) {
        switch (mode) {
            case SettingsStore.NOTIF_MODE_STEALTH: return "Stealth";
            case SettingsStore.NOTIF_MODE_MINIMAL: return "Minimal";
            default: return "Normal";
        }
    }

    private void cycleNotificationMode() {
        int current = settings.getNotificationMode();
        int next = (current + 1) % 3;
        settings.setNotificationMode(next);
        rebuildView();
    }

    private void rebuildView() {
        root.removeAllViews();
        buildHeaderSection();
        buildStatusSection();
        buildPermissionsSection();
        buildStartStopSection();
        buildGestureSettingsSection();
        buildReliabilitySection();
        buildNotificationSection();
        buildAppearanceSection();
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(13);
        int primary = settings.isDarkMode() ? 0xFFD0BCFF : 0xFF6750A4;
        header.setTextColor(primary);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, (int) (24 * getResources().getDisplayMetrics().density),
            0, (int) (8 * getResources().getDisplayMetrics().density));
        root.addView(header);
    }

    private LinearLayout createCard(int bgColor, int marginBottom) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(bgColor);
        int r = (int) (12 * getResources().getDisplayMetrics().density);
        card.setBackgroundDrawable(new RoundRectDrawable(bgColor, r));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, marginBottom);
        card.setLayoutParams(lp);
        card.setElevation(2 * getResources().getDisplayMetrics().density);
        return card;
    }

    private void addDetailText(String text) {
        TextView detail = new TextView(this);
        detail.setText(text);
        detail.setTextSize(11);
        int textSec = settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E;
        detail.setTextColor(textSec);
        detail.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (2 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density));
        root.addView(detail);
    }

    private void addPermissionItem(String title, String description,
                                    boolean granted, Runnable onClick) {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, margin);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density));
        row.setClickable(true);
        row.setOnClickListener(v -> onClick.run());

        TextView dot = new TextView(this);
        dot.setText(granted ? "\u2713" : "\u2715");
        dot.setTextSize(16);
        dot.setTypeface(null, android.graphics.Typeface.BOLD);
        dot.setTextColor(granted ? 0xFF1B5E20 : 0xFFB3261E);
        dot.setLayoutParams(new LinearLayout.LayoutParams(
            (int) (32 * getResources().getDisplayMetrics().density),
            LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        int onSurface = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        t.setTextColor(onSurface);
        t.setTextSize(15);

        TextView d = new TextView(this);
        d.setText(description);
        int textSec = settings.isDarkMode() ? 0xFFA09EA8 : 0xFF79747E;
        d.setTextColor(textSec);
        d.setTextSize(12);

        textCol.addView(t);
        textCol.addView(d);
        row.addView(dot);
        row.addView(textCol);
        card.addView(row);
        root.addView(card);
    }

    private void addSwitchSetting(String title, boolean checked,
                                   CompoundButton.OnCheckedChangeListener listener) {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, margin);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        int onSurface = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        t.setTextColor(onSurface);
        t.setTextSize(15);
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);

        row.addView(t);
        row.addView(sw);
        card.addView(row);
        root.addView(card);
    }

    private void addSeekbarSetting(String title, int value,
                                    IntConsumer onChanged, int min, int max) {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, margin);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        int onSurface = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        t.setTextColor(onSurface);
        t.setTextSize(14);
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        SeekBar seek = new SeekBar(this);
        seek.setLayoutParams(new LinearLayout.LayoutParams(
            (int) (120 * getResources().getDisplayMetrics().density),
            LinearLayout.LayoutParams.WRAP_CONTENT));
        seek.setMax(max - min);
        seek.setProgress(value - min);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser) onChanged.accept(p + min);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        row.addView(t);
        row.addView(seek);
        card.addView(row);
        root.addView(card);
    }

    private void addSelectSetting(String title, String currentValue, Runnable onClick) {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, margin);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding((int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density));
        row.setClickable(true);
        row.setOnClickListener(v -> onClick.run());

        TextView t = new TextView(this);
        t.setText(title);
        int onSurface = settings.isDarkMode() ? 0xFFE6E1F0 : 0xFF1D1B20;
        t.setTextColor(onSurface);
        t.setTextSize(15);
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView value = new TextView(this);
        value.setText(currentValue);
        int primary = settings.isDarkMode() ? 0xFFD0BCFF : 0xFF6750A4;
        value.setTextColor(primary);
        value.setTextSize(14);
        value.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(t);
        row.addView(value);
        card.addView(row);
        root.addView(card);
    }

    private void addButtonSetting(String title, View.OnClickListener onClick) {
        int cardBg = settings.isDarkMode() ? 0xFF282836 : 0xFFFFFFFF;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout card = createCard(cardBg, margin);

        Button btn = new Button(new ContextThemeWrapper(this,
            settings.isDarkMode() ? android.R.style.Theme_Material :
                android.R.style.Theme_Material_Light));
        btn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (44 * getResources().getDisplayMetrics().density)));
        btn.setText(title);
        btn.setTextSize(14);
        btn.setTextColor(0xFF6750A4);
        btn.setBackgroundColor(0x00FFFFFF);
        btn.setOnClickListener(onClick);

        card.addView(btn);
        root.addView(card);
    }

    private void updateStatus() {
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean accessibilityOn = isAccessibilityServiceEnabled();
        boolean canWrite = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canWrite = Settings.System.canWrite(this);
        }
        boolean batteryOk = isBatteryOptimizationDisabled();
        isServiceRunning = overlayGranted && accessibilityOn && canWrite;

        if (isServiceRunning) {
            statusText.setText("Service Ready");
            String detail = "All permissions granted. ";
            if (!batteryOk) detail += "Battery optimization still enabled.";
            else detail += "Start gesture controls.";
            statusDetail.setText(detail);
            statusDot.setBackgroundDrawable(new CircleDrawable(0xFF1B5E20));
        } else {
            statusText.setText("Setup Required");
            StringBuilder sb = new StringBuilder("Needs: ");
            if (!overlayGranted) sb.append("Overlay, ");
            if (!accessibilityOn) sb.append("Accessibility, ");
            if (!canWrite) sb.append("Write Settings, ");
            if (sb.length() > 6) sb.setLength(sb.length() - 2);
            statusDetail.setText(sb.toString());
            statusDot.setBackgroundDrawable(new CircleDrawable(0xFFB3261E));
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager)
            getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices =
            am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getId().contains(getPackageName())) return true;
        }
        return false;
    }

    private boolean isBatteryOptimizationDisabled() {
        return BatteryOptimizationHelper.isIgnoringOptimizations(this);
    }

    private void toggleService() {
        if (isServiceRunning) {
            Intent intent = new Intent(this, GestureForegroundService.class);
            stopService(intent);
            isServiceRunning = false;
            updateStatus();
        } else {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
            Intent intent = new Intent(this, GestureForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            isServiceRunning = true;
            updateStatus();
        }
    }
}
