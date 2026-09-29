package com.example.mediamatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailServiceActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String editorId, editorName = "Editor", category = "Jasa Kreatif", photoUrl = "";
    private boolean isWorking = true;

    private TextView txtEditorName, txtCategory, txtRating, btnSubmitOrder;
    private CircleImageView avatarImg;
    private LinearLayout containerAllPackages;
    private View emptyPackageCard, submitContainer;

    private final List<PackageItem> packageItems = new ArrayList<>();
    private int selectedPackageIndex = 0;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_detail_service);

        db = FirebaseFirestore.getInstance();
        editorId = getIntent().getStringExtra("editorId");
        if (editorId == null || editorId.trim().isEmpty()) {
            Toast.makeText(this, "Editor tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        loadEditor();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        avatarImg = findViewById(R.id.img_service_editor_avatar);
        txtEditorName = findViewById(R.id.txt_service_editor_name);
        txtCategory = findViewById(R.id.txt_service_category);
        txtRating = findViewById(R.id.txt_service_rating);

        containerAllPackages = findViewById(R.id.container_all_packages);
        emptyPackageCard = findViewById(R.id.card_empty_package);
        submitContainer = findViewById(R.id.btn_submit_container);
        btnSubmitOrder = findViewById(R.id.btn_submit_order);

        if (btnSubmitOrder != null) {
            btnSubmitOrder.setOnClickListener(v -> openBrief());
        }
    }

    private void loadEditor() {
        db.collection("user").document(editorId).get().addOnSuccessListener(doc -> {
            if (isFinishing() || !doc.exists()) return;

            editorName = safe(doc.getString("nama"), "Editor");
            category = detectCategory(doc);
            photoUrl = safe(doc.getString("fotoProfil"), "");

            if (avatarImg != null && !photoUrl.isEmpty()) {
                ImageLoader.load(avatarImg, photoUrl);
            }

            if (txtEditorName != null) txtEditorName.setText(editorName);
            if (txtCategory != null) txtCategory.setText(category);

            Double r = doc.getDouble("rating");
            Long rev = doc.getLong("totalReview");
            double ratingNum = r == null ? 0.0 : Math.min(5.0, Math.max(0.0, r));
            long reviewCount = rev == null ? 0 : rev;

            Boolean workingObj = doc.getBoolean("isWorking");
            if (workingObj != null) isWorking = workingObj;

            if (txtRating != null) {
                if (!isWorking) {
                    txtRating.setText("🔴 Editor sedang sibuk / off order");
                    txtRating.setTextColor(getResources().getColor(R.color.error));
                } else if (ratingNum > 0) {
                    txtRating.setText(String.format(Locale.US, "★ %.1f • %d ulasan", ratingNum, reviewCount));
                    txtRating.setTextColor(getResources().getColor(R.color.textPrimary));
                } else {
                    txtRating.setText("★ 0.0 • Belum ada ulasan");
                    txtRating.setTextColor(getResources().getColor(R.color.textMuted));
                }
            }

            // Load dynamic packages from Firestore
            packageItems.clear();
            Object pkgsObj = doc.get("packagesList");
            if (pkgsObj instanceof List && !((List<?>) pkgsObj).isEmpty()) {
                for (Object item : (List<?>) pkgsObj) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        String name = safe(String.valueOf(map.get("name")), "Paket Jasa");
                        long pVal = 0;
                        Object priceObj = map.get("price");
                        if (priceObj instanceof Number) pVal = ((Number) priceObj).longValue();
                        String desc = safe(String.valueOf(map.get("description")), "Instruksi pengerjaan & revisi fleksibel");
                        if (pVal > 0) {
                            packageItems.add(new PackageItem(name, pVal, desc));
                        }
                    }
                }
            }

            // Fallback load from hargaBasic & hargaPremium
            if (packageItems.isEmpty()) {
                Long bp = doc.getLong("hargaBasic");
                String bd = safe(doc.getString("deskripsiBasic"), "Instruksi dasar editing & revisi fleksibel");
                String bt = safe(doc.getString("packageNameBasic"), safe(doc.getString("namaPaketBasic"), category + " Standar"));
                if (bp != null && bp > 0) {
                    packageItems.add(new PackageItem(bt, bp, bd));
                }

                Long pp = doc.getLong("hargaPremium");
                String pd = safe(doc.getString("deskripsiPremium"), "Color grading, sound design & prioritas");
                String pt = safe(doc.getString("packageNamePremium"), safe(doc.getString("namaPaketPremium"), category + " Premium Pro"));
                if (pp != null && pp > 0) {
                    packageItems.add(new PackageItem(pt, pp, pd));
                }
            }

            // Fallback to hargaMulai
            if (packageItems.isEmpty()) {
                Long hm = doc.getLong("hargaMulai");
                if (hm != null && hm > 0) {
                    packageItems.add(new PackageItem(category + " Standar", hm, "Estimasi pengerjaan & revisi fleksibel"));
                }
            }

            renderPackageCards();
        });
    }

    private void renderPackageCards() {
        if (containerAllPackages == null) return;
        containerAllPackages.removeAllViews();

        boolean hasPackages = !packageItems.isEmpty();

        if (emptyPackageCard != null) {
            emptyPackageCard.setVisibility(hasPackages ? View.GONE : View.VISIBLE);
        }

        if (submitContainer != null) {
            submitContainer.setVisibility(hasPackages ? View.VISIBLE : View.GONE);
        }

        if (!hasPackages) return;

        if (selectedPackageIndex >= packageItems.size()) {
            selectedPackageIndex = 0;
        }

        for (int i = 0; i < packageItems.size(); i++) {
            final int index = i;
            PackageItem pkg = packageItems.get(i);
            boolean isSelected = (i == selectedPackageIndex);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(20));
            card.setCardElevation(dp(isSelected ? 4 : 2));
            card.setStrokeColor(isSelected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.borderLight));
            card.setStrokeWidth(dp(isSelected ? 2 : 1));
            card.setCardBackgroundColor(isSelected ? getResources().getColor(R.color.bg_soft_blue) : getResources().getColor(R.color.cardSurface));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardLp);

            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(20), dp(18), dp(20), dp(18));
            card.addView(box);

            // Title & Price Header Row
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            box.addView(titleRow);

            TextView title = new TextView(this);
            title.setText(pkg.name);
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(isSelected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textPrimary));
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, -2, 1);
            titleRow.addView(title, tLp);

            TextView price = new TextView(this);
            price.setText(rupiah(pkg.price));
            price.setTextSize(17);
            price.setTypeface(null, android.graphics.Typeface.BOLD);
            price.setTextColor(isSelected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textPrimary));
            titleRow.addView(price);

            // Subtitle Description Row
            String descText = pkg.description;
            if (descText == null || descText.trim().isEmpty()) {
                descText = "Termasuk instruksi pengerjaan & revisi fleksibel";
            }

            TextView desc = new TextView(this);
            desc.setText(descText);
            desc.setTextSize(13);
            desc.setTextColor(getResources().getColor(R.color.textMuted));
            desc.setPadding(0, dp(6), 0, 0);
            box.addView(desc);

            card.setOnClickListener(v -> {
                selectedPackageIndex = index;
                renderPackageCards();
            });

            containerAllPackages.addView(card);
        }

        // Update submit button text with selected package name & price
        PackageItem activePkg = packageItems.get(selectedPackageIndex);
        if (btnSubmitOrder != null) {
            if (!isWorking) {
                btnSubmitOrder.setText("Editor Sedang Sibuk / Off Order");
                btnSubmitOrder.setTextColor(getResources().getColor(R.color.textMuted));
            } else {
                btnSubmitOrder.setText("Pesan " + activePkg.name + " (" + rupiah(activePkg.price) + ")");
                btnSubmitOrder.setTextColor(Color.WHITE);
            }
        }
    }

    private String detectCategory(DocumentSnapshot doc) {
        Object skills = doc.get("skill");
        if (skills instanceof List && !((List<?>) skills).isEmpty()) {
            return String.valueOf(((List<?>) skills).get(0));
        }
        return safe(doc.getString("kategoriJasa"), "Jasa Kreatif");
    }

    private void openBrief() {
        if (!isWorking) {
            Toast.makeText(this, "Editor ini sedang sibuk / tidak menerima order saat ini.", Toast.LENGTH_LONG).show();
            return;
        }

        if (packageItems.isEmpty()) {
            Toast.makeText(this, "Editor belum menyediakan paket jasa", Toast.LENGTH_SHORT).show();
            return;
        }

        PackageItem activePkg = packageItems.get(selectedPackageIndex);

        Intent i = new Intent(this, BriefOrderActivity.class);
        i.putExtra("editorId", editorId);
        i.putExtra("editorName", editorName);
        i.putExtra("service", category);
        i.putExtra("packageName", activePkg.name);
        i.putExtra("packageType", "custom_pkg_" + (selectedPackageIndex + 1));
        i.putExtra("price", activePkg.price);
        startActivity(i);
        TransitionUtil.slideRight(this);
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class PackageItem {
        String name, description;
        long price;

        PackageItem(String name, long price, String description) {
            this.name = name;
            this.price = price;
            this.description = description;
        }
    }
}
