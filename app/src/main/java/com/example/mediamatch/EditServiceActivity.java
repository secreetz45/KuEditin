package com.example.mediamatch;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditServiceActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText inputKategori, inputExperience, inputSoftwareTools, inputDeskripsi, inputSampleLink, inputCustomServices;
    private TextView btnUploadSample, btnAddPackage;
    private LinearLayout sampleImagesContainer, packageContainer;

    private static final int REQ_SAMPLE_IMAGE = 301;
    private final List<String> sampleImagesList = new ArrayList<>();
    private final List<PackageCardHolder> packageCards = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_edit_service);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        load();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        inputKategori = findViewById(R.id.input_kategori);
        inputExperience = findViewById(R.id.input_experience);
        inputSoftwareTools = findViewById(R.id.input_software_tools);
        inputDeskripsi = findViewById(R.id.input_deskripsi);
        inputSampleLink = findViewById(R.id.input_sample_link);
        inputCustomServices = findViewById(R.id.input_custom_services);

        btnUploadSample = findViewById(R.id.btn_upload_sample);
        sampleImagesContainer = findViewById(R.id.sample_images_container);

        packageContainer = findViewById(R.id.package_container);
        btnAddPackage = findViewById(R.id.btn_add_package);

        if (btnUploadSample != null) {
            btnUploadSample.setOnClickListener(v -> pickSampleImage());
        }

        if (btnAddPackage != null) {
            btnAddPackage.setOnClickListener(v -> addPackageCard("", 0, ""));
        }

        View btnSaveService = findViewById(R.id.btn_save_service);
        if (btnSaveService != null) {
            btnSaveService.setOnClickListener(v -> save());
        }
    }

    private void pickSampleImage() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, REQ_SAMPLE_IMAGE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_SAMPLE_IMAGE && res == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            if (btnUploadSample != null) {
                btnUploadSample.setText("⏳ Mengunggah foto sampel...");
                btnUploadSample.setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            CloudinaryUtil.uploadUri(this, imageUri, "mediamatch/editor_samples", new CloudinaryUtil.Callback() {
                @Override
                public void onSuccess(String url, String publicId, String name, String mime) {
                    if (url != null && !url.trim().isEmpty()) {
                        sampleImagesList.add(url.trim());
                        renderSampleThumbnails();
                        if (btnUploadSample != null) {
                            btnUploadSample.setText("📷  + Upload Foto Sampel Karya Lagi (" + sampleImagesList.size() + " Foto)");
                            btnUploadSample.setTextColor(Color.parseColor("#166534"));
                        }
                        Toast.makeText(EditServiceActivity.this, "Foto portofolio berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    if (btnUploadSample != null) {
                        btnUploadSample.setText("❌ Upload Gagal. Coba lagi");
                        btnUploadSample.setTextColor(Color.parseColor("#EF4444"));
                    }
                    Toast.makeText(EditServiceActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void renderSampleThumbnails() {
        if (sampleImagesContainer == null) return;
        sampleImagesContainer.removeAllViews();

        for (int i = 0; i < sampleImagesList.size(); i++) {
            final String url = sampleImagesList.get(i);
            final int index = i;

            MaterialCardView card = new MaterialCardView(this);
            card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));
            card.setRadius(dp(16));
            card.setCardElevation(dp(3));
            card.setStrokeColor(getResources().getColor(R.color.borderLight));
            card.setStrokeWidth(dp(1));

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(130), dp(100));
            cardLp.setMargins(0, 0, dp(10), 0);
            card.setLayoutParams(cardLp);

            FrameLayout frame = new FrameLayout(this);
            card.addView(frame);

            ImageView img = new ImageView(this);
            img.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(img, url);
            frame.addView(img);

            // Delete badge button on top-right corner of thumbnail
            TextView btnDelete = new TextView(this);
            btnDelete.setText("✕");
            btnDelete.setTextSize(12);
            btnDelete.setTextColor(Color.WHITE);
            btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
            btnDelete.setGravity(Gravity.CENTER);
            btnDelete.setBackgroundResource(R.drawable.circle_blue);

            FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(dp(26), dp(26));
            btnLp.gravity = Gravity.TOP | Gravity.END;
            btnLp.setMargins(0, dp(6), dp(6), 0);
            frame.addView(btnDelete, btnLp);

            btnDelete.setOnClickListener(v -> {
                if (index >= 0 && index < sampleImagesList.size()) {
                    sampleImagesList.remove(index);
                    renderSampleThumbnails();
                    if (btnUploadSample != null) {
                        btnUploadSample.setText(sampleImagesList.isEmpty() ?
                                "📷  + Upload Foto Sampel Karya (Bisa Banyak)" :
                                "📷  + Upload Foto Sampel Karya Lagi (" + sampleImagesList.size() + " Foto)");
                        btnUploadSample.setTextColor(getResources().getColor(R.color.colorPrimary));
                    }
                }
            });

            sampleImagesContainer.addView(card);
        }
    }

    private void load() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("user").document(uid).get().addOnSuccessListener(d -> {
            if (isFinishing() || d == null || !d.exists()) return;

            if (inputKategori != null) inputKategori.setText(safe(d.getString("kategoriJasa"), "Video Editor"));
            if (inputExperience != null) inputExperience.setText(safe(d.getString("experienceYears"), safe(d.getString("pengalaman"), "")));
            if (inputSoftwareTools != null) inputSoftwareTools.setText(safe(d.getString("softwareTools"), safe(d.getString("toolsUsed"), "")));
            if (inputDeskripsi != null) inputDeskripsi.setText(safe(d.getString("deskripsi"), ""));

            String sampleLinkStr = safe(d.getString("sampleUrl"), safe(d.getString("sampleLink"), ""));
            if (inputSampleLink != null) inputSampleLink.setText(sampleLinkStr);

            // Load multiple sample images from Firestore
            sampleImagesList.clear();
            Object samplesObj = d.get("sampleImages");
            if (samplesObj == null) samplesObj = d.get("portfolioImages");
            if (samplesObj instanceof List) {
                for (Object item : (List<?>) samplesObj) {
                    if (item != null && !item.toString().trim().isEmpty()) {
                        sampleImagesList.add(item.toString().trim());
                    }
                }
            }
            if (sampleImagesList.isEmpty() && !sampleLinkStr.isEmpty() && sampleLinkStr.startsWith("http")) {
                sampleImagesList.add(sampleLinkStr);
            }

            renderSampleThumbnails();

            if (btnUploadSample != null && !sampleImagesList.isEmpty()) {
                btnUploadSample.setText("📷  + Upload Foto Sampel Karya Lagi (" + sampleImagesList.size() + " Foto)");
            }

            if (inputCustomServices != null) inputCustomServices.setText(safe(d.getString("customServices"), ""));

            // Load dynamic packages
            packageCards.clear();
            if (packageContainer != null) packageContainer.removeAllViews();

            Object pkgsObj = d.get("packagesList");
            if (pkgsObj instanceof List && !((List<?>) pkgsObj).isEmpty()) {
                for (Object item : (List<?>) pkgsObj) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        String name = safe(String.valueOf(map.get("name")), "Paket Jasa");
                        long pVal = 0;
                        Object priceObj = map.get("price");
                        if (priceObj instanceof Number) pVal = ((Number) priceObj).longValue();
                        String desc = safe(String.valueOf(map.get("description")), "");
                        addPackageCard(name, pVal, desc);
                    }
                }
            } else {
                // Backward compatibility load from hargaBasic & hargaPremium
                Long hb = d.getLong("hargaBasic");
                String titleB = safe(d.getString("packageNameBasic"), safe(d.getString("namaPaketBasic"), "Paket Standar"));
                String descB = safe(d.getString("deskripsiBasic"), "");
                if (hb != null && hb > 0) {
                    addPackageCard(titleB, hb, descB);
                } else {
                    addPackageCard("Paket Standar", 100000, "Instruksi dasar editing & revisi fleksibel");
                }

                Long hp = d.getLong("hargaPremium");
                String titleP = safe(d.getString("packageNamePremium"), safe(d.getString("namaPaketPremium"), "Paket Premium Pro"));
                String descP = safe(d.getString("deskripsiPremium"), "");
                if (hp != null && hp > 0) {
                    addPackageCard(titleP, hp, descP);
                } else {
                    addPackageCard("Paket Premium Pro", 300000, "Color grading cinematic, sound design & prioritas");
                }
            }
        });
    }

    private void addPackageCard(String initialTitle, long initialPrice, String initialDesc) {
        if (packageContainer == null) return;

        int packageIndex = packageCards.size() + 1;

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardLp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(box);

        // Header Row (Paket # + Remove Button)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(headerRow);

        TextView titleBadge = new TextView(this);
        titleBadge.setText("Paket " + packageIndex);
        titleBadge.setTextSize(14);
        titleBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBadge.setTextColor(getResources().getColor(R.color.colorPrimary));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(0, -2, 1);
        headerRow.addView(titleBadge, badgeLp);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Hapus");
        btnDelete.setTextSize(12);
        btnDelete.setTextColor(Color.parseColor("#EF4444"));
        btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
        btnDelete.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);
        headerRow.addView(btnDelete);

        // Input Nama Paket
        TextView labelName = createLabel("Nama Paket *");
        box.addView(labelName);

        EditText edtTitle = createInput("Contoh: Paket Reels 15 Detik / Podcast Full Edit", false);
        if (!initialTitle.isEmpty()) edtTitle.setText(initialTitle);
        box.addView(edtTitle);

        // Input Harga
        TextView labelPrice = createLabel("Harga Paket (Rp) *");
        box.addView(labelPrice);

        EditText edtPrice = createInput("Contoh: 150000", true);
        if (initialPrice > 0) edtPrice.setText(String.valueOf(initialPrice));
        box.addView(edtPrice);

        // Input Deskripsi
        TextView labelDesc = createLabel("Deskripsi & Fasilitas Paket *");
        box.addView(labelDesc);

        EditText edtDesc = new EditText(this);
        edtDesc.setHint("Fasilitas lengkap yang didapat di paket ini...");
        edtDesc.setTextSize(13);
        edtDesc.setTextColor(getResources().getColor(R.color.textPrimary));
        edtDesc.setHintTextColor(getResources().getColor(R.color.textMuted));
        edtDesc.setBackgroundResource(R.drawable.bg_textarea);
        edtDesc.setPadding(dp(14), dp(12), dp(14), dp(12));
        edtDesc.setGravity(Gravity.TOP | Gravity.START);
        edtDesc.setLines(3);
        if (!initialDesc.isEmpty()) edtDesc.setText(initialDesc);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, dp(70));
        descLp.setMargins(0, dp(6), 0, 0);
        box.addView(edtDesc, descLp);

        PackageCardHolder holder = new PackageCardHolder(card, edtTitle, edtPrice, edtDesc);
        packageCards.add(holder);

        btnDelete.setOnClickListener(v -> {
            if (packageCards.size() <= 1) {
                Toast.makeText(this, "Minimal sediakan 1 paket jasa editor", Toast.LENGTH_SHORT).show();
                return;
            }
            packageContainer.removeView(card);
            packageCards.remove(holder);
            refreshPackageTitles();
        });

        packageContainer.addView(card);
    }

    private void refreshPackageTitles() {
        for (int i = 0; i < packageCards.size(); i++) {
            PackageCardHolder h = packageCards.get(i);
            View v = h.cardView.getChildAt(0);
            if (v instanceof LinearLayout) {
                LinearLayout box = (LinearLayout) v;
                View top = box.getChildAt(0);
                if (top instanceof LinearLayout) {
                    LinearLayout topRow = (LinearLayout) top;
                    View titleView = topRow.getChildAt(0);
                    if (titleView instanceof TextView) {
                        ((TextView) titleView).setText("Paket " + (i + 1));
                    }
                }
            }
        }
    }

    private TextView createLabel(String textStr) {
        TextView tv = new TextView(this);
        tv.setText(textStr);
        tv.setTextSize(13);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(getResources().getColor(R.color.textPrimary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(16), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText createInput(String hintStr, boolean isNumber) {
        EditText edt = new EditText(this);
        edt.setHint(hintStr);
        edt.setTextSize(14);
        edt.setTextColor(getResources().getColor(R.color.textPrimary));
        edt.setHintTextColor(getResources().getColor(R.color.textMuted));
        edt.setBackgroundResource(R.drawable.bg_input);
        edt.setPadding(dp(16), 0, dp(16), 0);
        edt.setSingleLine(true);
        if (isNumber) edt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.setMargins(0, dp(6), 0, 0);
        edt.setLayoutParams(lp);
        return edt;
    }

    private void save() {
        if (auth.getCurrentUser() == null) return;

        String kat = inputKategori != null ? inputKategori.getText().toString().trim() : "";
        String exp = inputExperience != null ? inputExperience.getText().toString().trim() : "";
        String tools = inputSoftwareTools != null ? inputSoftwareTools.getText().toString().trim() : "";
        String desc = inputDeskripsi != null ? inputDeskripsi.getText().toString().trim() : "";
        String sampleLinkStr = inputSampleLink != null ? inputSampleLink.getText().toString().trim() : "";
        String customS = inputCustomServices != null ? inputCustomServices.getText().toString().trim() : "";

        if (kat.isEmpty()) {
            if (inputKategori != null) inputKategori.setError("Kategori wajib diisi");
            Toast.makeText(this, "Kategori keahlian utama wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (packageCards.isEmpty()) {
            Toast.makeText(this, "Masukkan minimal 1 paket jasa editor", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> packagesList = new ArrayList<>();
        long lowestPrice = Long.MAX_VALUE;

        long basicPrice = 0, premiumPrice = 0;
        String basicTitle = "Paket Standar", premiumTitle = "Paket Premium Pro";
        String basicDesc = "", premiumDesc = "";

        for (int i = 0; i < packageCards.size(); i++) {
            PackageCardHolder h = packageCards.get(i);
            String name = h.edtTitle.getText().toString().trim();
            long priceVal = num(h.edtPrice.getText().toString());
            String pDesc = h.edtDesc.getText().toString().trim();

            if (name.isEmpty()) name = "Paket Jasa " + (i + 1);
            if (priceVal <= 0) {
                Toast.makeText(this, "Harga untuk " + name + " harus diisi valid", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> pMap = new HashMap<>();
            pMap.put("id", "pkg_" + (i + 1));
            pMap.put("name", name);
            pMap.put("price", priceVal);
            pMap.put("description", pDesc);
            packagesList.add(pMap);

            if (priceVal < lowestPrice) lowestPrice = priceVal;

            if (i == 0) {
                basicTitle = name;
                basicPrice = priceVal;
                basicDesc = pDesc;
            } else if (i == 1) {
                premiumTitle = name;
                premiumPrice = priceVal;
                premiumDesc = pDesc;
            }
        }

        if (lowestPrice == Long.MAX_VALUE) lowestPrice = 0;

        String primarySampleUrl = sampleImagesList.isEmpty() ? sampleLinkStr : sampleImagesList.get(0);

        Map<String, Object> m = new HashMap<>();
        m.put("kategoriJasa", kat);
        m.put("skill", Arrays.asList(kat));
        m.put("experienceYears", exp);
        m.put("pengalaman", exp);
        m.put("softwareTools", tools);
        m.put("toolsUsed", tools);
        m.put("deskripsi", desc);
        m.put("sampleUrl", primarySampleUrl);
        m.put("sampleLink", primarySampleUrl);

        m.put("sampleImages", sampleImagesList);
        m.put("portfolioImages", sampleImagesList);

        m.put("packagesList", packagesList);

        // Backward compatibility assignment
        m.put("packageNameBasic", basicTitle);
        m.put("namaPaketBasic", basicTitle);
        m.put("hargaBasic", basicPrice);
        m.put("deskripsiBasic", basicDesc);

        if (premiumPrice > 0) {
            m.put("packageNamePremium", premiumTitle);
            m.put("namaPaketPremium", premiumTitle);
            m.put("hargaPremium", premiumPrice);
            m.put("deskripsiPremium", premiumDesc);
        }

        m.put("customServices", customS);
        m.put("hargaMulai", lowestPrice);

        Toast.makeText(this, "Menyimpan profil & paket jasa...", Toast.LENGTH_SHORT).show();
        String uid = auth.getCurrentUser().getUid();

        db.collection("user").document(uid).set(m, SetOptions.merge()).addOnSuccessListener(x -> {
            Toast.makeText(this, "Profil & galeri portofolio berhasil disimpan", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private long num(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class PackageCardHolder {
        MaterialCardView cardView;
        EditText edtTitle, edtPrice, edtDesc;

        PackageCardHolder(MaterialCardView cardView, EditText edtTitle, EditText edtPrice, EditText edtDesc) {
            this.cardView = cardView;
            this.edtTitle = edtTitle;
            this.edtPrice = edtPrice;
            this.edtDesc = edtDesc;
        }
    }
}
