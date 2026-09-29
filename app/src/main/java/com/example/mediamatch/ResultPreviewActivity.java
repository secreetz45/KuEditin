package com.example.mediamatch;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultPreviewActivity extends Activity {
    private FirebaseFirestore db;
    private LinearLayout root;
    private String orderId = "", resultUrl = "", resultBase64 = "", resultName = "hasil_editing", resultMime = "*/*";
    private long remainingPayment = 0;
    private boolean resultPaid = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = FirebaseFirestore.getInstance();
        orderId = safe(getIntent().getStringExtra("orderId"));
        resultUrl = safe(getIntent().getStringExtra("resultUrl"));
        resultBase64 = safe(getIntent().getStringExtra("resultBase64"));
        resultName = safe(getIntent().getStringExtra("resultName"), "hasil_editing");
        resultMime = safe(getIntent().getStringExtra("resultMime"), "*/*");
        remainingPayment = getIntent().getLongExtra("remainingPayment", 0);
        resultPaid = getIntent().getBooleanExtra("resultPaid", false) || remainingPayment <= 0;
        render();
    }

    private void render() {
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Color.parseColor("#F8FAFC"));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(34));
        sc.addView(root);
        setContentView(sc);

        TextView back = text("‹  Preview Hasil", 28, "#0F172A", true);
        back.setOnClickListener(v -> finish());
        root.addView(back);

        if (resultPaid) renderUnlocked(); else renderLocked();
    }

    private void renderLocked() {
        LinearLayout preview = card();
        preview.setGravity(Gravity.CENTER);
        preview.setBackgroundColor(Color.parseColor("#DDE7F3"));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(320));
        pp.setMargins(0, dp(24), 0, dp(20));
        root.addView(preview, pp);

        TextView lock = text("🔒\nPreview dikunci\nLunasi pembayaran untuk melihat dan mengunduh hasil pekerjaan", 20, "#FFFFFF", true);
        lock.setGravity(Gravity.CENTER);
        lock.setBackgroundColor(Color.parseColor("#990F172A"));
        lock.setPadding(dp(16), dp(16), dp(16), dp(16));
        preview.addView(lock, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout payCard = card();
        root.addView(payCard, margin(0, 0, 0, 18));
        TextView title = text("Total yang harus dibayar", 17, "#64748B", false);
        payCard.addView(title);
        TextView total = text(rupiah(remainingPayment), 30, "#0F172A", true);
        total.setPadding(0, dp(8), 0, dp(12));
        payCard.addView(total);
        TextView note = text("QR di bawah hanya berisi nominal. Jika discan Google Lens akan muncul angka: " + remainingPayment, 14, "#64748B", false);
        payCard.addView(note);

        ImageView qr = new ImageView(this);
        qr.setBackgroundColor(Color.WHITE);
        qr.setPadding(dp(12), dp(12), dp(12), dp(12));
        qr.setImageBitmap(makeQr(String.valueOf(remainingPayment)));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(260), dp(260));
        qp.gravity = Gravity.CENTER_HORIZONTAL;
        qp.setMargins(0, dp(18), 0, dp(10));
        payCard.addView(qr, qp);

        TextView paid = button("Saya Sudah Bayar", true);
        paid.setOnClickListener(v -> markPaid());
        root.addView(paid, margin(0, 0, 0, 10));

        TextView cancel = text("Batal", 16, "#64748B", true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(14), 0, dp(14));
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel);
    }

    private void renderUnlocked() {
        TextView success = text("Pembayaran sudah lunas\nFile hasil dapat dilihat dan diunduh.", 18, "#16A34A", true);
        success.setGravity(Gravity.CENTER);
        success.setPadding(dp(16), dp(18), dp(16), dp(18));
        success.setBackgroundResource(R.drawable.bg_card);
        root.addView(success, margin(24, 0, 0, 18));

        if (isImage()) {
            LinearLayout box = new LinearLayout(this);
            box.setGravity(Gravity.CENTER);
            box.setBackgroundColor(Color.parseColor("#F1F5F9"));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(360));
            bp.setMargins(0, dp(4), 0, dp(18));
            root.addView(box, bp);
            ImageView img = new ImageView(this);
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            box.addView(img, new LinearLayout.LayoutParams(-1, -1));
            loadImage(img);
        } else if (isVideo()) {
            TextView videoPreview = text("🎬\nPreview Video\nFile video siap dibuka setelah pembayaran lunas", 19, "#0F172A", true);
            videoPreview.setGravity(Gravity.CENTER);
            videoPreview.setPadding(dp(20), dp(80), dp(20), dp(80));
            videoPreview.setBackgroundResource(R.drawable.bg_card);
            root.addView(videoPreview, margin(4, 0, 0, 18));
        } else {
            TextView file = text("📄\n" + resultName + "\nPreview tidak tersedia untuk tipe file ini.", 18, "#0F172A", true);
            file.setGravity(Gravity.CENTER);
            file.setPadding(dp(20), dp(60), dp(20), dp(60));
            file.setBackgroundResource(R.drawable.bg_card);
            root.addView(file, margin(4, 0, 0, 18));
        }

        TextView download = button("Download File", true);
        download.setOnClickListener(v -> downloadOrOpen());
        root.addView(download, margin(0, 0, 0, 12));

        TextView open = button("Buka File", false);
        open.setOnClickListener(v -> FileBase64Util.openFile(this, resultUrl, resultBase64, resultName, resultMime));
        root.addView(open);
    }

    private void markPaid() {
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("resultPaymentPaid", true);
        m.put("resultPaymentNominal", remainingPayment);
        m.put("resultPaymentAt", FieldValue.serverTimestamp());
        m.put("paymentStatus", "LUNAS");
        db.collection("orders").document(orderId).update(m).addOnSuccessListener(v -> {
            Toast.makeText(this, "Pembayaran berhasil", Toast.LENGTH_SHORT).show();
            resultPaid = true;
            root.removeAllViews();
            render();
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal memperbarui pembayaran: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadImage(ImageView img) {
        if (!resultBase64.isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(resultBase64, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                img.setImageBitmap(bmp);
            } catch (Exception e) {
                img.setImageResource(android.R.drawable.ic_menu_report_image);
            }
            return;
        }
        if (resultUrl.isEmpty()) {
            img.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(resultUrl).openConnection();
                c.connect();
                InputStream is = c.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                runOnUiThread(() -> img.setImageBitmap(bmp));
            } catch (Exception e) {
                runOnUiThread(() -> img.setImageResource(android.R.drawable.ic_menu_report_image));
            }
        });
    }

    private void downloadOrOpen() {
        if (resultUrl == null || resultUrl.isEmpty()) {
            FileBase64Util.openFile(this, resultUrl, resultBase64, resultName, resultMime);
            return;
        }
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(resultUrl));
            req.setTitle(resultName);
            req.setDescription("Mengunduh hasil editing KuEditin");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resultName);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) dm.enqueue(req);
            Toast.makeText(this, "File sedang diunduh ke folder Download", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal download, membuka file...", Toast.LENGTH_SHORT).show();
            FileBase64Util.openFile(this, resultUrl, resultBase64, resultName, resultMime);
        }
    }

    private boolean isImage() {
        String s = (resultMime + " " + resultName + " " + resultUrl).toLowerCase(Locale.ROOT);
        return s.contains("image") || s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png") || s.contains(".jpg") || s.contains(".jpeg") || s.contains(".png") || s.contains(".webp");
    }

    private boolean isVideo() {
        String s = (resultMime + " " + resultName + " " + resultUrl).toLowerCase(Locale.ROOT);
        return s.contains("video") || s.contains(".mp4") || s.contains(".mov") || s.contains(".mkv") || s.contains(".webm");
    }

    private Bitmap makeQr(String value) {
        try {
            int size = 720;
            BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
            return bmp;
        } catch (Exception e) {
            Bitmap bmp = Bitmap.createBitmap(360, 360, Bitmap.Config.RGB_565);
            bmp.eraseColor(Color.WHITE);
            return bmp;
        }
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(20), dp(20), dp(20));
        l.setBackgroundResource(R.drawable.bg_card);
        return l;
    }

    private TextView button(String s, boolean blue) {
        TextView tv = text(s, 17, blue ? "#FFFFFF" : "#1A73E8", true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(16), dp(16), dp(16));
        tv.setBackgroundResource(blue ? R.drawable.bg_btn : R.drawable.bg_card);
        return tv;
    }

    private TextView text(String s, int size, String color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(Color.parseColor(color));
        if (bold) tv.setTypeface(null, 1);
        return tv;
    }

    private LinearLayout.LayoutParams margin(int top, int left, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private String rupiah(long v) { return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", ""); }
    private String safe(String s) { return s == null ? "" : s; }
    private String safe(String s, String f) { return s == null || s.trim().isEmpty() ? f : s; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
