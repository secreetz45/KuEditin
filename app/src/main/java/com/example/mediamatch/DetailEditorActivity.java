package com.example.mediamatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailEditorActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String editorId;
    private String editorName = "Editor";
    private String service = "Editor";
    private String price = "Rp0";
    private String rating = "0.0";
    private String bio = "";
    private final List<String> skills = new ArrayList<>();

    private TextView txtName, txtJob, txtBio, txtStatRating, txtStatOrders, txtStatPrice, txtStatExperience, txtStatus, txtSummaryScore, txtSummarySub;
    private CircleImageView avatar;
    private ImageView imgVerified;
    private LinearLayout skillContainer, portfolioContainer, reviewContainer, softwareContainer;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_detail_editor);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        editorId = getIntent().getStringExtra("editorId");

        txtName = findViewById(R.id.txt_editor_name);
        txtJob = findViewById(R.id.txt_editor_job);
        txtStatRating = findViewById(R.id.txt_stat_rating);
        txtStatPrice = findViewById(R.id.txt_stat_price);
        txtStatOrders = findViewById(R.id.txt_stat_orders);
        txtStatExperience = findViewById(R.id.txt_stat_experience);
        txtBio = findViewById(R.id.txt_editor_bio);
        txtStatus = findViewById(R.id.txt_editor_status);
        txtSummaryScore = findViewById(R.id.txt_summary_score);
        txtSummarySub = findViewById(R.id.txt_review_summary_sub);
        imgVerified = findViewById(R.id.img_verified);
        avatar = findViewById(R.id.editor_avatar);
        skillContainer = findViewById(R.id.container_skills);
        portfolioContainer = findViewById(R.id.container_portfolio);
        reviewContainer = findViewById(R.id.container_reviews);
        softwareContainer = findViewById(R.id.container_software_tools);
        loading = findViewById(R.id.progress_detail);

        if (txtStatRating != null) {
            txtStatRating.setClickable(true);
            txtStatRating.setFocusable(true);
            txtStatRating.setOnClickListener(v -> scrollToReviews());
        }

        if (txtSummaryScore != null) {
            txtSummaryScore.setClickable(true);
            txtSummaryScore.setFocusable(true);
            txtSummaryScore.setOnClickListener(v -> scrollToReviews());
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_chat_editor).setOnClickListener(v -> openOrCreateChat());
        findViewById(R.id.btn_order_editor).setOnClickListener(v -> {
            Intent i = new Intent(this, DetailServiceActivity.class);
            i.putExtra("editorId", editorId);
            startActivity(i);
        });

        loadEditor();
        loadReviews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    private void loadReviews() {
        if (reviewContainer == null) return;
        reviewContainer.removeAllViews();
        db.collection("ratings").whereEqualTo("editorId", editorId).get()
                .addOnSuccessListener(qs -> {
                    if (isFinishing()) return;
                    if (qs == null || qs.isEmpty()) {
                        if (txtSummarySub != null) txtSummarySub.setText("Belum ada ulasan dari klien sejauh ini");
                        reviewContainer.addView(createEmptyReviewCard());
                        return;
                    }

                    int count = qs.size();
                    double sumRating = 0;
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Double val = doc.getDouble("rating");
                        if (val == null) {
                            Long lv = doc.getLong("rating");
                            if (lv != null) val = lv.doubleValue();
                        }
                        if (val != null) sumRating += val;
                    }
                    double avgRating = count == 0 ? 0.0 : Math.min(5.0, Math.max(0.0, sumRating / count));

                    if (txtSummaryScore != null) {
                        txtSummaryScore.setText("★ " + String.format(Locale.US, "%.1f", avgRating) + " (" + count + ")");
                    }
                    if (txtStatRating != null) {
                        txtStatRating.setText(String.format(Locale.US, "%.1f", avgRating));
                    }
                    if (txtSummarySub != null) {
                        txtSummarySub.setText("Berdasarkan " + count + " ulasan pengalaman klien");
                    }

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        reviewContainer.addView(createReviewCard(doc));
                    }
                })
                .addOnFailureListener(e -> {
                    if (txtSummarySub != null) txtSummarySub.setText("Belum ada ulasan dari klien");
                    reviewContainer.addView(createEmptyReviewCard());
                });
    }

    private View createEmptyReviewCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cardWidth = Math.max(dp(280), screenWidth - dp(40));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, -2);
        card.setLayoutParams(lp);

        TextView tv = text("Belum Ada Ulasan Klien\nUlasan dan penilaian kinerja editor akan muncul di sini.", 13, getResources().getColor(R.color.textMuted), false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(20), dp(22), dp(20), dp(22));
        card.addView(tv);
        return card;
    }

    private View createReviewCard(DocumentSnapshot d) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cardWidth = Math.max(dp(280), (int) (screenWidth * 0.82f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, -2);
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(box);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top);

        CircleImageView clientAvatar = new CircleImageView(this);
        clientAvatar.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        clientAvatar.setBackgroundResource(R.drawable.circle_soft);
        top.addView(clientAvatar);

        String photo = d.getString("clientPhoto");
        if (photo != null && !photo.isEmpty()) ImageLoader.load(clientAvatar, photo);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1);
        ip.setMargins(dp(10), 0, dp(4), 0);
        top.addView(info, ip);

        TextView clientNameTv = text(safe(d.getString("clientName"), "Klien KuEditin"), 14, getResources().getColor(R.color.textPrimary), true);
        info.addView(clientNameTv);

        TextView tagTv = text("Klien Terverifikasi", 11, getResources().getColor(R.color.textMuted), false);
        info.addView(tagTv);

        double ratingVal = 0;
        Object rObj = d.get("rating");
        if (rObj instanceof Number) ratingVal = Math.min(5.0, Math.max(0.0, ((Number) rObj).doubleValue()));

        TextView ratingBadge = new TextView(this);
        ratingBadge.setText("★ " + String.format(Locale.US, "%.1f", ratingVal));
        ratingBadge.setTextColor(getResources().getColor(R.color.textPrimary));
        ratingBadge.setBackgroundResource(R.drawable.bg_chip);
        ratingBadge.setTextSize(12);
        ratingBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        ratingBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        top.addView(ratingBadge);

        TextView comment = text(safe(d.getString("review"), "Tidak ada komentar."), 12, getResources().getColor(R.color.textPrimary), false);
        comment.setPadding(0, dp(6), 0, 0);
        comment.setMaxLines(3);
        comment.setEllipsize(android.text.TextUtils.TruncateAt.END);
        box.addView(comment);

        return card;
    }

    private void loadEditor() {
        loading.setVisibility(View.VISIBLE);
        db.collection("user").document(editorId).get()
                .addOnSuccessListener(doc -> {
                    loading.setVisibility(View.GONE);
                    if (!doc.exists()) {
                        Toast.makeText(this, "Editor tidak ditemukan", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bindEditor(doc);
                    renderUI();
                })
                .addOnFailureListener(e -> {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void bindEditor(DocumentSnapshot doc) {
        editorName = safe(doc.getString("nama"), "Editor");
        bio = safe(doc.getString("deskripsi"), "Belum ada deskripsi profil tentang editor ini.");
        skills.clear();
        Object skillObj = doc.get("skill");
        if (skillObj instanceof List) {
            for (Object s : (List<?>) skillObj) skills.add(String.valueOf(s));
        }
        service = skills.isEmpty() ? "Editor" : toService(skills.get(0));

        Double r = doc.getDouble("rating");
        double ratingNum = r == null ? 0.0 : Math.min(5.0, Math.max(0.0, r));
        rating = String.format(Locale.US, "%.1f", ratingNum);

        Long harga = doc.getLong("hargaMulai");
        price = formatPrice(harga == null ? 0 : harga);
        Long totalProject = doc.getLong("totalOrder");

        // Verified approval check
        boolean isVerified = false;
        Boolean verObj = doc.getBoolean("isVerified");
        if (verObj == null) verObj = doc.getBoolean("isBestEditor");
        if (verObj == null) verObj = doc.getBoolean("isTerbaik");
        if (verObj == null) verObj = doc.getBoolean("isApproved");
        if (verObj != null) isVerified = verObj;

        String statusStr = doc.getString("status");
        if ("approved".equalsIgnoreCase(statusStr) || "terbaik".equalsIgnoreCase(statusStr) || "best_editor".equalsIgnoreCase(statusStr)) {
            isVerified = true;
        }

        if (imgVerified != null) {
            imgVerified.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        // Online & Working Status
        boolean isOnline = false;
        Boolean onlineObj = doc.getBoolean("isOnline");
        if (onlineObj != null) isOnline = onlineObj;

        boolean isWorking = true;
        Boolean workingObj = doc.getBoolean("isWorking");
        if (workingObj != null) isWorking = workingObj;

        final boolean editorIsWorking = isWorking;

        long lastSeen = 0;
        com.google.firebase.Timestamp ts = doc.getTimestamp("lastSeen");
        if (ts != null) lastSeen = ts.toDate().getTime();

        boolean actuallyOnline = isOnline && lastSeen > 0 && (System.currentTimeMillis() - lastSeen < 180000);

        if (txtStatus != null) {
            if (!editorIsWorking) {
                txtStatus.setText("● Editor Sedang Sibuk (Off Order)");
                txtStatus.setTextColor(Color.parseColor("#991B1B"));
                txtStatus.setBackgroundResource(R.drawable.bg_badge_canceled);
            } else if (actuallyOnline) {
                txtStatus.setText("● Online & Siap Order");
                txtStatus.setTextColor(Color.parseColor("#166534"));
                txtStatus.setBackgroundResource(R.drawable.bg_badge_online);
            } else {
                String lastText = PresenceUtil.lastOnlineText(lastSeen);
                txtStatus.setText("● " + lastText);
                txtStatus.setTextColor(Color.parseColor("#64748B"));
                txtStatus.setBackgroundResource(R.drawable.bg_badge_skill);
            }
        }

        View btnOrder = findViewById(R.id.btn_order_editor);
        if (btnOrder instanceof TextView) {
            TextView orderTv = (TextView) btnOrder;
            if (!editorIsWorking) {
                orderTv.setText("Editor Sedang Sibuk");
                orderTv.setTextColor(Color.parseColor("#64748B"));
                orderTv.setBackgroundResource(R.drawable.bg_chip);
            } else {
                orderTv.setText("Lihat Paket Jasa");
                orderTv.setTextColor(Color.WHITE);
                orderTv.setBackgroundResource(R.drawable.bg_btn);
            }

            orderTv.setOnClickListener(v -> {
                if (!editorIsWorking) {
                    Toast.makeText(this, "Editor ini sedang sibuk / tidak menerima order saat ini.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(this, DetailServiceActivity.class);
                i.putExtra("editorId", editorId);
                startActivity(i);
            });
        }

        String photo = safe(doc.getString("fotoProfil"), "");
        if (!photo.isEmpty()) ImageLoader.load(avatar, photo);

        if (txtName != null) txtName.setText(editorName);
        if (txtJob != null) txtJob.setText(service);

        if (txtStatRating != null) {
            txtStatRating.setText(rating);
            if (ratingNum > 0) {
                txtStatRating.setTextColor(Color.parseColor("#D97706"));
                if (txtSummaryScore != null) {
                    txtSummaryScore.setText("Rating " + rating);
                    txtSummaryScore.setTextColor(Color.parseColor("#B45309"));
                    txtSummaryScore.setBackgroundResource(R.drawable.bg_badge_rating);
                }
            } else {
                txtStatRating.setTextColor(getResources().getColor(R.color.textMuted));
                if (txtSummaryScore != null) {
                    txtSummaryScore.setText("Rating 0.0");
                    txtSummaryScore.setTextColor(getResources().getColor(R.color.textMuted));
                    txtSummaryScore.setBackgroundResource(R.drawable.bg_badge_skill);
                }
            }
        }

        if (txtStatPrice != null) txtStatPrice.setText(price);
        if (txtStatOrders != null) txtStatOrders.setText(String.valueOf(totalProject == null ? 0 : totalProject));

        // Experience Years Stat
        String expYears = safe(doc.getString("experienceYears"), safe(doc.getString("pengalaman"), ""));
        if (txtStatExperience != null) {
            txtStatExperience.setText(expYears.isEmpty() ? "-" : expYears);
        }

        if (txtBio != null) txtBio.setText(bio);

        // Load Portfolio Images
        if (portfolioContainer != null) {
            portfolioContainer.removeAllViews();
            List<String> portImg = new ArrayList<>();
            Object pObj = doc.get("portfolioImages");
            if (pObj == null) pObj = doc.get("sampleImages");
            if (pObj instanceof List) {
                for (Object p : (List<?>) pObj) {
                    if (p != null && !p.toString().trim().isEmpty()) {
                        portImg.add(p.toString().trim());
                    }
                }
            }

            String sampleUrl = safe(doc.getString("sampleUrl"), safe(doc.getString("sampleLink"), ""));
            if (portImg.isEmpty() && !sampleUrl.isEmpty() && sampleUrl.startsWith("http")) {
                portImg.add(sampleUrl);
            }

            if (portImg.isEmpty()) {
                for (int i = 0; i < 2; i++) portfolioContainer.addView(createPortfolioItem(null));
            } else {
                for (String url : portImg) portfolioContainer.addView(createPortfolioItem(url));
            }
        }

        // Load Software & Tools Used (Filled directly by Editor without hardcoded icons)
        if (softwareContainer != null) {
            softwareContainer.removeAllViews();
            String toolsStr = safe(doc.getString("softwareTools"), safe(doc.getString("toolsUsed"), ""));
            renderSoftwareTools(toolsStr);
        }
    }

    private void renderSoftwareTools(String toolsStr) {
        if (softwareContainer == null) return;
        softwareContainer.removeAllViews();

        if (toolsStr.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("Belum ada software / aplikasi yang ditambahkan oleh editor ini.");
            emptyTv.setTextColor(Color.parseColor("#64748B"));
            emptyTv.setTextSize(13);
            softwareContainer.addView(emptyTv);
            return;
        }

        String[] tools = toolsStr.split(",");
        LinearLayout currentRow = new LinearLayout(this);
        currentRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        currentRow.setLayoutParams(rowLp);

        int count = 0;
        for (String t : tools) {
            String toolName = t.trim();
            if (toolName.isEmpty()) continue;

            if (count >= 2) {
                softwareContainer.addView(currentRow);
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.setMargins(0, 0, 0, dp(8));
                currentRow.setLayoutParams(rowLp);
                count = 0;
            }

            TextView chip = new TextView(this);
            chip.setText(toolName);
            chip.setTextColor(getResources().getColor(R.color.textPrimary));
            chip.setTextSize(13);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackgroundResource(R.drawable.bg_badge_skill);

            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, -2, 1);
            chipLp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(chipLp);

            currentRow.addView(chip);
            count++;
        }

        if (count > 0) {
            softwareContainer.addView(currentRow);
        }
    }

    private View createPortfolioItem(String url) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setStrokeColor(Color.parseColor("#F1F5F9"));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(210), dp(140));
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (url != null && !url.trim().isEmpty() && url.startsWith("http")) {
            ImageLoader.load(img, url.trim());
            card.setOnClickListener(v -> showImageFullscreenDialog(url.trim()));
        } else {
            img.setImageResource(R.drawable.bg_home_header);
        }
        card.addView(img);
        return card;
    }

    private void showImageFullscreenDialog(String url) {
        if (url == null || url.trim().isEmpty() || isFinishing()) return;

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#0A0F1D"));

        // Main Fullscreen Image View
        ImageView img = new ImageView(this);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imgLp.gravity = Gravity.CENTER;
        img.setLayoutParams(imgLp);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setPadding(dp(16), dp(80), dp(16), dp(16));
        ImageLoader.load(img, url.trim());
        root.addView(img);

        // Top Header Bar Overlay
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), dp(48), dp(16), dp(14));
        topBar.setBackgroundColor(Color.parseColor("#B30A0F1D"));
        topBar.setClipChildren(false);
        topBar.setClipToPadding(false);

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        root.addView(topBar, topLp);

        // Back Circle Button
        ImageView btnClose = new ImageView(this);
        btnClose.setImageResource(R.drawable.ic_back_ios);
        btnClose.setColorFilter(Color.parseColor("#5D63FF"));
        btnClose.setPadding(dp(10), dp(10), dp(10), dp(10));
        btnClose.setBackgroundResource(R.drawable.circle_white_elevated);
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        topBar.addView(btnClose, closeLp);

        TextView titleTv = new TextView(this);
        titleTv.setText("Pratinjau Foto");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(17);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(dp(14), 0, 0, 0);
        topBar.addView(titleTv, titleLp);

        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        img.setOnClickListener(v -> dialog.dismiss());
        root.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void renderUI() {
        if (skillContainer == null) return;
        skillContainer.removeAllViews();

        if (skills.isEmpty()) {
            TextView chip = new TextView(this);
            chip.setText(service);
            chip.setTextColor(Color.parseColor("#5D63FF"));
            chip.setTextSize(13);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackgroundResource(R.drawable.bg_badge_skill);
            skillContainer.addView(chip);
            return;
        }

        LinearLayout currentRow = new LinearLayout(this);
        currentRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        currentRow.setLayoutParams(rowLp);

        int itemsInRow = 0;
        for (String s : skills) {
            if (itemsInRow >= 2) {
                skillContainer.addView(currentRow);
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.setMargins(0, 0, 0, dp(8));
                currentRow.setLayoutParams(rowLp);
                itemsInRow = 0;
            }

            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setTextColor(Color.parseColor("#5D63FF"));
            chip.setTextSize(13);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackgroundResource(R.drawable.bg_badge_skill);

            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, -2, 1);
            chipLp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(chipLp);

            currentRow.addView(chip);
            itemsInRow++;
        }

        if (itemsInRow > 0) {
            skillContainer.addView(currentRow);
        }
    }

    private void openOrCreateChat() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        String myUid = auth.getCurrentUser().getUid();
        if (myUid.equals(editorId)) {
            Toast.makeText(this, "Ini akun Anda sendiri", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("user").document(myUid).get().addOnSuccessListener(myDoc -> {
            String myName = safe(myDoc.getString("nama"), "Client");
            String myRole = safe(myDoc.getString("role"), "client");

            db.collection("chats")
                    .whereArrayContains("participantIds", myUid)
                    .get()
                    .addOnSuccessListener(qs -> {
                        DocumentSnapshot existing = null;
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            List<?> p = (List<?>) doc.get("participantIds");
                            if (p != null && p.contains(editorId)) {
                                existing = doc;
                                break;
                            }
                        }

                        if (existing != null) {
                            Intent i = new Intent(this, ChatRoomActivity.class);
                            i.putExtra("chatId", existing.getId());
                            i.putExtra("otherName", editorName);
                            startActivity(i);
                            TransitionUtil.slideRight(this);
                        } else {
                            Map<String, Object> newChat = new HashMap<>();
                            newChat.put("participantIds", Arrays.asList(myUid, editorId));
                            if ("editor".equalsIgnoreCase(myRole)) {
                                newChat.put("editorId", myUid);
                                newChat.put("editorName", myName);
                                newChat.put("clientId", editorId);
                                newChat.put("clientName", editorName);
                            } else {
                                newChat.put("clientId", myUid);
                                newChat.put("clientName", myName);
                                newChat.put("editorId", editorId);
                                newChat.put("editorName", editorName);
                            }
                            newChat.put("lastMessage", "Saling menyapa");
                            newChat.put("updatedAt", FieldValue.serverTimestamp());

                            db.collection("chats").add(newChat).addOnSuccessListener(ref -> {
                                Intent i = new Intent(this, ChatRoomActivity.class);
                                i.putExtra("chatId", ref.getId());
                                i.putExtra("otherName", editorName);
                                startActivity(i);
                                TransitionUtil.slideRight(this);
                            });
                        }
                    });
        });
    }

    private static String toService(String skill) {
        String s = skill == null ? "" : skill.toLowerCase();
        if (s.contains("video")) return "Video Editor";
        if (s.contains("ui") || s.contains("ux")) return "UI/UX Designer";
        if (s.contains("foto") || s.contains("photo")) return "Foto Editor";
        if (s.contains("motion")) return "Motion Graphic";
        if (s.contains("logo")) return "Logo Designer";
        if (s.contains("design")) return "Graphic Designer";
        return skill == null || skill.isEmpty() ? "Editor" : skill;
    }

    private TextView text(String s, int sp, int colorInt, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(colorInt);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView text(String s, int sp, String color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(Color.parseColor(color));
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private String formatPrice(long value) {
        if (value <= 0) return "Rp0";
        if (value >= 1000) return "Rp" + (value / 1000) + "K";
        return "Rp" + value;
    }

    private static String safe(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }

    private void scrollToReviews() {
        android.widget.ScrollView scroll = findViewById(R.id.detail_editor_scroll);
        if (reviewContainer != null && scroll != null) {
            final android.widget.ScrollView finalScroll = scroll;
            finalScroll.post(() -> finalScroll.smoothScrollTo(0, reviewContainer.getTop()));
        } else {
            Toast.makeText(this, "Membuka ulasan klien...", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
