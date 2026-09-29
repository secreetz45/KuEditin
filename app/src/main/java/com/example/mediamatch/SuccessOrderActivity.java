package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SuccessOrderActivity extends androidx.appcompat.app.AppCompatActivity {
    private String editorId, editorName, packageName, judul;
    private long price;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_success_order);

        editorId = getIntent().getStringExtra("editorId");
        editorName = getIntent().getStringExtra("editorName");
        packageName = getIntent().getStringExtra("packageName");
        judul = getIntent().getStringExtra("judul");
        price = getIntent().getLongExtra("price", 0);
        if (price <= 0) price = getIntent().getLongExtra("dp", 0);

        bindViews();
        animateCheckmark();
    }

    private void bindViews() {
        TextView txtOrderId = findViewById(R.id.txt_success_order_id);
        TextView txtDate = findViewById(R.id.txt_success_date);
        TextView txtTitle = findViewById(R.id.txt_success_title);
        TextView txtEditor = findViewById(R.id.txt_success_editor);
        TextView txtPackage = findViewById(R.id.txt_success_package);
        TextView txtTotalPrice = findViewById(R.id.txt_success_total_price);

        String randomCode = "#ORD-" + (int)(100000 + Math.random() * 899999);
        String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());

        if (txtOrderId != null) txtOrderId.setText(randomCode);
        if (txtDate != null) txtDate.setText(dateStr);
        if (txtTitle != null) txtTitle.setText(safe(judul, "Proyek Editing"));
        if (txtEditor != null) txtEditor.setText(safe(editorName, "Editor Pro"));
        if (txtPackage != null) txtPackage.setText(safe(packageName, "Paket Jasa"));
        if (txtTotalPrice != null) txtTotalPrice.setText(rupiah(price));

        View btnViewOrder = findViewById(R.id.btn_view_order);
        if (btnViewOrder != null) {
            btnViewOrder.setOnClickListener(v -> {
                Intent i = new Intent(this, OrderClientActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            });
        }

        View btnChatEditor = findViewById(R.id.btn_chat_editor);
        if (btnChatEditor != null) {
            btnChatEditor.setOnClickListener(v -> {
                Intent i = new Intent(this, ChatRoomActivity.class);
                i.putExtra("otherId", editorId == null ? "" : editorId);
                i.putExtra("otherName", editorName == null ? "Editor" : editorName);
                startActivity(i);
                finish();
            });
        }
    }

    private void animateCheckmark() {
        View checkmarkContainer = findViewById(R.id.card_success_checkmark);
        if (checkmarkContainer != null) {
            checkmarkContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(700)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        }
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }
}
