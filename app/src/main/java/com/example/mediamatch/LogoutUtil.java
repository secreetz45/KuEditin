package com.example.mediamatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutUtil {
    private static final String PREF_AUTH = "mediamatch_auth";
    private static final String KEY_MANUAL_LOGOUT = "manual_logout";

    public static void logout(Activity activity) {
        if (activity == null) return;

        Context appContext = activity.getApplicationContext();
        appContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MANUAL_LOGOUT, true)
                .apply();
        appContext.getSharedPreferences("user_role_pref", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        try { AppNotificationUtil.stop(); } catch (Exception ignored) {}
        try { PresenceUtil.forceOffline(); } catch (Exception ignored) {}
        
        try {
            FirebaseAuth fAuth = FirebaseAuth.getInstance();
            if (fAuth.getCurrentUser() != null) {
                fAuth.signOut();
            }
        } catch (Exception ignored) {}

        try {
            int id = appContext.getResources().getIdentifier("default_web_client_id", "string", appContext.getPackageName());
            if (id != 0) {
                String clientId = appContext.getString(id);
                if (clientId != null && !clientId.trim().isEmpty() && !clientId.startsWith("PASTE_WEB_CLIENT_ID_HERE")) {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(clientId)
                            .requestEmail()
                            .build();
                    GoogleSignIn.getClient(appContext, gso).signOut();
                }
            }
        } catch (Exception ignored) {}

        Intent i = new Intent(activity, MainActivity.class);
        i.putExtra("from_logout", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
        activity.overridePendingTransition(R.anim.no_anim, R.anim.no_anim);
    }

    public static boolean isManualLogout(Context context) {
        if (context == null) return false;
        return context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
                .getBoolean(KEY_MANUAL_LOGOUT, false);
    }

    public static void clearManualLogout(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MANUAL_LOGOUT, false)
                .apply();
    }
}
