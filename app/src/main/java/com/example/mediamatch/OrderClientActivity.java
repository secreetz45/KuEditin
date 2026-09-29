package com.example.mediamatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderClientActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout orderContainer;
    private TextView orderSummary, tabAktif, tabSelesai, tabBatal;
    private EditText searchOrder;
    private String filter = "aktif";
    private final List<OrderItem> orders = new ArrayList<>();

    @Override public void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_order_client);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        orderContainer = findViewById(R.id.order_container);
        orderSummary = findViewById(R.id.order_summary);
        searchOrder = findViewById(R.id.search_order);
        tabAktif = findViewById(R.id.tab_aktif);
        tabSelesai = findViewById(R.id.tab_selesai);
        tabBatal = findViewById(R.id.tab_batal);

        NavUtil.setupClientNav(this, 1);

        View logoM = findViewById(R.id.logoM);
        if (logoM != null) {
            logoM.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
        }

        setupTabs();
        setFilter("aktif");
        if (searchOrder != null) {
            searchOrder.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterOrders(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        loadOrders();
    }

    private void setupTabs() {
        if (tabAktif != null) tabAktif.setOnClickListener(v -> setFilter("aktif"));
        if (tabSelesai != null) tabSelesai.setOnClickListener(v -> setFilter("selesai"));
        if (tabBatal != null) tabBatal.setOnClickListener(v -> setFilter("batal"));
    }

    private void setFilter(String f) {
        filter = f;
        updateTabStyle();
        filterOrders();
    }

    private void updateTabStyle() {
        updateTabItem(tabAktif, "aktif".equals(filter));
        updateTabItem(tabSelesai, "selesai".equals(filter));
        updateTabItem(tabBatal, "batal".equals(filter));
    }

    private void updateTabItem(TextView tab, boolean isActive) {
        if (tab == null) return;
        tab.setBackgroundResource(isActive ? R.drawable.bg_btn : R.drawable.bg_chip);
        tab.setTextColor(isActive ? Color.WHITE : getResources().getColor(R.color.textMuted));
        tab.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadOrders() {
        if (auth.getCurrentUser() == null) { showMessage("Silakan login untuk melihat order"); return; }
        String uid = auth.getCurrentUser().getUid();
        showMessage("Memuat order...");
        db.collection("orders")
                .whereEqualTo("clientId", uid)
                .get()
                .addOnSuccessListener(qs -> {
                    if (isFinishing()) return;
                    orders.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        orders.add(OrderItem.fromFirestore(doc));
                    }
                    filterOrders();
                })
                .addOnFailureListener(e -> showMessage("Gagal memuat: " + e.getMessage()));
    }

    private void filterOrders() {
        if (orderContainer == null) return;
        orderContainer.removeAllViews();
        String keyword = searchOrder != null ? searchOrder.getText().toString().trim().toLowerCase() : "";
        int shown = 0;
        for (OrderItem order : orders) {
            boolean tab = filter.equals("aktif") ? !order.status.toLowerCase().contains("selesai") && !order.status.toLowerCase().contains("batal")
                    : filter.equals("selesai") ? order.status.toLowerCase().contains("selesai")
                    : order.status.toLowerCase().contains("batal");
            boolean match = keyword.isEmpty() || order.editorName.toLowerCase().contains(keyword) || order.service.toLowerCase().contains(keyword) || order.status.toLowerCase().contains(keyword);
            if (tab && match) { orderContainer.addView(orderCard(order)); shown++; }
        }
        if (orders.isEmpty()) { orderSummary.setText("Belum pernah order"); orderContainer.addView(messageView("Belum ada order. Pilih editor lalu tekan Pesan.")); }
        else { orderSummary.setText(shown + " order " + filter); if (shown == 0) orderContainer.addView(messageView("Tidak ada order " + filter)); }
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
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        avatar.setBackgroundResource(R.drawable.circle_soft);
        if (!item.editorPhoto.isEmpty()) ImageLoader.load(avatar, item.editorPhoto);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoP = new LinearLayout.LayoutParams(0, -2, 1);
        infoP.setMargins(dp(14), 0, dp(8), 0);
        info.setLayoutParams(infoP);

        info.addView(text(item.packageName, 17, getResources().getColor(R.color.textPrimary), true));
        TextView editor = text("Editor: " + item.editorName, 14, getResources().getColor(R.color.textMuted), false);
        editor.setPadding(0, dp(2), 0, 0);
        info.addView(editor);

        TextView price = text(rupiah(item.price), 15, getResources().getColor(R.color.colorPrimary), true);
        price.setPadding(0, dp(6), 0, 0);
        info.addView(price);

        top.addView(info);

        TextView status = new TextView(this);
        status.setText(statusShort(item.status));
        status.setTextSize(11);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setPadding(dp(10), dp(4), dp(10), dp(4));

        String lowerStatus = item.status.toLowerCase();
        if (lowerStatus.contains("selesai") || lowerStatus.contains("done")) {
            status.setTextColor(Color.parseColor("#166534"));
            status.setBackgroundResource(R.drawable.bg_badge_online);
        } else if (lowerStatus.contains("batal")) {
            status.setTextColor(Color.parseColor("#991B1B"));
            status.setBackgroundResource(R.drawable.bg_badge_canceled);
        } else if (lowerStatus.contains("proses") || lowerStatus.contains("review") || lowerStatus.contains("bayar")) {
            status.setTextColor(Color.parseColor("#92400E"));
            status.setBackgroundResource(R.drawable.bg_badge_working);
        } else {
            status.setTextColor(Color.parseColor("#5D63FF"));
            status.setBackgroundResource(R.drawable.bg_badge_pending);
        }
        top.addView(status);

        TextView progressLabel = text("Pantau Progres: " + item.progress + "%", 13, getResources().getColor(R.color.colorPrimary), true);
        progressLabel.setPadding(0, dp(12), 0, 0);
        box.addView(progressLabel);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, ClientOrderDetailActivity.class);
            i.putExtra("orderId", item.id);
            startActivity(i);
            TransitionUtil.slideRight(this);
        });

        return card;
    }

    private String statusShort(String s) { String v=s.toLowerCase(); if(v.contains("selesai"))return "Selesai"; if(v.contains("batal"))return "Batal"; if(v.contains("review"))return "Review"; return "Proses"; }

    private TextView text(String s, int size, int colorInt, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s == null ? "" : s);
        tv.setTextSize(size);
        tv.setTextColor(colorInt);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView text(String s, int size, String color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s == null ? "" : s);
        tv.setTextSize(size);
        tv.setTextColor(Color.parseColor(color));
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void showMessage(String msg) { if (orderSummary != null) orderSummary.setText(msg); if (orderContainer != null) { orderContainer.removeAllViews(); orderContainer.addView(messageView(msg)); } }
    private View messageView(String message) { TextView tv = text(message, 15, getResources().getColor(R.color.textMuted), false); tv.setGravity(Gravity.CENTER); tv.setPadding(dp(16), dp(22), dp(16), dp(22)); tv.setBackgroundResource(R.drawable.bg_empty_state); return tv; }
    private String rupiah(long v) { return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", ""); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    static class OrderItem {
        String id, editorName, service, status, icon, createdText, packageName, editorPhoto;
        int progress; long price;
        static OrderItem fromFirestore(DocumentSnapshot doc) {
            OrderItem o = new OrderItem();
            o.id = doc.getId();
            o.editorName = safe(doc.getString("editorName"), safe(doc.getString("nama"), "Editor Pro"));
            o.service = safe(doc.getString("service"), "Jasa Kreatif");

            String pkgStr = doc.getString("packageName");
            if (pkgStr == null || pkgStr.trim().isEmpty()) pkgStr = doc.getString("judul");
            if (pkgStr == null || pkgStr.trim().isEmpty()) pkgStr = o.service;
            o.packageName = pkgStr.trim();

            o.status = safe(doc.getString("status"), "Menunggu konfirmasi");
            o.editorPhoto = safe(doc.getString("editorPhoto"), safe(doc.getString("fotoProfil"), ""));

            Long p = doc.getLong("progress");
            o.progress = p == null ? 0 : p.intValue();

            Long priceVal = doc.getLong("hargaMulai");
            if (priceVal == null) priceVal = doc.getLong("harga");
            if (priceVal == null) priceVal = doc.getLong("price");
            if (priceVal == null) priceVal = doc.getLong("paymentNominal");
            o.price = priceVal == null ? 0 : priceVal;

            o.icon = iconFor(o.service);
            Object created = doc.get("createdAt");
            if (created instanceof com.google.firebase.Timestamp) {
                Date d = ((com.google.firebase.Timestamp) created).toDate();
                o.createdText = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(d);
            } else o.createdText = "baru saja";
            return o;
        }
        static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
        static String iconFor(String service) { String s = service == null ? "" : service.toLowerCase(); if (s.contains("video")) return "🎬"; if (s.contains("ui") || s.contains("ux") || s.contains("design")) return "🎨"; if (s.contains("foto")) return "📷"; if (s.contains("motion")) return "✨"; return "Ⓜ"; }
    }
}
