package com.example.mediamatch;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PresenceUtil {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static String runningUid = "";

    private static final Runnable heartbeat = new Runnable() {
        @Override public void run() {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid == null || uid.trim().isEmpty()) return;
            runningUid = uid;
            writeOnline(uid);
            
            // Also heartbeat Firestore
            try {
                FirebaseFirestore.getInstance().collection("user").document(uid)
                        .update("isOnline", true, "lastSeen", FieldValue.serverTimestamp());
            } catch (Exception ignored) {}
            
            handler.postDelayed(this, 60000); // 1 minute heartbeat for Firestore
        }
    };

    private static final Runnable delayedOffline = new Runnable() {
        @Override public void run() {
            forceOffline();
        }
    };

    public static void online() {
        handler.removeCallbacks(delayedOffline);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null || uid.trim().isEmpty()) return;
        runningUid = uid;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
        ref.onDisconnect().setValue(offlineMap());
        writeOnline(uid);

        try {
            FirebaseFirestore.getInstance().collection("user").document(uid)
                    .update("isOnline", true, "lastSeen", FieldValue.serverTimestamp());
        } catch (Exception ignored) {}

        handler.removeCallbacks(heartbeat);
        handler.postDelayed(heartbeat, 15000);
    }

    // Dipanggil dari onStop Activity. Jangan langsung offline, karena saat pindah halaman
    // onStop halaman lama akan terpanggil dan bisa membuat status salah offline.
    public static void offline() {
        handler.removeCallbacks(delayedOffline);
        handler.postDelayed(delayedOffline, 12000);
    }

    // Dipakai khusus saat logout agar langsung offline.
    public static void forceOffline() {
        handler.removeCallbacks(heartbeat);
        handler.removeCallbacks(delayedOffline);
        String uid = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        if ((uid == null || uid.trim().isEmpty()) && runningUid != null && !runningUid.trim().isEmpty()) {
            uid = runningUid;
        }
        if (uid == null || uid.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("status").child(uid).setValue(offlineMap());
        try {
            FirebaseFirestore.getInstance().collection("user").document(uid)
                    .update("isOnline", false, "lastSeen", FieldValue.serverTimestamp());
        } catch (Exception ignored) {}
        runningUid = "";
    }

    public static void awayOrOffline() {
        offline();
    }

    private static void writeOnline(String uid) {
        FirebaseDatabase.getInstance().getReference("status").child(uid).setValue(onlineMap());
    }

    private static Map<String, Object> onlineMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("state", "online");
        data.put("lastChanged", System.currentTimeMillis());
        data.put("serverTime", ServerValue.TIMESTAMP);
        return data;
    }

    private static Map<String, Object> offlineMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("state", "offline");
        data.put("lastChanged", System.currentTimeMillis());
        data.put("serverTime", ServerValue.TIMESTAMP);
        return data;
    }

    public static boolean isFreshOnline(String state, Long lastChanged) {
        if (!"online".equalsIgnoreCase(state == null ? "" : state.trim())) return false;
        if (lastChanged == null || lastChanged <= 0) return false;
        return System.currentTimeMillis() - lastChanged < 90000;
    }

    public static String lastOnlineText(Long last) {
        if (last == null || last <= 0) return "Offline";
        long minutes = Math.max(0, System.currentTimeMillis() - last) / 60000;
        if (minutes < 1) return "Baru saja online";
        if (minutes < 60) return "Terakhir online " + minutes + " menit lalu";
        long hours = minutes / 60;
        if (hours < 24) return "Terakhir online " + hours + " jam lalu";
        return "Terakhir online " + (hours / 24) + " hari lalu";
    }

    public static String lastSeenText(Long last) {
        if (last == null || last <= 0) return "Offline";
        long diff = Math.max(0, System.currentTimeMillis() - last);
        long min = diff / 60000;
        if (min < 1) return "Offline";
        if (min < 60) return "Offline • " + min + " menit lalu";
        long hour = min / 60;
        if (hour < 24) return "Offline • " + hour + " jam lalu";
        long day = hour / 24;
        return "Offline • " + day + " hari lalu";
    }
}
