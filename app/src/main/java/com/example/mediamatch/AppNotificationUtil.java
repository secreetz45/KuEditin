package com.example.mediamatch;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public class AppNotificationUtil {
    private static final String CHANNEL_ID = "kueditin_updates_v2";
    private static final Set<String> notified = new HashSet<>();
    private static ListenerRegistration clientReg;
    private static ListenerRegistration editorReg;

    public static void start(Context context) {
        if (context == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        createChannel(context);
        requestPermissionIfNeeded(context);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (clientReg == null) {
            clientReg = db.collection("orders").whereEqualTo("clientId", uid)
                    .addSnapshotListener((snap, e) -> {
                        if (snap == null) return;
                        for (DocumentChange dc : snap.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                String id = dc.getDocument().getId();
                                String status = safe(dc.getDocument().getString("status"), "Order diperbarui");
                                String editor = safe(dc.getDocument().getString("editorName"), "Editor");
                                String key = "client_" + id + "_" + status;

                                if (notified.add(key)) {
                                    Intent intent = new Intent(context, ClientOrderDetailActivity.class);
                                    intent.putExtra("orderId", id);
                                    show(context, "Update Order: " + status, "Proyek bersama " + editor + " berstatus: " + status, intent);
                                }
                            }
                        }
                    });
        }

        if (editorReg == null) {
            editorReg = db.collection("orders").whereEqualTo("editorId", uid)
                    .addSnapshotListener((snap, e) -> {
                        if (snap == null) return;
                        for (DocumentChange dc : snap.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String id = dc.getDocument().getId();
                                if (notified.add("editor_new_" + id)) {
                                    String client = safe(dc.getDocument().getString("clientName"), "Client");
                                    Intent intent = new Intent(context, EditorOrderDetailActivity.class);
                                    intent.putExtra("orderId", id);
                                    show(context, "Order Baru Masuk! 🔔", client + " telah memesan proyek baru darimu.", intent);
                                }
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String id = dc.getDocument().getId();
                                String status = safe(dc.getDocument().getString("status"), "Order diperbarui");
                                String client = safe(dc.getDocument().getString("clientName"), "Client");
                                String key = "editor_status_" + id + "_" + status;
                                if (notified.add(key)) {
                                    Intent intent = new Intent(context, EditorOrderDetailActivity.class);
                                    intent.putExtra("orderId", id);
                                    show(context, "Order diperbarui", "Order bersama " + client + " berstatus: " + status, intent);
                                }
                            }
                        }
                    });
        }
    }

    public static void stop() {
        if (clientReg != null) { clientReg.remove(); clientReg = null; }
        if (editorReg != null) { editorReg.remove(); editorReg = null; }
        notified.clear();
    }

    public static void show(Context context, String title, String message, Intent intent) {
        if (context == null) return;
        createChannel(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, Math.abs((title + message).hashCode()), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_app_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi);

        try {
            if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(Math.abs((title + message + System.currentTimeMillis()).hashCode()), builder.build());
            }
        } catch (Exception ignored) {}
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "KuEditin Notifikasi Real-Time", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi real-time update order, pembayaran, chat, dan status proyek KuEditin");
            channel.enableVibration(true);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private static void requestPermissionIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && context instanceof Activity) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9091);
            }
        }
    }

    private static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
}
