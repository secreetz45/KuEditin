package com.example.mediamatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditorOrdersActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout container;
    private TextView summary, tabKonfirmasi, tabProses, tabSelesai, tabBatal;
    private String filter = "konfirmasi";
    private final List<OrderItem> orders = new ArrayList<>();

    @Override
    public void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_editor_orders);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        container = findViewById(R.id.editor_order_container);
        summary = findViewById(R.id.editor_order_summary);

        tabKonfirmasi = findViewById(R.id.tab_konfirmasi);
        tabProses = findViewById(R.id.tab_proses);
        tabSelesai = findViewById(R.id.tab_selesai);
        tabBatal = findViewById(R.id.tab_batal);

        NavUtil.setupClientNav(this, 1);
        setupTabs();

        View logoM = findViewById(R.id.logoM);
        if (logoM != null) {
            logoM.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            if (logoM.getParent() instanceof View) {
                ((View) logoM.getParent()).setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            }
        }

        loadOrders();
    }

    private void setupTabs() {
        if (tabKonfirmasi != null) tabKonfirmasi.setOnClickListener(v -> setFilter("konfirmasi"));
        if (tabProses != null) tabProses.setOnClickListener(v -> setFilter("proses"));
        if (tabSelesai != null) tabSelesai.setOnClickListener(v -> setFilter("selesai"));
        if (tabBatal != null) tabBatal.setOnClickListener(v -> setFilter("batal"));
    }

    private void setFilter(String f) {
        filter = f;
        updateTabStyle();
        renderOrders();
    }

    private void updateTabStyle() {
        setTabStyle(tabKonfirmasi, "konfirmasi".equals(filter));
        setTabStyle(tabProses, "proses".equals(filter));
        setTabStyle(tabSelesai, "selesai".equals(filter));
        setTabStyle(tabBatal, "batal".equals(filter));
    }

    private void setTabStyle(TextView tab, boolean active) {
        if (tab == null) return;
        tab.setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tab.setTextColor(active ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textMuted));
        tab.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadOrders() {
        if (auth.getCurrentUser() == null) {
            showMessage("Silakan login sebagai editor");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        if (summary != null) summary.setText("Memuat order...");
        if (container != null) {
            container.removeAllViews();
            container.addView(messageView("Memuat order..."));
        }
        db.collection("orders")
                .whereEqualTo("editorId", uid)
                .get()
                .addOnSuccessListener(qs -> {
                    if (isFinishing()) return;
                    orders.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        orders.add(OrderItem.fromFirestore(doc));
                    }
                    renderOrders();
                })
                .addOnFailureListener(e -> showMessage("Gagal memuat order: " + e.getMessage()));
    }

    private void renderOrders() {
        if (container == null) return;
        container.removeAllViews();
        int shown = 0;

        for (OrderItem item : orders) {
            String statusLower = item.status.toLowerCase();
            boolean match = false;
            if ("konfirmasi".equals(filter)) {
                match = statusLower.contains("menunggu") || statusLower.contains("konfirmasi") || item.progress == 0;
            } else if ("proses".equals(filter)) {
                match = (statusLower.contains("proses") || statusLower.contains("review") || statusLower.contains("berlangsung") || statusLower.contains("mengerjakan") || statusLower.contains("bayar")) && !statusLower.contains("selesai");
            } else if ("selesai".equals(filter)) {
                match = statusLower.contains("selesai") || item.progress >= 100;
            } else if ("batal".equals(filter)) {
                match = statusLower.contains("batal");
            }

            if (match) {
                container.addView(orderCard(item));
                shown++;
            }
        }

        if (orders.isEmpty()) {
            if (summary != null) summary.setText("Belum ada order masuk");
            container.addView(messageView("Belum ada order masuk dari klien sejauh ini."));
        } else {
            if (summary != null) summary.setText(shown + " order " + filter);
            if (shown == 0) {
                container.addView(messageView("Tidak ada order dengan status " + filter));
            }
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
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(box);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top);

        CircleImageView avatar = new CircleImageView(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(54), dp(54)));
        avatar.setBackgroundResource(R.drawable.circle_soft);
        if (item.clientPhoto != null && !item.clientPhoto.trim().isEmpty()) ImageLoader.load(avatar, item.clientPhoto);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1);
        ip.setMargins(dp(14), 0, 0, 0);
        top.addView(info, ip);

        TextView title = text(item.clientName, 17, getResources().getColor(R.color.textPrimary), true);
        info.addView(title);
        TextView service = text(item.service, 14, getResources().getColor(R.color.textMuted), false);
        service.setPadding(0, dp(2), 0, 0);
        info.addView(service);

        TextView status = new TextView(this);
        status.setText(item.status + " • " + item.progress + "%");
        status.setTextSize(11);
        status.setTextColor(getResources().getColor(R.color.textSecondary));
        status.setBackgroundResource(R.drawable.bg_chip);
        status.setPadding(dp(10), dp(4), dp(10), dp(4));

        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-2, -2);
        stLp.setMargins(0, dp(10), 0, dp(10));
        status.setLayoutParams(stLp);
        box.addView(status);

        TextView detailButton = fullButton("Lihat Rincian & Bahan Proyek", v -> openDetail(item.id));
        box.addView(detailButton);

        if (item.canRespond()) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(0, dp(10), 0, 0);
            actions.addView(button("Batalkan Pesanan", v -> updateOrder(item.id, "Batal", 0)));
            actions.addView(button("Terima Pesanan", v -> updateOrder(item.id, "Sedang berlangsung", 30)));
            box.addView(actions);
        }

        card.setOnClickListener(v -> openDetail(item.id));
        return card;
    }

    private void openDetail(String orderId) {
        Intent i = new Intent(this, EditorOrderDetailActivity.class);
        i.putExtra("orderId", orderId);
        startActivity(i);
        TransitionUtil.slideRight(this);
    }

    private TextView fullButton(String label, View.OnClickListener listener) {
        TextView b = button(label, listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(42));
        p.setMargins(0, dp(6), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private TextView button(String label, View.OnClickListener listener) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTypeface(null, 1);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(R.drawable.bg_btn);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1);
        p.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(p);
        b.setOnClickListener(listener);
        return b;
    }

    private void updateOrder(String orderId, String status, int progress) {
        if ("Batal".equalsIgnoreCase(status) || "Dibatalkan".equalsIgnoreCase(status)) {
            db.collection("orders").document(orderId).get().addOnSuccessListener(d -> {
                if (d == null || !d.exists()) return;
                String clientId = d.getString("clientId");
                Long priceVal = d.getLong("price");
                if (priceVal == null) priceVal = d.getLong("paymentNominal");
                long price = priceVal == null ? 0 : priceVal;

                Map<String, Object> upd = new HashMap<>();
                upd.put("status", "Batal");
                upd.put("progress", 0);
                upd.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("orders").document(orderId).update(upd).addOnSuccessListener(x -> {
                    if (clientId != null && !clientId.isEmpty() && price > 0) {
                        db.collection("user").document(clientId)
                                .update("saldo", FieldValue.increment(price));
                    }
                    Toast.makeText(this, "Order dibatalkan. Dana Rp" + price + " telah dikembalikan 100% ke saldo client!", Toast.LENGTH_LONG).show();
                    loadOrders();
                });
            });
            return;
        }

        db.collection("orders").document(orderId)
                .update("status", status, "progress", progress, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Status order diperbarui ke " + status, Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private TextView text(String s, int size, int colorInt, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(colorInt);
        if (bold) tv.setTypeface(null, 1);
        return tv;
    }

    private void showMessage(String msg) {
        if (summary != null) summary.setText(msg);
        if (container != null) {
            container.removeAllViews();
            container.addView(messageView(msg));
        }
    }

    private View messageView(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(getResources().getColor(R.color.textMuted));
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(22), dp(16), dp(22));
        tv.setBackgroundResource(R.drawable.bg_empty_state);
        return tv;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    static class OrderItem {
        String id, clientName, service, status, clientPhoto, resultUrl, resultBase64;
        int progress;
        boolean canRespond() {
            String s = status == null ? "" : status.toLowerCase(Locale.ROOT);
            return s.contains("menunggu") || s.contains("konfirmasi") || progress == 0;
        }
        static OrderItem fromFirestore(DocumentSnapshot doc) {
            OrderItem o = new OrderItem();
            o.id = doc.getId();
            o.clientName = safe(doc.getString("clientName"), "Client");
            o.clientPhoto = safe(doc.getString("clientPhoto"), "");
            o.service = safe(doc.getString("service"), "Jasa Kreatif");
            o.status = safe(doc.getString("status"), "Menunggu konfirmasi");
            o.resultUrl = safe(doc.getString("resultUrl"), "");
            o.resultBase64 = safe(doc.getString("resultBase64"), "");
            Long p = doc.getLong("progress");
            o.progress = p == null ? 0 : p.intValue();
            return o;
        }
        static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
    }
}
