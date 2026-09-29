package com.example.mediamatch;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtil {

    public static void initTheme(Context context) {
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String theme = pref.getString("theme_mode", "light");

        int mode;
        if ("dark".equalsIgnoreCase(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if ("system".equalsIgnoreCase(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    public static void applyTheme(Context context) {
        // Safe no-op during activity onCreate to prevent mid-flight recreation abortion
    }

    public static String getThemeName(Context context) {
        if (context == null) return "Mode Terang (Light Mode)";
        SharedPreferences pref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String theme = pref.getString("theme_mode", "light");

        if ("dark".equalsIgnoreCase(theme)) {
            return "Mode Gelap (Dark Mode)";
        } else if ("system".equalsIgnoreCase(theme)) {
            return "Ikuti Default Sistem HP";
        } else {
            return "Mode Terang (Light Mode)";
        }
    }

    public static void setTheme(Context context, String themeMode) {
        if (context == null) return;
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("theme_mode", themeMode)
                .apply();

        int mode = "dark".equalsIgnoreCase(themeMode) ? AppCompatDelegate.MODE_NIGHT_YES :
                   ("system".equalsIgnoreCase(themeMode) ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_NO);

        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void toggleTheme(android.app.Activity activity) {
        if (activity == null) return;
        SharedPreferences pref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String current = pref.getString("theme_mode", "light");
        String nextTheme = "dark".equalsIgnoreCase(current) ? "light" : "dark";

        setTheme(activity, nextTheme);
    }
}
