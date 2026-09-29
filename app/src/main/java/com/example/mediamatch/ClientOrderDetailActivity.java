package com.example.mediamatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientOrderDetailActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseFirestore db;
    private String orderId;
    private String resultBase64 = "", resultName = "", resultMime = "", resultUrl = "";
    private String editorId = "", editorName = "";
    private String clientId = "", clientName = "", clientPhoto = "";
    private long remainingPayment = 0, totalPrice = 0;
    private boolean resultPaid = false;

    private TextView txtEditorName, txtPackage, txtStatusBadge, txtPrice;
    private TextView txtJudul, txtBrief, txtDeadline, txtProgress;
    private TextView btnOpenResult, btnGiveRating, btnAcceptProject, btnRequestRevision;
    private View btnResultContainer, btnRatingContainer, layoutClientReviewActions;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_client_order_detail);

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

        txtEditorName = findViewById(R.id.txt_order_editor_name);
        txtPackage = findViewById(R.id.txt_order_package);
        txtStatusBadge = findViewById(R.id.txt_order_status_badge);
        txtPrice = findViewById(R.id.txt_order_price);

        txtJudul = findViewById(R.id.txt_order_judul);
        txtBrief = findViewById(R.id.txt_order_brief);
        txtDeadline = findViewById(R.id.txt_order_deadline);
        txtProgress = findViewById(R.id.txt_order_progress_text);

        btnOpenResult = findViewById(R.id.btn_open_result);
        btnResultContainer = findViewById(R.id.btn_result_container);

        btnGiveRating = findViewById(R.id.btn_give_rating);
        btnRatingContainer = findViewById(R.id.btn_rating_container);

        layoutClientReviewActions = findViewById(R.id.layout_client_review_actions);
        btnAcceptProject = findViewById(R.id.btn_accept_project);
        btnRequestRevision = findViewById(R.id.btn_request_revision);

        if (btnOpenResult != null) {
            btnOpenResult.setOnClickListener(v -> openResultPreview());
        }

        if (btnAcceptProject != null) {
            btnAcceptProject.setOnClickListener(v -> acceptProjectAndFinish());
        }

        if (btnRequestRevision != null) {
            btnRequestRevision.setOnClickListener(v -> requestRevision());
        }
    }

    private void load() {
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Order tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(this::render)
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void render(DocumentSnapshot d) {
        if (d == null || !d.exists()) return;

        editorName = safe(d.getString("editorName"), "Editor");
        editorId = safe(d.getString("editorId"));
        clientId = safe(d.getString("clientId"));
        clientName = safe(d.getString("clientName"), "Client");
        clientPhoto = safe(d.getString("clientPhoto"), "");

        String paket = safe(d.getString("packageName"), safe(d.getString("service"), "Paket Jasa"));
        String status = safe(d.getString("status"), "Menunggu konfirmasi");

        Long price = d.getLong("price");
        totalPrice = price == null ? 0 : price;

        Long progress = d.getLong("progress");
        long progressVal = progress == null ? 0 : progress;

        Long remain = d.getLong("remainingPayment");
        if (remain == null) remain = Math.max(0, totalPrice / 2);
        remainingPayment = remain;

        Boolean paid = d.getBoolean("resultPaymentPaid");
        resultPaid = paid != null && paid;

        resultBase64 = safe(d.getString("resultBase64"));
        resultUrl = safe(d.getString("resultUrl"));
        resultName = safe(d.getString("resultName"), "hasil_editing");
        resultMime = safe(d.getString("resultMime"), "*/*");

        String deadline = "-";
        if (d.getTimestamp("deadline") != null) {
            deadline = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(d.getTimestamp("deadline").toDate());
        }

        // Bind data to views
        if (txtEditorName != null) txtEditorName.setText(editorName);
        if (txtPackage != null) txtPackage.setText(paket);
        if (txtPrice != null) txtPrice.setText(rupiah(totalPrice));
        if (txtJudul != null) txtJudul.setText(safe(d.getString("judul"), "-"));
        if (txtBrief != null) txtBrief.setText(safe(d.getString("brief"), "-"));
        if (txtDeadline != null) txtDeadline.setText(deadline);
        if (txtProgress != null) txtProgress.setText(progressVal + "%");

        // Status Badge Style
        if (txtStatusBadge != null) {
            txtStatusBadge.setText(status);
            String lower = status.toLowerCase();
            if (lower.contains("selesai") || lower.contains("done")) {
                txtStatusBadge.setTextColor(Color.parseColor("#166534"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_online);
            } else if (lower.contains("batal")) {
                txtStatusBadge.setTextColor(Color.parseColor("#991B1B"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_canceled);
            } else if (lower.contains("proses") || lower.contains("review") || lower.contains("mengerjakan") || lower.contains("bayar")) {
                txtStatusBadge.setTextColor(Color.parseColor("#92400E"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_working);
            } else {
                txtStatusBadge.setTextColor(Color.parseColor("#5D63FF"));
                txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            }
        }

        // Result Button Status
        boolean hasResult = !resultUrl.isEmpty() || !resultBase64.isEmpty();
        boolean isDone = status.toLowerCase().contains("selesai");

        String resultText;
        if (!hasResult) {
            resultText = "File hasil belum dikirim editor";
        } else if (resultPaid || remainingPayment <= 0) {
            resultText = "Preview / Download File Hasil (" + resultName + ")";
        } else {
            resultText = "Preview hasil terkunci (Lunasi " + rupiah(remainingPayment) + ")";
        }

        if (btnOpenResult != null) {
            btnOpenResult.setText(resultText);
            btnOpenResult.setEnabled(hasResult);
        }

        if (btnResultContainer != null) {
            if (btnResultContainer instanceof MaterialCardView) {
                ((MaterialCardView) btnResultContainer).setCardBackgroundColor(Color.parseColor(hasResult ? "#5D63FF" : "#CBD5E1"));
            }
        }

        // Client Review & Revision Action Buttons
        if (layoutClientReviewActions != null) {
            layoutClientReviewActions.setVisibility((hasResult && !isDone) ? View.VISIBLE : View.GONE);
        }

        // Rating Button
        boolean alreadyRated = d.getBoolean("clientRated") != null && d.getBoolean("clientRated");

        if (btnRatingContainer != null) {
            btnRatingContainer.setVisibility(isDone ? View.VISIBLE : View.GONE);
        }

        if (btnGiveRating != null) {
            if (alreadyRated) {
                btnGiveRating.setText("Rating Sudah Diberikan");
                btnGiveRating.setEnabled(false);
                btnGiveRating.setTextColor(Color.parseColor("#64748B"));
            } else {
                btnGiveRating.setText("Beri Penilaian & Rating untuk Editor");
                btnGiveRating.setEnabled(true);
                btnGiveRating.setTextColor(Color.parseColor("#5D63FF"));
                btnGiveRating.setOnClickListener(v -> showRatingDialog());
            }
        }
    }

    private void acceptProjectAndFinish() {
        if (orderId == null || orderId.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put("status", "Selesai");
        update.put("progress", 100);
        update.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("orders").document(orderId).update(update)
                .addOnSuccessListener(v -> {
                    long platformFee = (totalPrice * 10) / 100;
                    long editorNetRevenue = totalPrice - platformFee;

                    if (editorId != null && !editorId.isEmpty() && editorNetRevenue > 0) {
                        db.collection("user").document(editorId)
                                .update("saldo", FieldValue.increment(editorNetRevenue));
                    }

                    Toast.makeText(this, "Selamat! Proyek diterima & telah selesai.", Toast.LENGTH_SHORT).show();
                    load();
                    showRatingDialog();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memperbarui status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void requestRevision() {
        if (orderId == null || orderId.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put("status", "Revisi Client");
        update.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("orders").document(orderId).update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Pengajuan revisi terkirim ke editor. Buka chat untuk diskusi revisi.", Toast.LENGTH_LONG).show();
                    load();

                    Intent i = new Intent(this, ChatRoomActivity.class);
                    i.putExtra("otherId", editorId);
                    i.putExtra("otherName", editorName);
                    startActivity(i);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengajukan revisi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openResultPreview() {
        Intent i = new Intent(this, ResultPreviewActivity.class);
        i.putExtra("orderId", orderId);
        i.putExtra("resultUrl", resultUrl);
        i.putExtra("resultBase64", resultBase64);
        i.putExtra("resultName", resultName);
        i.putExtra("resultMime", resultMime);
        i.putExtra("remainingPayment", remainingPayment);
        i.putExtra("resultPaid", resultPaid || remainingPayment <= 0);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderId != null) load();
    }

    private void showRatingDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = text("Bagaimana hasil kerjanya?", 18, getResources().getColor(R.color.textPrimary), true);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = text("Berikan penilaian & ulasan untuk " + safe(editorName, "Editor"), 13, getResources().getColor(R.color.textMuted), false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, 0);
        box.addView(sub);

        LinearLayout stars = new LinearLayout(this);
        stars.setOrientation(LinearLayout.HORIZONTAL);
        stars.setGravity(Gravity.CENTER);
        stars.setPadding(0, dp(18), 0, dp(18));

        final int[] rating = {5};
        TextView[] starViews = new TextView[5];
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            TextView star = text("★", 38, "#F59E0B", false);
            star.setGravity(Gravity.CENTER);
            star.setOnClickListener(v -> {
                rating[0] = value;
                updateStars(starViews, rating[0]);
            });
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(44), dp(44));
            stars.addView(star, sp);
            starViews[i] = star;
        }
        updateStars(starViews, rating[0]);
        box.addView(stars);

        EditText review = new EditText(this);
        review.setHint("Tulis ulasan pengalaman kamu mengenai hasil proyek ini...");
        review.setMinLines(3);
        review.setGravity(Gravity.TOP | Gravity.START);
        review.setBackgroundResource(R.drawable.bg_input);
        review.setPadding(dp(16), dp(14), dp(16), dp(14));
        review.setTextColor(getResources().getColor(R.color.textPrimary));
        review.setHintTextColor(getResources().getColor(R.color.textMuted));
        review.setTextSize(14);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(110));
        box.addView(review, rp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("Nanti", null)
                .setPositiveButton("Kirim Penilaian", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.textMuted));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String comment = review.getText().toString().trim();
                if (comment.isEmpty()) {
                    Toast.makeText(this, "Mohon berikan ulasan singkat", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitRating(rating[0], comment);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateStars(TextView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < rating ? "★" : "☆");
            stars[i].setTextColor(i < rating ? Color.parseColor("#F59E0B") : getResources().getColor(R.color.textMuted));
        }
    }

    private void submitRating(int rating, String review) {
        if (orderId == null || orderId.isEmpty() || editorId == null || editorId.isEmpty()) {
            Toast.makeText(this, "Data order tidak lengkap", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("editorId", editorId);
        data.put("editorName", editorName);
        data.put("clientId", clientId);
        data.put("clientName", clientName);
        data.put("clientPhoto", clientPhoto);
        data.put("rating", rating);
        data.put("review", review);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("ratings").document(orderId)
                .set(data)
                .addOnSuccessListener(ok -> db.collection("orders").document(orderId)
                        .update("clientRated", true, "rating", rating, "review", review)
                        .addOnSuccessListener(v -> updateEditorRating())
                        .addOnFailureListener(e -> Toast.makeText(this, "Rating tersimpan, tetapi order gagal diperbarui", Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim rating: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateEditorRating() {
        db.collection("ratings").whereEqualTo("editorId", editorId).get()
                .addOnSuccessListener(qs -> {
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot r : qs.getDocuments()) {
                        Double val = r.getDouble("rating");
                        if (val == null) {
                            Long lv = r.getLong("rating");
                            if (lv != null) val = lv.doubleValue();
                        }
                        if (val != null) {
                            total += val;
                            count++;
                        }
                    }
                    double avg = count == 0 ? 0 : total / count;
                    Map<String, Object> m = new HashMap<>();
                    m.put("rating", avg);
                    m.put("reviewCount", count);
                    db.collection("user").document(editorId).update(m)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Terima kasih, rating berhasil dikirim", Toast.LENGTH_LONG).show();
                                load();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Rating berhasil dikirim", Toast.LENGTH_LONG).show();
                                load();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Rating berhasil dikirim", Toast.LENGTH_LONG).show();
                    load();
                });
    }

    private TextView text(String s, int size, int colorInt, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(colorInt);
        if (bold) tv.setTypeface(null, 1);
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

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v) { return v == null ? "" : v; }
    private String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
