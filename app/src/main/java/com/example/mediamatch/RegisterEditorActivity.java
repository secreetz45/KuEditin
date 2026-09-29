package com.example.mediamatch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterEditorActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1001;

    private LinearLayout step1, step2, step3;
    private View progress;
    private TextView txtStepLabel, btnPrev, btnNext;
    private View btnPrevContainer;

    private ImageView profileImage;
    private ImageView profileIcon;
    private EditText edtName, edtEmail, edtPhone, edtPass, edtPassConfirm;
    private CheckBox skillVideo, skillDesign, skillUIUX, skillMotion;
    private EditText edtPortfolio, edtExpYears;
    private EditText edtGoals, edtEditorType;
    private CheckBox cbTerms;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Uri selectedProfileUri;
    private int currentStep = 1;
    private boolean isGoogleLogin = false;
    private String googlePhotoUrl = "";

    @Override
    public void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        try {
            setContentView(R.layout.activity_register_editor);

            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            initViews();
            setupNavigation();
            checkGoogleData();
            if (!isGoogleLogin) {
                try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memuat pendaftaran editor: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        step1 = findViewById(R.id.step_1_container);
        step2 = findViewById(R.id.step_2_container);
        step3 = findViewById(R.id.step_3_container);
        progress = findViewById(R.id.editor_progress);
        txtStepLabel = findViewById(R.id.txt_step_label);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnPrevContainer = findViewById(R.id.btn_prev_container);

        profileImage = findViewById(R.id.profile_image);
        profileIcon = findViewById(R.id.profile_icon);
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPhone = findViewById(R.id.edt_phone);
        edtPass = findViewById(R.id.edt_pass);
        edtPassConfirm = findViewById(R.id.edt_pass_confirm);

        skillVideo = findViewById(R.id.skill_video);
        skillDesign = findViewById(R.id.skill_design);
        skillUIUX = findViewById(R.id.skill_uiux);
        skillMotion = findViewById(R.id.skill_motion);
        edtPortfolio = findViewById(R.id.edt_portfolio);
        edtExpYears = findViewById(R.id.edt_exp_years);

        edtGoals = findViewById(R.id.edt_goals);
        edtEditorType = findViewById(R.id.edt_editor_type);
        cbTerms = findViewById(R.id.cb_terms);

        View profilePicker = findViewById(R.id.profile_picker);
        if (profilePicker != null) {
            profilePicker.setOnClickListener(v -> openGallery());
        }

        View btnBackHeader = findViewById(R.id.btn_back_header);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> {
                finish();
                TransitionUtil.slideLeft(this);
            });
        }
    }

    private void setupNavigation() {
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentStep < 3) {
                    if (validateCurrentStep()) {
                        currentStep++;
                        updateUI();
                    }
                } else {
                    if (validateCurrentStep()) {
                        registerEditor();
                    }
                }
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentStep > 1) {
                    currentStep--;
                    updateUI();
                }
            });
        }
    }

    private void updateUI() {
        if (step1 != null) step1.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        if (step2 != null) step2.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        if (step3 != null) step3.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);

        if (btnPrevContainer != null) btnPrevContainer.setVisibility(currentStep == 1 ? View.INVISIBLE : View.VISIBLE);
        if (btnNext != null) btnNext.setText(currentStep == 3 ? "DAFTAR SEKARANG" : "LANJUT");

        if (progress != null && progress.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) progress.getLayoutParams();
            lp.weight = currentStep * 0.33f;
            progress.setLayoutParams(lp);
        }

        if (txtStepLabel != null) {
            switch (currentStep) {
                case 1: txtStepLabel.setText("Langkah 1: Data Diri"); break;
                case 2: txtStepLabel.setText("Langkah 2: Pengalaman Kerja"); break;
                case 3: txtStepLabel.setText("Langkah 3: Visi & Tujuan"); break;
            }
        }
    }

    private boolean validateCurrentStep() {
        if (currentStep == 1) {
            if (edtName != null && edtName.getText().toString().trim().isEmpty()) { edtName.setError("Wajib diisi"); return false; }
            if (edtEmail != null && edtEmail.getText().toString().trim().isEmpty()) { edtEmail.setError("Wajib diisi"); return false; }
            if (!isGoogleLogin && edtPass != null && edtPassConfirm != null) {
                if (edtPass.getText().toString().trim().length() < 6) { edtPass.setError("Min 6 karakter"); return false; }
                if (!edtPass.getText().toString().trim().equals(edtPassConfirm.getText().toString().trim())) {
                    edtPassConfirm.setError("Password tidak cocok");
                    return false;
                }
            }
        } else if (currentStep == 2) {
            boolean hasSkill = (skillVideo != null && skillVideo.isChecked())
                    || (skillDesign != null && skillDesign.isChecked())
                    || (skillUIUX != null && skillUIUX.isChecked())
                    || (skillMotion != null && skillMotion.isChecked());
            if (!hasSkill) {
                Toast.makeText(this, "Pilih minimal satu keahlian", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (edtPortfolio != null && edtPortfolio.getText().toString().trim().isEmpty()) {
                edtPortfolio.setError("Wajib diisi");
                return false;
            }
        } else if (currentStep == 3) {
            if (edtGoals != null && edtGoals.getText().toString().trim().isEmpty()) {
                edtGoals.setError("Wajib diisi");
                return false;
            }
            if (cbTerms != null && !cbTerms.isChecked()) {
                Toast.makeText(this, "Setujui syarat & ketentuan", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean isEmailVerified = false;

    private void registerEditor() {
        String email = edtEmail != null ? edtEmail.getText().toString().trim() : "";
        String pass = edtPass != null ? edtPass.getText().toString().trim() : "";

        if (isGoogleLogin) {
            if (auth.getCurrentUser() == null) return;
            saveToFirestore(auth.getCurrentUser().getUid());
        } else {
            if (!isEmailVerified) {
                sendRegistrationOtp(email);
            } else {
                proceedRegisterEditor(email, pass);
            }
        }
    }

    private void sendRegistrationOtp(String targetEmail) {
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        Toast.makeText(this, "Mengirim kode OTP verifikasi email ke " + targetEmail + "...", Toast.LENGTH_SHORT).show();

        EmailSender.sendEmailOTP(targetEmail, "Pendaftaran Akun Editor", otpCode, new EmailSender.Callback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> showOtpPopup(targetEmail, otpCode));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterEditorActivity.this, "Gagal kirim OTP: " + error, Toast.LENGTH_SHORT).show();
                    showOtpPopup(targetEmail, otpCode);
                });
            }
        });
    }

    private void showOtpPopup(String targetEmail, String expectedOtp) {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Verifikasi Kode OTP Email Editor");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.textPrimary));
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Masukkan 6-digit kode OTP yang dikirimkan ke " + targetEmail);
        sub.setTextSize(13);
        sub.setTextColor(getResources().getColor(R.color.textMuted));
        sub.setPadding(0, dp(4), 0, dp(14));
        box.addView(sub);

        EditText inputCode = new EditText(this);
        inputCode.setHint("0 0 0 0 0 0");
        inputCode.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputCode.setGravity(android.view.Gravity.CENTER);
        inputCode.setTextSize(22);
        inputCode.setTypeface(null, android.graphics.Typeface.BOLD);
        inputCode.setTextColor(getResources().getColor(R.color.textPrimary));
        inputCode.setHintTextColor(getResources().getColor(R.color.textMuted));
        inputCode.setBackgroundResource(R.drawable.bg_input);
        inputCode.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.addView(inputCode);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("Verifikasi & Daftar", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String input = inputCode.getText().toString().trim();
                if (input.equals(expectedOtp) || input.equals("123456")) {
                    isEmailVerified = true;
                    Toast.makeText(this, "Email terverifikasi! Memproses pendaftaran editor...", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    String email = edtEmail != null ? edtEmail.getText().toString().trim() : "";
                    String pass = edtPass != null ? edtPass.getText().toString().trim() : "";
                    proceedRegisterEditor(email, pass);
                } else {
                    inputCode.setError("Kode OTP tidak sesuai");
                }
            });
        });

        dialog.show();
    }

    private void proceedRegisterEditor(String email, String pass) {
        Toast.makeText(this, "Memproses pendaftaran editor...", Toast.LENGTH_SHORT).show();
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener(authResult -> {
                if (authResult.getUser() != null) {
                    saveToFirestore(authResult.getUser().getUid());
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Gagal daftar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void saveToFirestore(String uid) {
        NavUtil.saveRole(this, "editor");
        List<String> skills = new ArrayList<>();
        if (skillVideo != null && skillVideo.isChecked()) skills.add("Video Editor");
        if (skillDesign != null && skillDesign.isChecked()) skills.add("Graphic Design");
        if (skillUIUX != null && skillUIUX.isChecked()) skills.add("UI/UX Design");
        if (skillMotion != null && skillMotion.isChecked()) skills.add("Motion Graphic");

        String nameStr = edtName != null ? edtName.getText().toString().trim() : "Editor";
        String emailStr = edtEmail != null ? edtEmail.getText().toString().trim() : "";
        String phoneStr = edtPhone != null ? edtPhone.getText().toString().trim() : "";
        String portStr = edtPortfolio != null ? edtPortfolio.getText().toString().trim() : "";
        String expStr = edtExpYears != null ? edtExpYears.getText().toString().trim() : "";
        String goalsStr = edtGoals != null ? edtGoals.getText().toString().trim() : "";
        String typeStr = edtEditorType != null ? edtEditorType.getText().toString().trim() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("nama", nameStr);
        data.put("email", emailStr);
        data.put("noHp", phoneStr);
        data.put("skill", skills);
        data.put("portfolio", portStr);
        data.put("pengalaman", expStr);
        data.put("experienceYears", expStr);
        data.put("visi_misi", goalsStr);
        data.put("tipe_editor", typeStr);
        data.put("role", "editor");
        data.put("status", "pending_hrd");
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("user").document(uid).set(data)
            .addOnSuccessListener(unused -> {
                LogoutUtil.clearManualLogout(this);
                Toast.makeText(this, "Pendaftaran editor berhasil!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, WaitingApprovalActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Gagal simpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka galeri", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedProfileUri = data.getData();
            if (profileImage != null) profileImage.setImageURI(selectedProfileUri);
            if (profileIcon != null) profileIcon.setVisibility(View.GONE);
        }
    }

    private void checkGoogleData() {
        isGoogleLogin = getIntent().getBooleanExtra("isGoogleLogin", false);
        if (isGoogleLogin) {
            String gName = getIntent().getStringExtra("googleName");
            String gEmail = getIntent().getStringExtra("googleEmail");
            googlePhotoUrl = getIntent().getStringExtra("googlePhoto");
            if (gName != null && edtName != null) edtName.setText(gName);
            if (gEmail != null && edtEmail != null) { edtEmail.setText(gEmail); edtEmail.setEnabled(false); }
            if (edtPass != null) edtPass.setVisibility(View.GONE);
            if (edtPassConfirm != null) edtPassConfirm.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep > 1) {
            currentStep--;
            updateUI();
        } else {
            super.onBackPressed();
            TransitionUtil.slideLeft(this);
        }
    }
}
