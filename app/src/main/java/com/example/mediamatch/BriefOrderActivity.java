package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class BriefOrderActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText inputTitle, inputBrief, inputCloudLink;
    private TextView txtCounter, btnUploadFile;
    private TextView txtSummaryEditor, txtSummaryPackage, txtSummaryPrice;
    private TextView optRatio916, optRatio169, optRatio11, optRatioCustom;
    private EditText inputCustomRatio;

    private static final int REQ_FILE = 201;
    private Uri fileUri;
    private String fileName = "";
    private String fileBase64 = "";
    private String fileMime = "";
    private String fileUrl = "";
    private String selectedRatio = "9:16 Vertical (Reels/TikTok)";

    private String editorId, editorName, service, packageName, packageType;
    private long price;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_brief_order);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editorId = getIntent().getStringExtra("editorId");
        editorName = getIntent().getStringExtra("editorName");
        service = getIntent().getStringExtra("service");
        packageName = getIntent().getStringExtra("packageName");
        packageType = getIntent().getStringExtra("packageType");
        price = getIntent().getLongExtra("price", 0);

        bindViews();
        updateSummary();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        inputTitle = findViewById(R.id.input_title);
        inputBrief = findViewById(R.id.input_brief);
        inputCloudLink = findViewById(R.id.input_cloud_link);
        txtCounter = findViewById(R.id.txt_counter);

        btnUploadFile = findViewById(R.id.btn_upload_file);

        optRatio916 = findViewById(R.id.opt_ratio_916);
        optRatio169 = findViewById(R.id.opt_ratio_169);
        optRatio11 = findViewById(R.id.opt_ratio_11);
        optRatioCustom = findViewById(R.id.opt_ratio_custom);
        inputCustomRatio = findViewById(R.id.input_custom_ratio);

        txtSummaryEditor = findViewById(R.id.txt_summary_editor);
        txtSummaryPackage = findViewById(R.id.txt_summary_package);
        txtSummaryPrice = findViewById(R.id.txt_summary_price);

        if (inputBrief != null) {
            inputBrief.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (txtCounter != null) {
                        txtCounter.setText(Math.min(s.length(), 500) + "/500");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        setupRatioOptions();

        if (btnUploadFile != null) {
            btnUploadFile.setOnClickListener(v -> pickFile());
        }

        TextView btnSubmitBrief = findViewById(R.id.btn_submit_brief);
        if (btnSubmitBrief != null) {
            btnSubmitBrief.setOnClickListener(v -> submit());
        }
    }

    private void setupRatioOptions() {
        if (optRatio916 != null) optRatio916.setOnClickListener(v -> selectStandardRatio("9:16 Vertical (Reels/TikTok)", optRatio916));
        if (optRatio169 != null) optRatio169.setOnClickListener(v -> selectStandardRatio("16:9 Landscape (YouTube)", optRatio169));
        if (optRatio11 != null) optRatio11.setOnClickListener(v -> selectStandardRatio("1:1 Square (Feed Post)", optRatio11));
        if (optRatioCustom != null) optRatioCustom.setOnClickListener(v -> selectCustomRatio());
    }

    private void selectStandardRatio(String ratioText, TextView selected) {
        selectedRatio = ratioText;
        if (inputCustomRatio != null) inputCustomRatio.setVisibility(View.GONE);

        resetRatioChips();
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(Color.parseColor("#5D63FF"));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void selectCustomRatio() {
        selectedRatio = "custom";
        if (inputCustomRatio != null) {
            inputCustomRatio.setVisibility(View.VISIBLE);
            inputCustomRatio.requestFocus();
        }

        resetRatioChips();
        if (optRatioCustom != null) {
            optRatioCustom.setBackgroundResource(R.drawable.bg_chip_selected);
            optRatioCustom.setTextColor(Color.parseColor("#5D63FF"));
            optRatioCustom.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void resetRatioChips() {
        TextView[] chips = {optRatio916, optRatio169, optRatio11, optRatioCustom};
        for (TextView chip : chips) {
            if (chip != null) {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(Color.parseColor("#475569"));
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_FILE);
    }

    private void updateSummary() {
        if (txtSummaryEditor != null) txtSummaryEditor.setText(safe(editorName, "Editor"));
        if (txtSummaryPackage != null) txtSummaryPackage.setText(safe(packageName, safe(service, "Paket Jasa")));
        if (txtSummaryPrice != null) txtSummaryPrice.setText(rupiah(price));
    }

    private void submit() {
        if (inputTitle == null || inputBrief == null) return;

        String t = inputTitle.getText().toString().trim();
        String br = inputBrief.getText().toString().trim();
        String cloudLink = inputCloudLink != null ? inputCloudLink.getText().toString().trim() : "";

        inputTitle.setBackgroundResource(R.drawable.bg_input);
        inputBrief.setBackgroundResource(R.drawable.bg_textarea);

        if (t.isEmpty()) {
            inputTitle.setBackgroundResource(R.drawable.bg_input_error);
            Toast.makeText(this, "Judul project wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (br.isEmpty()) {
            inputBrief.setBackgroundResource(R.drawable.bg_textarea_error);
            Toast.makeText(this, "Brief instruksi wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail() == null ? "" : auth.getCurrentUser().getEmail();

        String finalRatio = selectedRatio;
        if ("custom".equals(selectedRatio)) {
            String customVal = inputCustomRatio != null ? inputCustomRatio.getText().toString().trim() : "";
            if (customVal.isEmpty()) {
                if (inputCustomRatio != null) inputCustomRatio.setError("Tulis rasio kustom kamu");
                Toast.makeText(this, "Tulis rasio atau format kustom kamu", Toast.LENGTH_SHORT).show();
                return;
            }
            finalRatio = "Custom (" + customVal + ")";
        }

        // Include format ratio and cloud link in brief notes
        String fullBrief = br + "\n\nFormat/Rasio: " + finalRatio;
        if (!cloudLink.isEmpty()) {
            fullBrief += "\nLink Bahan Cloud: " + cloudLink;
        }

        final String finalFullBrief = fullBrief;

        db.collection("user").document(uid).get().addOnSuccessListener(me -> {
            String clientName = safe(me.getString("nama"), email);
            String clientPhoto = safe(me.getString("fotoProfil"), "");

            db.collection("user").document(editorId).get().addOnSuccessListener(ed -> {
                String editorPhoto = safe(ed.getString("fotoProfil"), "");

                Intent pay = new Intent(this, PaymentActivity.class);
                pay.putExtra("clientName", clientName);
                pay.putExtra("clientEmail", email);
                pay.putExtra("clientPhoto", clientPhoto);
                pay.putExtra("editorId", editorId);
                pay.putExtra("editorName", safe(editorName, safe(ed.getString("nama"), "Editor")));
                pay.putExtra("editorPhoto", editorPhoto);
                pay.putExtra("service", service);
                pay.putExtra("packageName", packageName);
                pay.putExtra("packageType", packageType);
                pay.putExtra("price", price);
                pay.putExtra("dp", price); // Direct full payment
                pay.putExtra("judul", t);
                pay.putExtra("brief", finalFullBrief);
                pay.putExtra("cloudLink", cloudLink);
                pay.putExtra("fileName", fileName);
                pay.putExtra("fileMime", fileMime);
                pay.putExtra("fileUrl", fileUrl);
                pay.putExtra("fileLocalUri", fileUri == null ? "" : fileUri.toString());
                startActivity(pay);
            }).addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat editor: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat profil: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}

            fileName = getFileName(fileUri);
            fileMime = getContentResolver().getType(fileUri);
            if (fileMime == null) fileMime = "application/octet-stream";

            fileUrl = "";
            fileBase64 = "";

            if (btnUploadFile != null) {
                btnUploadFile.setText("⏳ Memproses file...\n" + fileName);
                btnUploadFile.setTextColor(Color.parseColor("#5D63FF"));
            }

            CloudinaryUtil.uploadUri(this, fileUri, "mediamatch/order_bahan", new CloudinaryUtil.Callback() {
                @Override
                public void onSuccess(String url, String publicId, String name, String mime) {
                    fileUrl = url;
                    fileName = name;
                    fileMime = mime;
                    if (btnUploadFile != null) {
                        btnUploadFile.setText("✓ File berhasil diunggah\n" + fileName);
                        btnUploadFile.setTextColor(Color.parseColor("#166534"));
                    }
                }

                @Override
                public void onError(String message) {
                    fileUrl = "";
                    if (btnUploadFile != null) {
                        btnUploadFile.setText("❌ Upload gagal\n" + message);
                        btnUploadFile.setTextColor(Color.parseColor("#EF4444"));
                    }
                    Toast.makeText(BriefOrderActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "File dipilih";
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }

    private String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
}
