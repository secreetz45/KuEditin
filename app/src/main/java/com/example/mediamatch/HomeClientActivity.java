package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeClientActivity extends androidx.appcompat.app.AppCompatActivity {
    private LinearLayout editorContainer;
    private EditText searchEditor;
    private String selectedCategory = "";
    private final List<EditorItem> editors = new ArrayList<>();
    private final List<String> userInterests = new ArrayList<>();
    private ListenerRegistration editorListener;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Pagination
    private int currentPage = 1;
    private static final int PAGE_SIZE = 6; // 6 items = 3 rows of 2 columns

    @Override
    public void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_home_client);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavUtil.saveRole(this, "client");

        editorContainer = findViewById(R.id.editor_container);
        searchEditor = findViewById(R.id.search_editor);

        setupCategory(R.id.cat_video, "Video");
        setupCategory(R.id.cat_uiux, "UI/UX");
        setupCategory(R.id.cat_foto, "Foto");
        setupCategory(R.id.cat_motion, "Motion");

        findViewById(R.id.btn_show_all).setOnClickListener(v -> {
            startActivity(new Intent(HomeClientActivity.this, AllEditorsActivity.class));
            TransitionUtil.slideRight(this);
        });
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            startActivity(new Intent(HomeClientActivity.this, NotificationsActivity.class));
            TransitionUtil.slideUp(this);
        });

        View logoM = findViewById(R.id.logoM);
        if (logoM != null) {
            logoM.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            if (logoM.getParent() instanceof View) {
                ((View) logoM.getParent()).setOnClickListener(v -> ThemeUtil.toggleTheme(this));
            }
        }

        View btnLayoutMode = findViewById(R.id.btn_layout_mode);
        if (btnLayoutMode != null) {
            btnLayoutMode.setOnClickListener(v -> {
                int currentCols = getSharedPreferences("app_settings", MODE_PRIVATE).getInt("feed_grid_mode", 2);
                int nextCols = currentCols == 1 ? 2 : 1;
                getSharedPreferences("app_settings", MODE_PRIVATE).edit().putInt("feed_grid_mode", nextCols).apply();
                updateLayoutModeIcon();
                Toast.makeText(this, nextCols == 1 ? "Mode 1 Kolom (List) Aktif" : "Mode 2 Kolom (Grid) Aktif", Toast.LENGTH_SHORT).show();
                filterEditors();
            });
        }
        updateLayoutModeIcon();

        searchEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 1;
                filterEditors();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        NavUtil.setupClientNav(this, 0);
        ChatUnreadUtil.listenUnreadBadge((TextView) findViewById(R.id.nav_chat), false);

        updateCategoryUI();
        loadUserInterests();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (editorListener != null) editorListener.remove();
    }

    private void loadUserInterests() {
        if (auth.getCurrentUser() == null) {
            loadEditorsFromFirebase();
            return;
        }

        db.collection("user").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    userInterests.clear();
                    Object interestObj = doc.get("interests");
                    if (interestObj instanceof List) {
                        for (Object interest : (List<?>) interestObj) {
                            userInterests.add(String.valueOf(interest).toLowerCase().trim());
                        }
                    }
                    Object onbObj = doc.get("onboarding_interests");
                    if (onbObj instanceof List) {
                        for (Object interest : (List<?>) onbObj) {
                            userInterests.add(String.valueOf(interest).toLowerCase().trim());
                        }
                    }
                    loadEditorsFromFirebase();
                })
                .addOnFailureListener(e -> loadEditorsFromFirebase());
    }

    private void loadEditorsFromFirebase() {
        editorContainer.removeAllViews();
        editorContainer.addView(createLoadingState());

        if (editorListener != null) editorListener.remove();
        editorListener = db.collection("user")
                .whereEqualTo("role", "editor")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        editorContainer.removeAllViews();
                        editorContainer.addView(createMessageState("Gagal memuat editor: " + e.getMessage()));
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        editors.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            editors.add(EditorItem.fromFirestore(doc));
                        }
                        filterEditors();
                    }
                });
    }

    private void setupCategory(int id, String category) {
        findViewById(id).setOnClickListener(v -> {
            selectedCategory = category.equals(selectedCategory) ? "" : category;
            currentPage = 1;
            updateCategoryUI();
            filterEditors();
        });
    }

    private void updateCategoryUI() {
        updateOneCategory(R.id.cat_video, R.id.cat_video_text, "Video");
        updateOneCategory(R.id.cat_uiux, R.id.cat_uiux_text, "UI/UX");
        updateOneCategory(R.id.cat_foto, R.id.cat_foto_text, "Foto");
        updateOneCategory(R.id.cat_motion, R.id.cat_motion_text, "Motion");
    }

    private void updateOneCategory(int boxId, int textId, String category) {
        View box = findViewById(boxId);
        TextView text = findViewById(textId);
        if (box == null || text == null) return;
        boolean active = category.equals(selectedCategory);
        if (box instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) box;
            card.setStrokeColor(active ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.borderLight));
            card.setStrokeWidth(dp(active ? 2 : 1));
            card.setCardBackgroundColor(active ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.cardSurface));

            if (card.getChildCount() > 0 && card.getChildAt(0) instanceof LinearLayout) {
                LinearLayout innerLayout = (LinearLayout) card.getChildAt(0);
                for (int i = 0; i < innerLayout.getChildCount(); i++) {
                    View child = innerLayout.getChildAt(i);
                    if (child instanceof ImageView) {
                        ImageView img = (ImageView) child;
                        if (active) {
                            img.setColorFilter(Color.WHITE);
                        } else {
                            img.clearColorFilter();
                        }
                    }
                }
            }
        } else {
            box.setBackgroundResource(active ? R.drawable.bg_btn : R.drawable.bg_chip);
        }
        text.setTextColor(active ? Color.WHITE : getResources().getColor(R.color.textPrimary));
        text.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void updateLayoutModeIcon() {
        ImageView imgIcon = findViewById(R.id.img_layout_mode_icon);
        if (imgIcon != null) {
            int currentCols = getSharedPreferences("app_settings", MODE_PRIVATE).getInt("feed_grid_mode", 2);
            imgIcon.setImageResource(currentCols == 1 ? R.drawable.ic_layout_list : R.drawable.ic_layout_grid);
        }
    }

    private void filterEditors() {
        editorContainer.removeAllViews();
        String keyword = searchEditor.getText().toString().trim().toLowerCase();

        List<EditorItem> filtered = new ArrayList<>();

        for (EditorItem editor : editors) {
            if ("pending_hrd".equalsIgnoreCase(editor.statusStr)) continue; // HRD approval filter

            boolean categoryMatch = selectedCategory.isEmpty() || editor.hasSkill(selectedCategory);
            boolean searchMatch = keyword.isEmpty()
                    || editor.name.toLowerCase().contains(keyword)
                    || editor.service.toLowerCase().contains(keyword)
                    || editor.skillsText().toLowerCase().contains(keyword);

            if (categoryMatch && searchMatch) {
                filtered.add(editor);
            }
        }

        // Sort by interest
        Collections.sort(filtered, (e1, e2) -> {
            int score1 = calculateInterestScore(e1);
            int score2 = calculateInterestScore(e2);
            return Integer.compare(score2, score1);
        });

        if (filtered.isEmpty()) {
            if (editors.isEmpty()) {
                editorContainer.addView(createMessageState("Belum ada editor terdaftar"));
            } else {
                editorContainer.addView(createMessageState("Editor tidak ditemukan"));
            }
            return;
        }

        // Render Grid Mode (1 Column vs 2 Column) based on Setting
        int columns = getSharedPreferences("app_settings", MODE_PRIVATE).getInt("feed_grid_mode", 2);

        int totalItems = filtered.size();
        int maxItemsToShow = Math.min(currentPage * PAGE_SIZE, totalItems);

        if (columns == 1) {
            // Mode 1 Kolom (Full-Width List)
            for (int i = 0; i < maxItemsToShow; i++) {
                View card = createEditorGridCard(filtered.get(i));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, dp(14));
                card.setLayoutParams(lp);
                editorContainer.addView(card);
            }
        } else {
            // Mode 2 Kolom (Grid Side-by-Side)
            for (int i = 0; i < maxItemsToShow; i += 2) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.setMargins(0, 0, 0, dp(14));
                row.setLayoutParams(rowLp);

                // Left Card
                View leftCard = createEditorGridCard(filtered.get(i));
                LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, -2, 1);
                leftLp.setMargins(0, 0, dp(6), 0);
                leftCard.setLayoutParams(leftLp);
                row.addView(leftCard);

                // Right Card
                if (i + 1 < maxItemsToShow) {
                    View rightCard = createEditorGridCard(filtered.get(i + 1));
                    LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, -2, 1);
                    rightLp.setMargins(dp(6), 0, 0, 0);
                    rightCard.setLayoutParams(rightLp);
                    row.addView(rightCard);
                } else {
                    // Spacer for balanced width
                    View spacer = new View(this);
                    LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, -2, 1);
                    spacerLp.setMargins(dp(6), 0, 0, 0);
                    spacer.setLayoutParams(spacerLp);
                    row.addView(spacer);
                }

                editorContainer.addView(row);
            }
        }

        // Pagination "Muat Lebih Banyak" Button
        if (totalItems > maxItemsToShow) {
            TextView btnLoadMore = new TextView(this);
            btnLoadMore.setText("Muat Lebih Banyak Editor (" + (totalItems - maxItemsToShow) + " lagi)  👇");
            btnLoadMore.setTextColor(Color.parseColor("#5D63FF"));
            btnLoadMore.setTextSize(13);
            btnLoadMore.setTypeface(null, android.graphics.Typeface.BOLD);
            btnLoadMore.setGravity(Gravity.CENTER);
            btnLoadMore.setPadding(dp(16), dp(14), dp(16), dp(14));
            btnLoadMore.setBackgroundResource(R.drawable.bg_badge_skill);
            btnLoadMore.setClickable(true);
            btnLoadMore.setFocusable(true);

            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
            btnLp.setMargins(0, dp(8), 0, dp(16));
            btnLoadMore.setLayoutParams(btnLp);

            btnLoadMore.setOnClickListener(v -> {
                currentPage++;
                filterEditors();
            });

            editorContainer.addView(btnLoadMore);
        }
    }

    private int calculateInterestScore(EditorItem editor) {
        int score = 0;
        if (userInterests == null || userInterests.isEmpty()) {
            if (editor.isVerified) score += 10;
            try {
                double r = Double.parseDouble(editor.rating);
                score += (int) (r * 2);
            } catch (Exception ignored) {}
            return score;
        }

        String editorService = editor.service == null ? "" : editor.service.toLowerCase();

        for (String interest : userInterests) {
            String q = interest.toLowerCase().trim();
            if (q.isEmpty()) continue;

            if (editorService.contains(q) || q.contains(editorService)) {
                score += 15;
            }

            if (editor.skills != null) {
                for (String skill : editor.skills) {
                    String s = skill.toLowerCase();
                    if (s.contains(q) || q.contains(s)) score += 8;
                }
            }

            if ((q.contains("video") || q.contains("tiktok") || q.contains("reels") || q.contains("youtube") || q.contains("sinematik"))
                    && (editorService.contains("video") || editorService.contains("reels"))) {
                score += 12;
            }

            if ((q.contains("ui") || q.contains("ux") || q.contains("website") || q.contains("app"))
                    && (editorService.contains("ui") || editorService.contains("ux"))) {
                score += 12;
            }

            if ((q.contains("foto") || q.contains("photo") || q.contains("kamera"))
                    && (editorService.contains("foto") || editorService.contains("photo"))) {
                score += 12;
            }

            if ((q.contains("motion") || q.contains("animasi") || q.contains("grafis"))
                    && (editorService.contains("motion") || editorService.contains("animasi"))) {
                score += 12;
            }
        }

        if (editor.isVerified) score += 5;

        try {
            double r = Double.parseDouble(editor.rating);
            if (r > 0) score += (int) (r * 2);
        } catch (Exception ignored) {}

        return score;
    }

    private View createEditorGridCard(EditorItem item) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));
        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        card.addView(box);

        // 1. Top Full-Bleed Media Cover Image
        FrameLayout coverFrame = new FrameLayout(this);
        coverFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(180)));

        ImageView coverImg = new ImageView(this);
        coverImg.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String sampleCover = item.getSampleCoverUrl();
        if (sampleCover != null && !sampleCover.isEmpty()) {
            ImageLoader.load(coverImg, sampleCover);
        } else if (item.photo != null && !item.photo.isEmpty()) {
            ImageLoader.load(coverImg, item.photo);
        } else {
            coverImg.setImageResource(R.drawable.bg_home_header);
        }
        coverFrame.addView(coverImg);

        // Gradient Bottom Shadow Overlay
        View shadowOverlay = new View(this);
        FrameLayout.LayoutParams shadowLp = new FrameLayout.LayoutParams(-1, dp(60));
        shadowLp.gravity = Gravity.BOTTOM;
        shadowOverlay.setLayoutParams(shadowLp);
        shadowOverlay.setBackgroundResource(R.drawable.bg_gradient_bottom_shadow);
        coverFrame.addView(shadowOverlay);

        // Verified Checkmark
        if (item.isVerified) {
            ImageView verifiedIcon = new ImageView(this);
            verifiedIcon.setImageResource(R.drawable.ic_verified_badge);
            FrameLayout.LayoutParams vLp = new FrameLayout.LayoutParams(dp(22), dp(22));
            vLp.gravity = Gravity.TOP | Gravity.START;
            vLp.setMargins(dp(10), dp(10), 0, 0);
            coverFrame.addView(verifiedIcon, vLp);
        }

        // Rating Badge (Top Right of Image)
        boolean hasRating = !("0.0".equals(item.rating) || "0".equals(item.rating));
        TextView ratingBadge = new TextView(this);
        ratingBadge.setText(hasRating ? ("★ " + item.rating) : "★ New");
        ratingBadge.setTextSize(11);
        ratingBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        ratingBadge.setTextColor(Color.WHITE);
        ratingBadge.setBackgroundResource(R.drawable.bg_badge_rating_dark);
        ratingBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        FrameLayout.LayoutParams rLp = new FrameLayout.LayoutParams(-2, -2);
        rLp.gravity = Gravity.TOP | Gravity.END;
        rLp.setMargins(0, dp(10), dp(10), 0);
        coverFrame.addView(ratingBadge, rLp);

        box.addView(coverFrame);

        // 2. Bottom Content Info Box
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), dp(10), dp(12), dp(12));
        box.addView(info);

        // Service Title / Specialty
        TextView serviceTitle = new TextView(this);
        serviceTitle.setText(item.service);
        serviceTitle.setTextColor(getResources().getColor(R.color.textPrimary));
        serviceTitle.setTextSize(13);
        serviceTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        serviceTitle.setMaxLines(2);
        serviceTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(serviceTitle);

        // Footer Row: Avatar + Online Dot + Name + Price
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2);
        footLp.setMargins(0, dp(10), 0, 0);
        footer.setLayoutParams(footLp);

        // Small Circle Avatar with Dot Overlay
        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));

        CircleImageView avatar = new CircleImageView(this);
        avatar.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        avatar.setBackgroundResource(R.drawable.circle_soft);
        if (item.photo != null && !item.photo.isEmpty()) {
            ImageLoader.load(avatar, item.photo);
        }
        avatarFrame.addView(avatar);

        // Online Status Dot
        boolean actuallyOnline = item.isOnline && item.lastSeen > 0 && (System.currentTimeMillis() - item.lastSeen < 180000);
        View dot = new View(this);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(8), dp(8));
        dotLp.gravity = Gravity.BOTTOM | Gravity.END;
        dot.setLayoutParams(dotLp);
        dot.setBackgroundResource(actuallyOnline ? R.drawable.circle_green_dot : R.drawable.circle_gray_dot);
        avatarFrame.addView(dot);

        footer.addView(avatarFrame);

        TextView nameTv = new TextView(this);
        nameTv.setText(item.name);
        nameTv.setTextColor(getResources().getColor(R.color.textMuted));
        nameTv.setTextSize(11);
        nameTv.setMaxLines(1);
        nameTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(0, -2, 1);
        nLp.setMargins(dp(6), 0, dp(4), 0);
        nameTv.setLayoutParams(nLp);
        footer.addView(nameTv);

        TextView priceTv = new TextView(this);
        if (!item.isWorking) {
            priceTv.setText("Sedang Sibuk");
            priceTv.setTextColor(Color.parseColor("#D97706"));
        } else {
            priceTv.setText(item.price.equals("Rp0") ? "Hubungi" : item.price);
            priceTv.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        priceTv.setTextSize(11);
        priceTv.setTypeface(null, android.graphics.Typeface.BOLD);
        footer.addView(priceTv);

        info.addView(footer);

        card.setOnClickListener(v -> openEditorDetail(item));
        return card;
    }

    private void openEditorDetail(EditorItem item) {
        Intent intent = new Intent(this, DetailEditorActivity.class);
        intent.putExtra("editorId", item.id);
        startActivity(intent);
        TransitionUtil.slideRight(this);
    }

    private View createLoadingState() {
        return createMessageState("Memuat editor...");
    }

    private View createMessageState(String message) {
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextColor(Color.parseColor("#64748B"));
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(22), dp(16), dp(22));
        empty.setBackgroundResource(R.drawable.bg_empty_state);
        return empty;
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterEditors();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class EditorItem {
        String id, name, service, rating, price, icon, photo, sampleUrl, statusStr;
        List<String> skills, portfolioImages;
        boolean isOnline, isWorking, isVerified;
        long totalOrder, lastSeen;

        EditorItem(String id, String name, String service, String rating, String price, String icon, String photo, String sampleUrl, List<String> skills, List<String> portfolioImages, boolean isOnline, boolean isWorking, boolean isVerified, long totalOrder, long lastSeen, String statusStr) {
            this.id = id;
            this.name = name;
            this.service = service;
            this.rating = rating;
            this.price = price;
            this.icon = icon;
            this.photo = photo;
            this.sampleUrl = sampleUrl;
            this.skills = skills == null ? new ArrayList<>() : skills;
            this.portfolioImages = portfolioImages == null ? new ArrayList<>() : portfolioImages;
            this.isOnline = isOnline;
            this.isWorking = isWorking;
            this.isVerified = isVerified;
            this.totalOrder = totalOrder;
            this.lastSeen = lastSeen;
            this.statusStr = statusStr == null ? "" : statusStr;
        }

        String getSampleCoverUrl() {
            if (portfolioImages != null && !portfolioImages.isEmpty()) {
                for (String img : portfolioImages) {
                    if (img != null && !img.trim().isEmpty() && img.startsWith("http")) return img.trim();
                }
            }
            if (sampleUrl != null && !sampleUrl.trim().isEmpty() && sampleUrl.startsWith("http")) {
                return sampleUrl.trim();
            }
            return "";
        }

        static EditorItem fromFirestore(DocumentSnapshot doc) {
            String name = safe(doc.getString("nama"), "Editor");
            List<String> skillList = new ArrayList<>();
            Object skillObj = doc.get("skill");
            if (skillObj instanceof List) {
                for (Object obj : (List<?>) skillObj) skillList.add(String.valueOf(obj));
            }

            List<String> portImgs = new ArrayList<>();
            Object portObj = doc.get("portfolioImages");
            if (portObj == null) portObj = doc.get("sampleImages");
            if (portObj instanceof List) {
                for (Object obj : (List<?>) portObj) {
                    if (obj != null && !obj.toString().trim().isEmpty()) {
                        portImgs.add(obj.toString().trim());
                    }
                }
            }

            String sampleUrl = safe(doc.getString("sampleUrl"), safe(doc.getString("sampleLink"), ""));
            if (portImgs.isEmpty() && !sampleUrl.isEmpty() && sampleUrl.startsWith("http")) {
                portImgs.add(sampleUrl);
            }

            String service = skillList.isEmpty() ? "Editor" : toService(skillList.get(0));

            double ratingValue = 0.0;
            Number ratingNumber = doc.getDouble("rating");
            if (ratingNumber != null) ratingValue = ratingNumber.doubleValue();

            long priceValue = 0;
            Number priceNumber = doc.getLong("hargaMulai");
            if (priceNumber != null) priceValue = priceNumber.longValue();

            boolean online = false;
            Boolean onlineObj = doc.getBoolean("isOnline");
            if (onlineObj != null) online = onlineObj;

            boolean working = false;
            Boolean workingObj = doc.getBoolean("isWorking");
            if (workingObj != null) working = workingObj;

            boolean verified = false;
            Boolean verObj = doc.getBoolean("isVerified");
            if (verObj == null) verObj = doc.getBoolean("isBestEditor");
            if (verObj == null) verObj = doc.getBoolean("isTerbaik");
            if (verObj == null) verObj = doc.getBoolean("isApproved");
            if (verObj != null) verified = verObj;

            String statusStr = doc.getString("status");
            if ("approved".equalsIgnoreCase(statusStr) || "terbaik".equalsIgnoreCase(statusStr) || "best_editor".equalsIgnoreCase(statusStr)) {
                verified = true;
            }

            long orders = 0;
            Long orderObj = doc.getLong("totalOrder");
            if (orderObj != null) orders = orderObj;

            long lastSeen = 0;
            com.google.firebase.Timestamp ts = doc.getTimestamp("lastSeen");
            if (ts != null) lastSeen = ts.toDate().getTime();

            String icon = iconForSkill(skillList.isEmpty() ? service : skillList.get(0));
            String photo = safe(doc.getString("fotoProfil"), "");
            return new EditorItem(doc.getId(), name, service, String.format(java.util.Locale.US, "%.1f", ratingValue), formatPrice(priceValue), icon, photo, sampleUrl, skillList, portImgs, online, working, verified, orders, lastSeen, statusStr);
        }

        private static String safe(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
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

        private static String iconForSkill(String skill) {
            String s = skill == null ? "" : skill.toLowerCase();
            if (s.contains("video")) return "🎬";
            if (s.contains("ui") || s.contains("ux") || s.contains("design")) return "🎨";
            if (s.contains("foto") || s.contains("photo")) return "📷";
            if (s.contains("motion")) return "✨";
            return "Ⓜ";
        }

        private static String formatPrice(long value) {
            if (value <= 0) return "Rp0";
            if (value >= 1000) return "Rp" + (value / 1000) + "K";
            return "Rp" + value;
        }

        boolean hasSkill(String category) {
            for (String skill : skills) {
                if (skill.toLowerCase().contains(category.toLowerCase())) return true;
            }
            return service.toLowerCase().contains(category.toLowerCase());
        }

        String skillsText() {
            StringBuilder builder = new StringBuilder();
            for (String s : skills) builder.append(s).append(" ");
            return builder.toString();
        }
    }
}
