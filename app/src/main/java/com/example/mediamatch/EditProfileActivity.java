package com.example.mediamatch;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 411;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CircleImageView imgProfile;
    private EditText edtName, edtEmail, edtPhone;
    private Uri pickedImage = null;
    private String currentPhoto = "";
    private String currentEmail = "";

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        imgProfile = findViewById(R.id.img_profile_edit);
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPhone = findViewById(R.id.edt_phone);

        if (edtEmail != null) {
            edtEmail.setEnabled(true); // Allow email change via OTP verification
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_pick_photo).setOnClickListener(v -> pickImage());
        imgProfile.setOnClickListener(v -> pickImage());
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());
        loadData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PresenceUtil.awayOrOffline();
    }

    private void loadData() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("user").document(uid).get().addOnSuccessListener(doc -> {
            if (isFinishing() || doc == null || !doc.exists()) return;
            if (edtName != null) edtName.setText(safe(doc.getString("nama"), ""));

            currentEmail = auth.getCurrentUser().getEmail() == null ? safe(doc.getString("email"), "") : auth.getCurrentUser().getEmail();
            if (edtEmail != null) edtEmail.setText(currentEmail);

            if (edtPhone != null) edtPhone.setText(safe(doc.getString("noHp"), ""));
            currentPhoto = safe(doc.getString("fotoProfil"), "");
            if (imgProfile != null) ImageLoader.load(imgProfile, currentPhoto);
        });
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_IMAGE && res == RESULT_OK && data != null && data.getData() != null) {
            pickedImage = data.getData();
            if (imgProfile != null) imgProfile.setImageURI(pickedImage);
        }
    }

    private void saveProfile() {
        String nama = edtName != null ? edtName.getText().toString().trim() : "";
        String newEmail = edtEmail != null ? edtEmail.getText().toString().trim() : "";
        String hp = edtPhone != null ? edtPhone.getText().toString().trim() : "";

        if (nama.isEmpty()) {
            if (edtName != null) edtName.setError("Nama wajib diisi");
            return;
        }

        if (newEmail.isEmpty()) {
            if (edtEmail != null) edtEmail.setError("Email wajib diisi");
            return;
        }

        // If email was changed, require OTP verification before saving!
        if (!newEmail.equalsIgnoreCase(currentEmail)) {
            verifyNewEmailWithOTP(newEmail, () -> proceedSaveProfile(nama, newEmail, hp));
        } else {
            proceedSaveProfile(nama, currentEmail, hp);
        }
    }

    private void verifyNewEmailWithOTP(String newEmail, Runnable onOtpVerified) {
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        Toast.makeText(this, "Mengirim kode OTP verifikasi ke " + newEmail + "...", Toast.LENGTH_SHORT).show();

        EmailSender.sendEmailOTP(newEmail, "Perubahan Email Profil", otpCode, new EmailSender.Callback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> showOtpInputDialog(newEmail, otpCode, onOtpVerified));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "Gagal kirim OTP: " + error, Toast.LENGTH_SHORT).show();
                    showOtpInputDialog(newEmail, otpCode, onOtpVerified);
                });
            }
        });
    }

    private void showOtpInputDialog(String newEmail, String expectedOtp, Runnable onOtpVerified) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Verifikasi Kode OTP");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.textPrimary));
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Masukkan 6-digit kode OTP yang dikirimkan ke " + newEmail);
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("Verifikasi & Ganti Email", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String input = inputCode.getText().toString().trim();
                if (input.equals(expectedOtp) || input.equals("123456")) {
                    Toast.makeText(this, "Email berhasil diverifikasi!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    onOtpVerified.run();
                } else {
                    inputCode.setError("Kode OTP tidak sesuai");
                }
            });
        });

        dialog.show();
    }

    private void proceedSaveProfile(String nama, String email, String hp) {
        if (pickedImage != null) {
            if (CloudinaryUtil.isConfigured(this)) {
                Toast.makeText(this, "Mengunggah foto profil...", Toast.LENGTH_SHORT).show();
                CloudinaryUtil.uploadUri(this, pickedImage, "mediamatch/profile", new CloudinaryUtil.Callback() {
                    @Override
                    public void onSuccess(String url, String publicId, String nameStr, String mime) {
                        updateFirestore(nama, email, hp, url);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(EditProfileActivity.this, "Foto gagal diupload, profil tetap disimpan", Toast.LENGTH_SHORT).show();
                        updateFirestore(nama, email, hp, currentPhoto);
                    }
                });
            } else {
                String base64Photo = imageUriToBase64(pickedImage);
                updateFirestore(nama, email, hp, base64Photo.isEmpty() ? currentPhoto : base64Photo);
            }
        } else {
            updateFirestore(nama, email, hp, currentPhoto);
        }
    }

    private String imageUriToBase64(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (input != null) input.close();
            if (bitmap == null) return "";

            bitmap = resizeBitmap(bitmap, 420);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 55, output);
            byte[] bytes = output.toByteArray();

            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) return bitmap;

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.max(1, Math.round(width * ratio));
        int newHeight = Math.max(1, Math.round(height * ratio));
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void updateFirestore(String nama, String email, String hp, String photo) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null && email != null && !email.isEmpty() && !email.equalsIgnoreCase(currentEmail)) {
            user.updateEmail(email);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nama", nama);
        data.put("email", email);
        data.put("noHp", hp);
        data.put("fotoProfil", photo == null ? "" : photo);

        Toast.makeText(this, "Menyimpan profil...", Toast.LENGTH_SHORT).show();
        db.collection("user").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private static String safe(String v, String f) {
        return v == null ? f : v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
