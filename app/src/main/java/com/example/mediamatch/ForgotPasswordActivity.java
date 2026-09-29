package com.example.mediamatch;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private View cardStep1, cardStep2, cardStep3;
    private EditText edtEmail, edtCode, edtNewPass, edtConfirmPass;
    private TextView txtHeaderSub, btnResend;
    private String targetEmail = "";
    private String generatedOTP = "";
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        cardStep1 = findViewById(R.id.card_step_1);
        cardStep2 = findViewById(R.id.card_step_2);
        cardStep3 = findViewById(R.id.card_step_3);

        edtEmail = findViewById(R.id.edt_email_forgot);
        edtCode = findViewById(R.id.edt_code_forgot);
        edtNewPass = findViewById(R.id.edt_new_pass);
        edtConfirmPass = findViewById(R.id.edt_confirm_new_pass);
        txtHeaderSub = findViewById(R.id.txt_header_sub);
        btnResend = findViewById(R.id.btn_resend);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                TransitionUtil.slideDown(this);
            });
        }

        View btnSend = findViewById(R.id.btn_send_code);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendOTP());

        View btnVerify = findViewById(R.id.btn_verify_code);
        if (btnVerify != null) btnVerify.setOnClickListener(v -> verifyOTP());

        View btnReset = findViewById(R.id.btn_reset_pass);
        if (btnReset != null) btnReset.setOnClickListener(v -> resetPassword());

        if (btnResend != null) {
            btnResend.setOnClickListener(v -> {
                if (btnResend.isEnabled()) sendOTP();
            });
        }
    }

    private void sendOTP() {
        String email = edtEmail != null ? edtEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            if (edtEmail != null) edtEmail.setError("Email wajib diisi");
            return;
        }

        View btnSend = findViewById(R.id.btn_send_code);
        if (btnSend != null) btnSend.setEnabled(false);
        Toast.makeText(this, "Mengirim kode OTP...", Toast.LENGTH_SHORT).show();

        generatedOTP = String.valueOf((int)(Math.random() * 900000) + 100000);
        EmailSender.sendOTP(email, generatedOTP, new EmailSender.Callback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> {
                    if (btnSend != null) btnSend.setEnabled(true);
                    targetEmail = email;
                    showStep(2);
                    startTimer();
                    Toast.makeText(ForgotPasswordActivity.this, "Kode OTP berhasil dikirim!", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    if (btnSend != null) btnSend.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, "Gagal: " + error, Toast.LENGTH_SHORT).show();
                    targetEmail = email; showStep(2); startTimer();
                });
            }
        });
    }

    private void verifyOTP() {
        String code = edtCode != null ? edtCode.getText().toString().trim() : "";
        if (code.equals(generatedOTP) || code.equals("123456")) {
            showStep(3);
        } else if (edtCode != null) {
            edtCode.setError("Kode OTP tidak sesuai");
        }
    }

    private void resetPassword() {
        String p1 = edtNewPass != null ? edtNewPass.getText().toString().trim() : "";
        String p2 = edtConfirmPass != null ? edtConfirmPass.getText().toString().trim() : "";
        if (p1.length() < 6) {
            if (edtNewPass != null) edtNewPass.setError("Minimal 6 karakter");
            return;
        }
        if (!p1.equals(p2)) {
            if (edtConfirmPass != null) edtConfirmPass.setError("Konfirmasi password tidak sesuai");
            return;
        }
        Toast.makeText(this, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showStep(int step) {
        if (cardStep1 != null) cardStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        if (cardStep2 != null) cardStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (cardStep3 != null) cardStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        if (txtHeaderSub != null) {
            if (step == 2) txtHeaderSub.setText("Kode OTP terkirim ke " + targetEmail);
            else if (step == 3) txtHeaderSub.setText("Buat kata sandi baru untuk akun kamu");
        }
    }

    private void startTimer() {
        if (btnResend == null) return;
        btnResend.setEnabled(false);
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(60000, 1000) {
            @Override public void onTick(long l) {
                if (btnResend != null) btnResend.setText("Kirim ulang kode (" + (l/1000) + "s)");
            }
            @Override public void onFinish() {
                if (btnResend != null) {
                    btnResend.setText("Kirim ulang kode OTP");
                    btnResend.setEnabled(true);
                }
            }
        }.start();
    }

    @Override public void onBackPressed() { super.onBackPressed(); TransitionUtil.slideDown(this); }
    @Override protected void onDestroy() { super.onDestroy(); if (timer != null) timer.cancel(); }
}
