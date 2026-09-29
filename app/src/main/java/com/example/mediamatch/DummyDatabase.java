package com.example.mediamatch;

import android.content.Context;
import android.content.SharedPreferences;

public class DummyDatabase {
    private static final String DB = "kueditin_dummy_db";

    public static void saveClient(Context context, String name, String email, String phone, String password, String needs) {
        SharedPreferences.Editor editor = context.getSharedPreferences(DB, Context.MODE_PRIVATE).edit();
        editor.putString("role", "client");
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("phone", phone);
        editor.putString("password", password);
        editor.putString("needs", needs);
        editor.putBoolean("is_registered", true);
        editor.apply();
    }

    public static boolean isClientRegistered(Context context) {
        SharedPreferences pref = context.getSharedPreferences(DB, Context.MODE_PRIVATE);
        return pref.getBoolean("is_registered", false) && "client".equals(pref.getString("role", ""));
    }

    public static boolean loginClient(Context context, String email, String password) {
        SharedPreferences pref = context.getSharedPreferences(DB, Context.MODE_PRIVATE);
        return isClientRegistered(context)
                && email.equals(pref.getString("email", ""))
                && password.equals(pref.getString("password", ""));
    }

    public static String getName(Context context) {
        return context.getSharedPreferences(DB, Context.MODE_PRIVATE).getString("name", "Klien");
    }

    public static String getEmail(Context context) {
        return context.getSharedPreferences(DB, Context.MODE_PRIVATE).getString("email", "-");
    }

    public static String getPhone(Context context) {
        return context.getSharedPreferences(DB, Context.MODE_PRIVATE).getString("phone", "-");
    }

    public static String getNeeds(Context context) {
        return context.getSharedPreferences(DB, Context.MODE_PRIVATE).getString("needs", "-");
    }
}
