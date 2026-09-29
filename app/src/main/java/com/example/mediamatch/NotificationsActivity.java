package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout notificationContainer;
    private TextView txtNotificationCount;

    private ListenerRegistration orderListener;
    private int notificationCount = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_notifications);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        loadNotifications();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        notificationContainer = findViewById(R.id.notification_container);
        txtNotificationCount = findViewById(R.id.txt_notification_count);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
        AppNotificationUtil.start(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) orderListener.remove();
    }

    private void loadNotifications() {
        if (auth.getCurrentUser() == null) {
            showEmptyState("Silakan Login Terlebih Dahulu", "Masuk ke akunmu untuk melihat notifikasi dan aktivitas proyek.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("user").document(uid).get().addOnSuccessListener(userDoc -> {
            if (isFinishing() || userDoc == null || !userDoc.exists()) return;

            String role = safe(userDoc.getString("role"), "client");
            boolean isEditor = "editor".equalsIgnoreCase(role);

            listenOrderUpdates(uid, isEditor);
        }).addOnFailureListener(e -> showEmptyState("Gagal Memuat Notifikasi", e.getMessage()));
    }

    private void listenOrderUpdates(String uid, boolean isEditor) {
        if (notificationContainer == null) return;

        notificationContainer.removeAllViews();
        notificationContainer.addView(createLoadingCard());

        if (orderListener != null) orderListener.remove();

        Query query = db.collection("orders")
                .whereEqualTo(isEditor ? "editorId" : "clientId", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(25);

        orderListener = query.addSnapshotListener((snap, e) -> {
            if (isFinishing()) return;

            if (e != null) {
                // Fallback query if index or ordering fails
                db.collection("orders")
                        .whereEqualTo(isEditor ? "editorId" : "clientId", uid)
                        .get()
                        .addOnSuccessListener(fallbackSnap -> renderOrderNotifications(fallbackSnap.getDocuments(), isEditor))
                        .addOnFailureListener(err -> showEmptyState("Gagal Memuat Notifikasi", err.getMessage()));
                return;
            }

            if (snap != null) {
                renderOrderNotifications(snap.getDocuments(), isEditor);
            }
        });
    }

    private void renderOrderNotifications(List<DocumentSnapshot> docs, boolean isEditor) {
        if (notificationContainer == null) return;
        notificationContainer.removeAllViews();
        notificationCount = 0;

        if (docs == null || docs.isEmpty()) {
            showEmptyState("Belum Ada Notifikasi Baru", "Aktivitas order, pembayaran, dan update status proyek akan tampil di sini.");
            return;
        }

        for (DocumentSnapshot doc : docs) {
            notificationCount++;
            View card = createNotificationCard(doc, isEditor);
            notificationContainer.addView(card);
        }

        if (txtNotificationCount != null) {
            txtNotificationCount.setText(notificationCount + " Notifikasi");
        }
    }

    private View createNotificationCard(DocumentSnapshot doc, boolean isEditor) {
        String orderId = doc.getId();
        String status = safe(doc.getString("status"), "Order Diperbarui");
        String title = safe(doc.getString("judul"), safe(doc.getString("service"), "Proyek Editing"));

        String otherName = isEditor ? safe(doc.getString("clientName"), "Client") : safe(doc.getString("editorName"), "Editor");

        Date updatedAt = doc.getTimestamp("updatedAt") != null ? doc.getTimestamp("updatedAt").toDate() :
                (doc.getTimestamp("createdAt") != null ? doc.getTimestamp("createdAt").toDate() : new Date());

        String timeStr = new SimpleDateFormat("dd MMM HH:mm", new Locale("id", "ID")).format(updatedAt);

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(20));
        card.setCardElevation(dp(3));
        card.setStrokeColor(Color.parseColor("#F1F5F9"));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardLp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(box);

        // Header Row: Status Badge Pill + Time
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(topRow);

        TextView statusBadge = new TextView(this);
        statusBadge.setText(status);
        statusBadge.setTextSize(11);
        statusBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        statusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));

        String lowerStatus = status.toLowerCase();
        if (lowerStatus.contains("selesai") || lowerStatus.contains("done")) {
            statusBadge.setTextColor(Color.parseColor("#166534"));
            statusBadge.setBackgroundResource(R.drawable.bg_badge_online);
        } else if (lowerStatus.contains("batal")) {
            statusBadge.setTextColor(Color.parseColor("#991B1B"));
            statusBadge.setBackgroundResource(R.drawable.bg_badge_canceled);
        } else if (lowerStatus.contains("review") || lowerStatus.contains("proses") || lowerStatus.contains("bayar")) {
            statusBadge.setTextColor(Color.parseColor("#92400E"));
            statusBadge.setBackgroundResource(R.drawable.bg_badge_working);
        } else {
            statusBadge.setTextColor(Color.parseColor("#5D63FF"));
            statusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
        }

        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2);
        topRow.addView(statusBadge, badgeLp);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, -2, 1);
        topRow.addView(spacer, spacerLp);

        TextView timeTv = new TextView(this);
        timeTv.setText(timeStr);
        timeTv.setTextColor(Color.parseColor("#94A3B8"));
        timeTv.setTextSize(11);
        topRow.addView(timeTv);

        // Title Row
        TextView titleTv = new TextView(this);
        titleTv.setText(isEditor ? "Order dari " + otherName : "Update Order bersama " + otherName);
        titleTv.setTextColor(Color.parseColor("#0F172A"));
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setPadding(0, dp(10), 0, 0);
        box.addView(titleTv);

        // Subtitle Message
        TextView msgTv = new TextView(this);
        msgTv.setText("Proyek \"" + title + "\" berstatus: " + status);
        msgTv.setTextColor(Color.parseColor("#475569"));
        msgTv.setTextSize(13);
        msgTv.setPadding(0, dp(4), 0, 0);
        box.addView(msgTv);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, isEditor ? EditorOrderDetailActivity.class : ClientOrderDetailActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
            TransitionUtil.slideRight(this);
        });

        return card;
    }

    private View createLoadingCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(20));
        card.setCardElevation(dp(2));
        card.setStrokeColor(Color.parseColor("#F1F5F9"));

        TextView tv = new TextView(this);
        tv.setText("⏳ Memuat notifikasi real-time...");
        tv.setTextColor(Color.parseColor("#5D63FF"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(20), dp(30), dp(20), dp(30));
        card.addView(tv);
        return card;
    }

    private void showEmptyState(String title, String message) {
        if (notificationContainer == null) return;
        notificationContainer.removeAllViews();

        if (txtNotificationCount != null) {
            txtNotificationCount.setText("0 Notifikasi");
        }

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(3));
        card.setStrokeColor(Color.parseColor("#F1F5F9"));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(40), dp(24), dp(40));
        card.addView(box);

        TextView icon = new TextView(this);
        icon.setText("🔔");
        icon.setTextSize(36);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(Color.parseColor("#0F172A"));
        titleTv.setTextSize(17);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setPadding(0, dp(10), 0, 0);
        box.addView(titleTv);

        TextView msgTv = new TextView(this);
        msgTv.setText(message);
        msgTv.setTextColor(Color.parseColor("#64748B"));
        msgTv.setTextSize(13);
        msgTv.setGravity(Gravity.CENTER);
        msgTv.setPadding(0, dp(6), 0, 0);
        box.addView(msgTv);

        notificationContainer.addView(card);
    }

    private String safe(String v, String fallback) {
        return v == null || v.trim().isEmpty() ? fallback : v;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
