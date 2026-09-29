package com.example.mediamatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private EditText edtOld, edtNew, edtConfirm;

    @Override protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        edtOld = findViewById(R.id.edt_old_password);
        edtNew = findViewById(R.id.edt_new_password);
        edtConfirm = findViewById(R.id.edt_confirm_password);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_password).setOnClickListener(v -> changePassword());

        checkGoogleUser();
    }

    private void checkGoogleUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        boolean isGoogle = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                isGoogle = true;
                break;
            }
        }

        if (isGoogle) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Login Akun Google")
                    .setMessage("Akun Anda terhubung menggunakan Google Sign-In.\n\nKata sandi dikelola secara otomatis & aman langsung melalui Akun Google Anda, sehingga Anda tidak perlu membuat atau mengubah password di KuEditin.")
                    .setPositiveButton("Mengerti", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    private void changePassword() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Akun tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldPass = edtOld.getText().toString().trim();
        String newPass = edtNew.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        if (oldPass.isEmpty()) {
            edtOld.setError("Password lama wajib diisi");
            return;
        }
        if (newPass.length() < 6) {
            edtNew.setError("Password baru minimal 6 karakter");
            return;
        }
        if (!newPass.equals(confirm)) {
            edtConfirm.setError("Konfirmasi password tidak sama");
            return;
        }
        if (oldPass.equals(newPass)) {
            edtNew.setError("Password baru tidak boleh sama dengan password lama");
            return;
        }

        findViewById(R.id.btn_save_password).setEnabled(false);
        Toast.makeText(this, "Mengecek password lama...", Toast.LENGTH_SHORT).show();

        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldPass))
                .addOnSuccessListener(unused -> user.updatePassword(newPass)
                        .addOnSuccessListener(ok -> {
                            Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            findViewById(R.id.btn_save_password).setEnabled(true);
                            Toast.makeText(this, "Gagal mengubah password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    findViewById(R.id.btn_save_password).setEnabled(true);
                    edtOld.setError("Password lama salah");
                    Toast.makeText(this, "Password lama salah atau login sudah kedaluwarsa", Toast.LENGTH_LONG).show();
                });
    }
}
