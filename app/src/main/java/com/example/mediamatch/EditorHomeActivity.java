package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditorHomeActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout orderContainer;
    private TextView welcome, totalOrder, activeOrder, doneOrder, txtRevenue, performanceSummary;
    private SwitchMaterial switchWorking;

    private final List<OrderItem> orders = new ArrayList<>();
    private ListenerRegistration dashboardListener;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_editor_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavUtil.saveRole(this, "editor");

        welcome = findViewById(R.id.txt_editor_welcome);
        totalOrder = findViewById(R.id.txt_total_order);
        activeOrder = findViewById(R.id.txt_active_order);
        doneOrder = findViewById(R.id.txt_done_order);
        txtRevenue = findViewById(R.id.txt_revenue);

        orderContainer = findViewById(R.id.editor_home_order_container);
        switchWorking = findViewById(R.id.switch_is_working);

        View logoM = findViewById(R.id.logoM);
        if (logoM != null) {
            logoM.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            if (logoM.getParent() instanceof View) {
                ((View) logoM.getParent()).setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            }
        }

        View btnLogout = findViewById(R.id.btn_logout_editor_home);
        if (btnLogout != null) {
            btnLogout.setVisibility(View.GONE);
        }

        View btnOrders = findViewById(R.id.btn_editor_orders);
        if (btnOrders != null) {
            btnOrders.setOnClickListener(v -> {
                startActivity(new Intent(this, EditorOrdersActivity.class));
                TransitionUtil.slideRight(this);
            });
        }

        View btnChat = findViewById(R.id.btn_editor_chat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                startActivity(new Intent(this, ChatListActivity.class));
                TransitionUtil.slideRight(this);
            });
        }

        View btnManageService = findViewById(R.id.btn_manage_service);
        if (btnManageService != null) {
            btnManageService.setOnClickListener(v -> {
                startActivity(new Intent(this, EditServiceActivity.class));
                TransitionUtil.slideRight(this);
            });
        }

        if (switchWorking != null) {
            switchWorking.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    updateWorkingStatus(isChecked);
                }
            });
        }

        NavUtil.setupClientNav(this, 0);

        loadEditorData();
        loadDashboard();
    }

    private void loadEditorData() {
        if (auth.getCurrentUser() == null) return;
        db.collection("user").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || doc == null || !doc.exists()) return;

                    String nama = safe(doc.getString("nama"), "Editor");
                    if (welcome != null) {
                        welcome.setText("Halo, " + nama);
                    }

                    Boolean working = doc.getBoolean("isWorking");
                    if (working != null && switchWorking != null) {
                        switchWorking.setChecked(working);
                    }
                });
    }

    private void updateWorkingStatus(boolean working) {
        if (auth.getCurrentUser() == null) return;
        db.collection("user").document(auth.getCurrentUser().getUid())
                .update("isWorking", working)
                .addOnSuccessListener(v -> Toast.makeText(this, working ? "Status: Siap Menerima Order" : "Status: Off Order", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal update status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadDashboard() {
        if (auth.getCurrentUser() == null) {
            showMessage("Silakan login sebagai editor");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        if (orderContainer != null) {
            orderContainer.removeAllViews();
            orderContainer.addView(messageView("Memuat performa kerja..."));
        }

        if (dashboardListener != null) dashboardListener.remove();
        dashboardListener = db.collection("orders")
                .whereEqualTo("editorId", uid)
                .addSnapshotListener((snap, e) -> {
                    if (isFinishing()) return;
                    if (e != null) {
                        showMessage("Gagal memuat data: " + e.getMessage());
                        return;
                    }

                    orders.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            orders.add(OrderItem.fromFirestore(doc));
                        }
                    }
                    renderDashboard();
                });
    }

    private void renderDashboard() {
        if (isFinishing() || orderContainer == null) return;

        int total = orders.size();
        int active = 0, done = 0;
        long estimatedRevenue = 0;

        for (OrderItem o : orders) {
            long netPrice = (o.price * 90) / 100; // Net 90% after 10% platform fee
            if ("Selesai".equalsIgnoreCase(o.status) || o.progress >= 100) {
                done++;
                estimatedRevenue += netPrice;
            } else if (!o.status.toLowerCase().contains("batal")) {
                active++;
                estimatedRevenue += netPrice;
            }
        }

        if (totalOrder != null) totalOrder.setText(String.valueOf(total));
        if (activeOrder != null) activeOrder.setText(String.valueOf(active));
        if (doneOrder != null) doneOrder.setText(String.valueOf(done));
        if (txtRevenue != null) txtRevenue.setText(rupiah(estimatedRevenue));

        if (performanceSummary != null) {
            if (total == 0) {
                performanceSummary.setText("Belum ada order. Performa akan muncul setelah client memesan.");
            } else {
                int percent = (done * 100) / total;
                performanceSummary.setText("Penyelesaian Proyek: " + percent + "% • Potongan Sistem 10%");
            }
        }

        orderContainer.removeAllViews();
        List<OrderItem> activeList = new ArrayList<>();
        for (OrderItem o : orders) {
            if (!("Selesai".equalsIgnoreCase(o.status) || o.progress >= 100)) {
                activeList.add(o);
            }
        }

        if (activeList.isEmpty()) {
            orderContainer.addView(messageView("Tidak ada order aktif saat ini."));
            return;
        }

        for (int i = 0; i < Math.min(activeList.size(), 5); i++) {
            orderContainer.addView(orderCard(activeList.get(i)));
        }
    }

    private View orderCard(OrderItem item) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));
        card.setRadius(dp(20));
        card.setCardElevation(dp(3));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(box);

        // Header Row: Avatar + Client Name & Service + Status Pill
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top);

        CircleImageView avatar = new CircleImageView(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        avatar.setBackgroundResource(R.drawable.circle_soft);
        if (item.clientPhoto != null && !item.clientPhoto.trim().isEmpty()) {
            ImageLoader.load(avatar, item.clientPhoto.trim());
        }
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1);
        ip.setMargins(dp(14), 0, dp(8), 0);
        top.addView(info, ip);

        TextView clientNameTv = text(item.clientName, 16, getResources().getColor(R.color.textPrimary), true);
        info.addView(clientNameTv);

        TextView serviceTv = text(item.service, 13, getResources().getColor(R.color.textMuted), false);
        serviceTv.setPadding(0, dp(2), 0, 0);
        info.addView(serviceTv);

        // Status Badge Pill
        TextView statusBadge = new TextView(this);
        statusBadge.setText(item.status);
        statusBadge.setTextSize(11);
        statusBadge.setTextColor(getResources().getColor(R.color.textSecondary));
        statusBadge.setBackgroundResource(R.drawable.bg_chip);
        statusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        top.addView(statusBadge);

        // Progress & Price Footer Row
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2);
        footLp.setMargins(0, dp(12), 0, 0);
        footer.setLayoutParams(footLp);

        TextView progressTv = text("Progres: " + item.progress + "%", 12, getResources().getColor(R.color.textMuted), true);
        LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(0, -2, 1);
        footer.addView(progressTv, prLp);

        TextView priceTv = text(rupiah(item.price), 14, getResources().getColor(R.color.colorPrimary), true);
        footer.addView(priceTv);

        box.addView(footer);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, EditorOrderDetailActivity.class);
            i.putExtra("orderId", item.id);
            startActivity(i);
            TransitionUtil.slideRight(this);
        });

        return card;
    }

    private void showMessage(String message) {
        if (orderContainer == null) return;
        orderContainer.removeAllViews();
        orderContainer.addView(messageView(message));
    }

    private TextView messageView(String message) {
        TextView tv = text(message, 15, getResources().getColor(R.color.textMuted), false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(24), dp(16), dp(24));
        tv.setBackgroundResource(R.drawable.bg_empty_state);
        return tv;
    }

    private TextView text(String s, int size, int colorInt, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s == null ? "" : s);
        tv.setTextSize(size);
        tv.setTextColor(colorInt);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private static String safe(String v, String fallback) { return v == null || v.trim().isEmpty() ? fallback : v; }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dashboardListener != null) dashboardListener.remove();
    }

    static class OrderItem {
        String id, clientName, service, status, clientPhoto;
        int progress;
        long price;

        static OrderItem fromFirestore(DocumentSnapshot doc) {
            OrderItem o = new OrderItem();
            o.id = doc.getId();
            o.clientName = safe(doc.getString("clientName"), "Client");
            o.clientPhoto = safe(doc.getString("clientPhoto"), "");
            o.service = safe(doc.getString("service"), safe(doc.getString("packageName"), "Jasa Kreatif"));
            o.status = safe(doc.getString("status"), "Menunggu konfirmasi");

            Long p = doc.getLong("progress");
            Long price = doc.getLong("hargaMulai");
            if (price == null) price = doc.getLong("harga");
            if (price == null) price = doc.getLong("price");

            o.progress = p == null ? 0 : p.intValue();
            o.price = price == null ? 0 : price;
            return o;
        }
    }
}
