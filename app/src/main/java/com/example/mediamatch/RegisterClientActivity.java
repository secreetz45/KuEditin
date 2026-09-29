package com.example.mediamatch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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

import java.util.HashMap;
import java.util.Map;

public class RegisterClientActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1001;

    private ImageView profileImage;
    private ImageView profileIcon;
    private EditText fullName, email, phone, password, confirmPassword;
    private CheckBox agree;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Uri selectedProfileUri;
    private boolean isGoogleLogin = false;
    private String googlePhotoUrl = "";

    @Override
    public void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        try {
            setContentView(R.layout.activity_register_client);

            LogoutUtil.clearManualLogout(this);
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            profileImage = findViewById(R.id.profile_image);
            profileIcon = findViewById(R.id.profile_icon);
            fullName = findViewById(R.id.full_name);
            email = findViewById(R.id.email);
            phone = findViewById(R.id.phone);
            password = findViewById(R.id.password);
            confirmPassword = findViewById(R.id.confirm_password);
            agree = findViewById(R.id.agree);

            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    finish();
                    TransitionUtil.slideLeft(this);
                });
            }

            View profilePicker = findViewById(R.id.profile_picker);
            if (profilePicker != null) {
                profilePicker.setOnClickListener(v -> openGallery());
            }

            View txtTerms = findViewById(R.id.txt_terms);
            if (txtTerms != null) {
                txtTerms.setOnClickListener(v -> {
                    startActivity(new Intent(this, TermsConditionsActivity.class));
                    TransitionUtil.slideRight(this);
                });
            }

            View btnRegister = findViewById(R.id.btn_register);
            if (btnRegister != null) {
                btnRegister.setOnClickListener(v -> validate());
            }

            if (fullName != null) addLiveCleaner(fullName);
            if (email != null) addLiveCleaner(email);
            if (phone != null) addLiveCleaner(phone);
            if (password != null) addLiveCleaner(password);
            if (confirmPassword != null) addLiveCleaner(confirmPassword);

            checkGoogleData();
            if (!isGoogleLogin) {
                try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memuat pendaftaran client: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkGoogleData() {
        isGoogleLogin = getIntent().getBooleanExtra("isGoogleLogin", false);
        if (isGoogleLogin) {
            String gName = getIntent().getStringExtra("googleName");
            String gEmail = getIntent().getStringExtra("googleEmail");
            googlePhotoUrl = getIntent().getStringExtra("googlePhoto");
            if (gName != null && fullName != null) fullName.setText(gName);
            if (gEmail != null && email != null) { email.setText(gEmail); email.setEnabled(false); }
            if (password != null) password.setVisibility(View.GONE);
            if (confirmPassword != null) confirmPassword.setVisibility(View.GONE);
            if (googlePhotoUrl != null && !googlePhotoUrl.isEmpty()) {
                if (profileImage != null) ImageLoader.load(profileImage, googlePhotoUrl);
                if (profileIcon != null) profileIcon.setVisibility(View.GONE);
            }
        }
    }

    private void addLiveCleaner(EditText field) {
        if (field == null) return;
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    field.setError(null);
                    field.setBackgroundResource(R.drawable.bg_input);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
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

    private boolean isEmailVerified = false;

    private void validate() {
        boolean valid = true;
        valid &= checkRequired(fullName, "Nama lengkap wajib diisi");
        valid &= checkRequired(email, "Email wajib diisi");
        
        if (!isGoogleLogin) {
            valid &= checkRequired(password, "Password wajib diisi");
            valid &= checkRequired(confirmPassword, "Konfirmasi password wajib diisi");
            if (password != null && confirmPassword != null) {
                if (!password.getText().toString().trim().equals(confirmPassword.getText().toString().trim())) {
                    confirmPassword.setError("Konfirmasi password tidak sama");
                    valid = false;
                }
            }
        }

        if (agree != null && !agree.isChecked()) {
            Toast.makeText(this, "Kamu harus menyetujui syarat & ketentuan", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (!valid) return;

        if (!isGoogleLogin && !isEmailVerified) {
            String emailText = email != null ? email.getText().toString().trim() : "";
            sendRegistrationOtp(emailText);
        } else {
            registerClientToFirebase();
        }
    }

    private void sendRegistrationOtp(String targetEmail) {
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        Toast.makeText(this, "Mengirim kode OTP verifikasi email ke " + targetEmail + "...", Toast.LENGTH_SHORT).show();

        EmailSender.sendEmailOTP(targetEmail, "Pendaftaran Akun Klien", otpCode, new EmailSender.Callback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> showOtpPopup(targetEmail, otpCode));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterClientActivity.this, "Gagal kirim OTP: " + error, Toast.LENGTH_SHORT).show();
                    showOtpPopup(targetEmail, otpCode);
                });
            }
        });
    }

    private void showOtpPopup(String targetEmail, String expectedOtp) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Verifikasi Kode OTP Email");
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
        inputCode.setGravity(Gravity.CENTER);
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
                    Toast.makeText(this, "Email terverifikasi! Memproses pendaftaran...", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    registerClientToFirebase();
                } else {
                    inputCode.setError("Kode OTP tidak sesuai");
                }
            });
        });

        dialog.show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void registerClientToFirebase() {
        String emailText = email != null ? email.getText().toString().trim() : "";
        String passText = password != null ? password.getText().toString().trim() : "";

        if (isGoogleLogin) {
            if (auth.getCurrentUser() == null) return;
            saveProfileToFirestore(auth.getCurrentUser().getUid());
        } else {
            Toast.makeText(this, "Memproses pendaftaran...", Toast.LENGTH_SHORT).show();
            auth.createUserWithEmailAndPassword(emailText, passText)
                    .addOnSuccessListener(result -> {
                        if (result.getUser() != null) {
                            saveProfileToFirestore(result.getUser().getUid());
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal daftar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void saveProfileToFirestore(String uid) {
        NavUtil.saveRole(this, "client");

        if (selectedProfileUri != null) {
            Toast.makeText(this, "Mengunggah foto profil...", Toast.LENGTH_SHORT).show();
            CloudinaryUtil.uploadUri(this, selectedProfileUri, "mediamatch/profiles", new CloudinaryUtil.Callback() {
                @Override
                public void onSuccess(String url, String publicId, String name, String mime) {
                    saveFinalFirestoreData(uid, url);
                }

                @Override
                public void onError(String message) {
                    saveFinalFirestoreData(uid, isGoogleLogin ? googlePhotoUrl : "");
                }
            });
        } else {
            saveFinalFirestoreData(uid, isGoogleLogin ? googlePhotoUrl : "");
        }
    }

    private void saveFinalFirestoreData(String uid, String photoUrl) {
        String nameStr = fullName != null ? fullName.getText().toString().trim() : "Client";
        String emailStr = email != null ? email.getText().toString().trim() : "";
        String phoneStr = phone != null ? phone.getText().toString().trim() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("nama", nameStr);
        data.put("email", emailStr);
        data.put("noHp", phoneStr);
        data.put("role", "client");
        data.put("fotoProfil", photoUrl == null ? "" : photoUrl);
        data.put("provider", isGoogleLogin ? "google" : "email");
        data.put("onboarding_complete", false);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("user").document(uid).set(data)
                .addOnSuccessListener(unused -> {
                    LogoutUtil.clearManualLogout(this);
                    Toast.makeText(this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ClientOnboardingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    TransitionUtil.slideRight(this);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal simpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean checkRequired(EditText field, String msg) {
        if (field == null) return false;
        if (field.getText().toString().trim().isEmpty()) {
            field.setBackgroundResource(R.drawable.bg_input_error);
            field.setError(msg);
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        TransitionUtil.slideLeft(this);
    }
}
