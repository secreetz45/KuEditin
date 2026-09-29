package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditorOrderDetailActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseFirestore db;
    private String orderId;

    private TextView txtStatusBadge, txtEditorTitle, txtEditorClient, txtEditorPackage, txtEditorPrice, txtEditorNetRevenue, txtEditorBrief;
    private TextView txtOpenBahanLabel, txtOpenResultLabel;
    private View btnOpenBahan, btnUploadResult, btnOpenResult, btnOpenCloudLink, btnCopyCloudLink;
    private View btnActionAccept, btnActionReview, btnActionFinish, btnAcceptRevisionContainer, btnAcceptRevision;

    private String fileBase64 = "", fileName = "", fileMime = "", fileUrl = "", cloudLink = "";
    private String resultBase64 = "", resultName = "", resultMime = "", resultUrl = "";

    private static final int REQ_RESULT = 501;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_editor_order_detail);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        bindViews();
        load();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        txtStatusBadge = findViewById(R.id.txt_status_badge);
        txtEditorTitle = findViewById(R.id.txt_editor_title);
        txtEditorClient = findViewById(R.id.txt_editor_client);
        txtEditorPackage = findViewById(R.id.txt_editor_package);
        txtEditorPrice = findViewById(R.id.txt_editor_price);
        txtEditorNetRevenue = findViewById(R.id.txt_editor_net_revenue);
        txtEditorBrief = findViewById(R.id.txt_editor_brief);

        if (txtEditorBrief != null) {
            txtEditorBrief.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
        }

        btnOpenBahan = findViewById(R.id.btn_open_bahan);
        txtOpenBahanLabel = findViewById(R.id.txt_open_bahan_label);

        btnOpenCloudLink = findViewById(R.id.btn_open_cloud_link);
        btnCopyCloudLink = findViewById(R.id.btn_copy_cloud_link);

        btnUploadResult = findViewById(R.id.btn_upload_result);
        btnOpenResult = findViewById(R.id.btn_open_result);
        txtOpenResultLabel = findViewById(R.id.txt_open_result_label);

        btnActionAccept = findViewById(R.id.btn_action_accept);
        btnActionReview = findViewById(R.id.btn_action_review);
        btnActionFinish = findViewById(R.id.btn_action_finish);
        btnAcceptRevisionContainer = findViewById(R.id.btn_accept_revision_container);
        btnAcceptRevision = findViewById(R.id.btn_accept_revision);

        if (btnActionAccept != null) btnActionAccept.setOnClickListener(v -> updateStatus("Diproses", 30));
        if (btnActionReview != null) btnActionReview.setOnClickListener(v -> updateStatus("Review Client", 80));
        if (btnActionFinish != null) btnActionFinish.setOnClickListener(v -> finishOrder());
        if (btnAcceptRevision != null) btnAcceptRevision.setOnClickListener(v -> updateStatus("Diproses (Revisi)", 50));

        if (btnUploadResult != null) btnUploadResult.setOnClickListener(v -> pickResult());

        if (btnOpenBahan != null) {
            btnOpenBahan.setOnClickListener(v -> FileBase64Util.openFile(this, fileUrl, fileBase64, fileName, fileMime));
        }

        if (btnOpenResult != null) {
            btnOpenResult.setOnClickListener(v -> FileBase64Util.openFile(this, resultUrl, resultBase64, resultName, resultMime));
        }
    }

    private void load() {
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Order tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("orders").document(orderId).get().addOnSuccessListener(this::render)
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void render(DocumentSnapshot d) {
        if (isFinishing() || d == null || !d.exists()) return;

        String client = safe(d.getString("clientName"), "Client");
        String service = safe(d.getString("service"), "Jasa Kreatif");
        String paket = safe(d.getString("packageName"), service);
        String judul = safe(d.getString("judul"), "Proyek Editing");
        String brief = safe(d.getString("brief"), "Tidak ada catatan brief khusus.");
        String status = safe(d.getString("status"), "Menunggu konfirmasi");

        Long priceVal = d.getLong("hargaMulai");
        if (priceVal == null) priceVal = d.getLong("harga");
        if (priceVal == null) priceVal = d.getLong("price");
        long price = priceVal == null ? 0 : priceVal;

        long platformFee = (price * 10) / 100;
        long netRevenue = price - platformFee;

        if (txtEditorTitle != null) txtEditorTitle.setText(judul);
        if (txtEditorClient != null) txtEditorClient.setText("Pemesan: " + client);
        if (txtEditorPackage != null) txtEditorPackage.setText(paket);
        if (txtEditorPrice != null) txtEditorPrice.setText(rupiah(price));
        if (txtEditorNetRevenue != null) txtEditorNetRevenue.setText(rupiah(netRevenue));
        if (txtEditorBrief != null) txtEditorBrief.setText(brief);

        // Status Badge Style
        if (txtStatusBadge != null) {
            txtStatusBadge.setText(status);
            String lowerStatus = status.toLowerCase();
            if (lowerStatus.contains("selesai") || lowerStatus.contains("done")) {
                txtStatusBadge.setTextColor(Color.parseColor("#166534"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_online);
            } else if (lowerStatus.contains("batal")) {
                txtStatusBadge.setTextColor(Color.parseColor("#991B1B"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_canceled);
            } else if (lowerStatus.contains("proses") || lowerStatus.contains("review") || lowerStatus.contains("bayar")) {
                txtStatusBadge.setTextColor(Color.parseColor("#92400E"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_working);
            } else {
                txtStatusBadge.setTextColor(Color.parseColor("#5D63FF"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            }
        }

        fileBase64 = safe(d.getString("fileBase64"));
        fileUrl = safe(d.getString("fileUrl"));
        fileName = safe(d.getString("fileName"), "file_bahan");
        fileMime = safe(d.getString("fileMime"), "*/*");

        resultBase64 = safe(d.getString("resultBase64"));
        resultUrl = safe(d.getString("resultUrl"));
        resultName = safe(d.getString("resultName"), "hasil_editing");
        resultMime = safe(d.getString("resultMime"), "*/*");

        // Action Button Visibility Rules based on Order Status
        boolean isPending = status.toLowerCase().contains("menunggu") || status.toLowerCase().contains("konfirmasi");
        boolean isRevision = status.toLowerCase().contains("revisi");

        if (btnActionAccept != null) btnActionAccept.setVisibility(isPending ? View.VISIBLE : View.GONE);
        if (btnActionReview != null) btnActionReview.setVisibility(!isPending ? View.VISIBLE : View.GONE);
        if (btnActionFinish != null) btnActionFinish.setVisibility(!isPending ? View.VISIBLE : View.GONE);
        if (btnUploadResult != null) btnUploadResult.setVisibility(!isPending ? View.VISIBLE : View.GONE);
        if (btnAcceptRevisionContainer != null) btnAcceptRevisionContainer.setVisibility(isRevision ? View.VISIBLE : View.GONE);

        boolean hasFile = !fileUrl.isEmpty() || !fileBase64.isEmpty();
        if (btnOpenBahan != null) {
            btnOpenBahan.setVisibility(hasFile ? View.VISIBLE : View.GONE);
            if (txtOpenBahanLabel != null && hasFile) {
                txtOpenBahanLabel.setText("Buka / Download File Bahan (" + fileName + ")");
            }
        }

        cloudLink = safe(d.getString("cloudLink"), "");
        if (cloudLink.isEmpty() && brief != null) {
            for (String word : brief.split("\\s+")) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    cloudLink = word.trim();
                    break;
                }
            }
        }

        final String finalCloudLink = cloudLink;
        boolean hasCloudLink = !finalCloudLink.isEmpty() && finalCloudLink.startsWith("http");

        if (btnOpenCloudLink != null) {
            btnOpenCloudLink.setVisibility(hasCloudLink ? View.VISIBLE : View.GONE);
            btnOpenCloudLink.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalCloudLink));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCopyCloudLink != null) {
            btnCopyCloudLink.setVisibility(hasCloudLink ? View.VISIBLE : View.GONE);
            btnCopyCloudLink.setOnClickListener(v -> {
                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Link Bahan Cloud", finalCloudLink);
                    if (clipboard != null) clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Link bahan berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal menyalin link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        boolean hasResult = !resultUrl.isEmpty() || !resultBase64.isEmpty();
        if (btnOpenResult != null) {
            btnOpenResult.setVisibility(hasResult ? View.VISIBLE : View.GONE);
            if (txtOpenResultLabel != null && hasResult) {
                txtOpenResultLabel.setText("Lihat File / Link Hasil (" + resultName + ")");
            }
        }

        View btnSendResultLink = findViewById(R.id.btn_send_result_link);
        if (btnSendResultLink != null) {
            btnSendResultLink.setVisibility(!isPending ? View.VISIBLE : View.GONE);
            btnSendResultLink.setOnClickListener(v -> showResultLinkDialog());
        }
    }

    private void pickResult() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_RESULT);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent data) {
        super.onActivityResult(r, c, data);
        if (r == REQ_RESULT && c == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Toast.makeText(this, "Mengunggah file hasil...", Toast.LENGTH_SHORT).show();

            CloudinaryUtil.uploadUri(this, uri, "mediamatch/hasil_editor", new CloudinaryUtil.Callback() {
                @Override
                public void onSuccess(String url, String publicId, String name, String mime) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("resultName", name);
                    m.put("resultMime", mime);
                    m.put("resultUrl", url);
                    m.put("resultBase64", "");
                    m.put("status", "Review Client");
                    m.put("progress", 90);
                    m.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("orders").document(orderId).update(m).addOnSuccessListener(v -> {
                        Toast.makeText(EditorOrderDetailActivity.this, "File hasil berhasil dikirim ke client!", Toast.LENGTH_SHORT).show();
                        load();
                    });
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(EditorOrderDetailActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void finishOrder() {
        if (resultUrl == null) resultUrl = "";
        if (resultBase64 == null) resultBase64 = "";
        if (resultUrl.trim().isEmpty() && resultBase64.trim().isEmpty()) {
            Toast.makeText(this, "Upload file hasil editing atau lampirkan link video hasil terlebih dahulu sebelum menyelesaikan proyek!", Toast.LENGTH_LONG).show();
            return;
        }
        updateStatus("Selesai", 100);
    }

    private void updateStatus(String status, int progress) {
        db.collection("orders").document(orderId)
                .update("status", status, "progress", progress, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Status order diperbarui ke " + status, Toast.LENGTH_SHORT).show();
                    load();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v) { return v == null ? "" : v; }
    private String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }

    private void showResultLinkDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Kirim Link Hasil Editing");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.textPrimary));
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Masukkan link Google Drive, Dropbox, YouTube, atau Vimeo hasil pengerjaan proyek kamu");
        sub.setTextSize(12);
        sub.setTextColor(getResources().getColor(R.color.textMuted));
        sub.setPadding(0, dp(4), 0, dp(14));
        box.addView(sub);

        EditText inputLink = new EditText(this);
        inputLink.setHint("https://drive.google.com/... atau YouTube link");
        inputLink.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        inputLink.setTextSize(14);
        inputLink.setTextColor(getResources().getColor(R.color.textPrimary));
        inputLink.setHintTextColor(getResources().getColor(R.color.textMuted));
        inputLink.setBackgroundResource(R.drawable.bg_input);
        inputLink.setPadding(dp(16), dp(12), dp(16), dp(12));
        if (resultUrl != null && !resultUrl.isEmpty()) inputLink.setText(resultUrl);
        box.addView(inputLink);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("Kirim Link Hasil", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.textMuted));
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String link = inputLink.getText().toString().trim();
                if (link.isEmpty()) {
                    inputLink.setError("Masukkan link hasil editing");
                    return;
                }

                Map<String, Object> m = new HashMap<>();
                m.put("resultUrl", link);
                m.put("resultName", "Link Hasil Editing");
                m.put("status", "Review Client");
                m.put("progress", 90);
                m.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("orders").document(orderId).update(m).addOnSuccessListener(x -> {
                    Toast.makeText(this, "Link hasil editing berhasil dikirim ke client!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    load();
                });
            });
        });

        dialog.show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
