package com.example.mediamatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LanguageUtil {
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANG = "selected_language";

    public static void setLocale(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, langCode).apply();
        updateResources(context, langCode);
    }

    public static String getLang(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANG, "in");
    }

    public static void updateResources(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        context.createConfigurationContext(config);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static void init(Context context) {
        updateResources(context, getLang(context));
    }
}
