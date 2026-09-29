package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String editorId, editorName, editorPhoto, clientName, clientEmail, clientPhoto;
    private String service, packageName, packageType, judul, brief, fileName, fileMime, fileUrl, fileLocalUri;
    private long price, deadlineMillis;

    private TextView txtPaymentPackage, txtPaymentEditor, txtPaymentPrice;
    private ImageView imgPaymentQr;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_payment);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent in = getIntent();
        editorId = in.getStringExtra("editorId");
        editorName = in.getStringExtra("editorName");
        editorPhoto = in.getStringExtra("editorPhoto");
        clientName = in.getStringExtra("clientName");
        clientEmail = in.getStringExtra("clientEmail");
        clientPhoto = in.getStringExtra("clientPhoto");
        service = in.getStringExtra("service");
        packageName = in.getStringExtra("packageName");
        packageType = in.getStringExtra("packageType");
        judul = in.getStringExtra("judul");
        brief = in.getStringExtra("brief");
        fileName = in.getStringExtra("fileName");
        fileMime = in.getStringExtra("fileMime");
        fileUrl = in.getStringExtra("fileUrl");
        fileLocalUri = in.getStringExtra("fileLocalUri");
        price = in.getLongExtra("price", 0);
        deadlineMillis = in.getLongExtra("deadlineMillis", 0);

        bindViews();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        txtPaymentPackage = findViewById(R.id.txt_payment_package);
        txtPaymentEditor = findViewById(R.id.txt_payment_editor);
        txtPaymentPrice = findViewById(R.id.txt_payment_price);
        imgPaymentQr = findViewById(R.id.img_payment_qr);

        if (txtPaymentPackage != null) txtPaymentPackage.setText(safe(packageName, safe(service, "Paket Jasa")));
        if (txtPaymentEditor != null) txtPaymentEditor.setText("Editor: " + safe(editorName, "Editor Pro"));
        if (txtPaymentPrice != null) txtPaymentPrice.setText(rupiah(price));

        if (imgPaymentQr != null) {
            imgPaymentQr.setImageBitmap(makeQr(String.valueOf(price > 0 ? price : 100000)));
        }

        View btnPaidConfirm = findViewById(R.id.btn_paid_confirm);
        if (btnPaidConfirm != null) {
            btnPaidConfirm.setOnClickListener(v -> createPaidOrder());
        }

        View btnCancel = findViewById(R.id.btn_cancel_payment);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void createPaidOrder() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        long platformFee = (price * 10) / 100; // 10% System Commission
        long editorNetRevenue = price - platformFee; // 90% Editor Net Payout

        Map<String, Object> data = new HashMap<>();
        data.put("clientId", uid);
        data.put("clientName", safe(clientName, "Client"));
        data.put("clientEmail", safe(clientEmail, ""));
        data.put("clientPhoto", safe(clientPhoto, ""));
        data.put("editorId", safe(editorId, ""));
        data.put("editorName", safe(editorName, "Editor"));
        data.put("editorPhoto", safe(editorPhoto, ""));
        data.put("service", safe(service, "Jasa Kreatif"));
        data.put("packageName", safe(packageName, "Paket Jasa"));
        data.put("packageType", safe(packageType, "basic"));
        data.put("price", price);
        data.put("dp", price);
        data.put("remainingPayment", 0);
        data.put("paymentStatus", "LUNAS");
        data.put("paymentNominal", price);
        data.put("platformFee", platformFee);
        data.put("editorNetRevenue", editorNetRevenue);
        data.put("judul", safe(judul, "Proyek Editing"));
        data.put("brief", safe(brief, ""));

        if (deadlineMillis > 0) {
            data.put("deadline", new com.google.firebase.Timestamp(new java.util.Date(deadlineMillis)));
        }

        data.put("fileName", safe(fileName, ""));
        data.put("fileMime", safe(fileMime, ""));
        data.put("fileUrl", safe(fileUrl, ""));
        data.put("fileLocalUri", safe(fileLocalUri, ""));
        data.put("status", "Menunggu konfirmasi");
        data.put("progress", 0);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        Toast.makeText(this, "Memproses pembayaran...", Toast.LENGTH_SHORT).show();

        db.collection("orders").add(data).addOnSuccessListener(r -> {
            Toast.makeText(this, "Pembayaran berhasil!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SuccessOrderActivity.class);
            intent.putExtra("orderId", r.getId());
            intent.putExtra("editorId", editorId);
            intent.putExtra("editorName", editorName);
            intent.putExtra("packageName", packageName);
            intent.putExtra("judul", judul);
            intent.putExtra("price", price);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal membuat order: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private Bitmap makeQr(String value) {
        try {
            int size = 720;
            BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            Bitmap bmp = Bitmap.createBitmap(360, 360, Bitmap.Config.RGB_565);
            bmp.eraseColor(Color.WHITE);
            return bmp;
        }
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }
}
