package com.example.virtualbuttons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

public class BatteryOptimizationHelper {
    private static final String TAG = "BatteryOptHelper";

    public static boolean isIgnoringOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    public static void openBatterySettings(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = getOemBatteryIntent(context, manufacturer);
        if (intent != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            } catch (Exception e) {
                Log.w(TAG, "OEM intent failed, falling back", e);
            }
        }
        Intent fallback = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        fallback.setData(Uri.parse("package:" + context.getPackageName()));
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(fallback);
    }

    private static Intent getOemBatteryIntent(Context context, String manufacturer) {
        String pkg = context.getPackageName();

        switch (manufacturer) {
            case "xiaomi":
            case "redmi":
            case "poco":
                Intent mi = new Intent();
                mi.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                return mi;

            case "oppo":
            case "realme":
            case "oneplus":
                Intent op = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                op.setData(Uri.parse("package:" + pkg));
                return op;

            case "vivo":
                Intent vi = new Intent();
                vi.setComponent(new ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.addwhite.AdWhiteListActivity"));
                return vi;

            case "huawei":
            case "honor":
                Intent hu = new Intent();
                hu.setComponent(new ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                return hu;

            case "samsung":
                Intent sa = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                sa.setData(Uri.parse("package:" + pkg));
                return sa;

            case "meizu":
                Intent me = new Intent("com.meizu.safe.security.SHOW_APPSEC");
                me.putExtra("packageName", pkg);
                me.setComponent(new ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.security.AppSecActivity"));
                return me;

            case "lenovo":
                Intent le = new Intent();
                le.setComponent(new ComponentName(
                    "com.lenovo.powersetting",
                    "com.lenovo.powersetting.ui.PowerSettingActivity"));
                return le;

            default:
                Intent def = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                def.setData(Uri.parse("package:" + pkg));
                return def;
        }
    }

    public static String getOemName() {
        String mfr = Build.MANUFACTURER;
        if (mfr == null) return "Unknown";

        String lower = mfr.toLowerCase();
        if (lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco"))
            return "Xiaomi/HyperOS";
        if (lower.contains("oppo") || lower.contains("realme") || lower.contains("oneplus"))
            return "Oppo/Realme/OnePlus";
        if (lower.contains("vivo")) return "Vivo";
        if (lower.contains("huawei") || lower.contains("honor")) return "Huawei/Honor";
        if (lower.contains("samsung")) return "Samsung OneUI";
        if (lower.contains("meizu")) return "Meizu";
        if (lower.contains("lenovo")) return "Lenovo";
        if (lower.contains("google") || lower.contains("pixel")) return "Google Pixel";
        return mfr;
    }

    public static String getOemBatteryGuidance(Context context) {
        String mfr = Build.MANUFACTURER.toLowerCase();

        if (mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco")) {
            return "MIUI / HyperOS — Go to Settings > Apps > Manage Apps > Virtual Buttons > " +
                   "Battery Saver > No restrictions. Also enable Autostart.";
        } else if (mfr.contains("oppo") || mfr.contains("oneplus") || mfr.contains("realme")) {
            return "ColorOS / OxygenOS — Open Settings > Battery > App Battery Management > " +
                   "Virtual Buttons > Allow background activity. Also lock the app in Recents.";
        } else if (mfr.contains("vivo")) {
            return "Funtouch OS — Go to Settings > Battery > Background App Management > " +
                   "Virtual Buttons > Allow background activity.";
        } else if (mfr.contains("huawei") || mfr.contains("honor")) {
            return "EMUI / HarmonyOS — Open Settings > Battery > App Launch > Virtual Buttons > " +
                   "Manage manually > Allow background activity (all 3 toggles ON).";
        } else if (mfr.contains("samsung")) {
            return "One UI — Go to Settings > Apps > Virtual Buttons > Battery > " +
                   "Unrestricted. Also put the app in the Never Sleeping Apps list.";
        } else if (mfr.contains("meizu")) {
            return "Flyme — Open Security Center > Permissions > Autostart and enable Virtual Buttons.";
        } else if (mfr.contains("lenovo")) {
            return "ZUI — Open Settings > Battery > Background app management > " +
                   "select Virtual Buttons and turn off background restriction.";
        } else {
            return "Go to Settings > Apps > Virtual Buttons > Battery > Unrestricted. " +
                   "If your device has a 'Protected apps' or 'Auto-start' list, add this app.";
        }
    }
}
