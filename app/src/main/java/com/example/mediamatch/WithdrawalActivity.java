package com.example.mediamatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WithdrawalActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView txtBalance, txtAccountLabel;
    private TextView chipDana, chipGopay, chipOvo, chipShopeepay;
    private TextView chipBca, chipMandiri, chipBri, chipBni;
    private EditText edAmount, edAccount;

    private String selectedMethod = "DANA";

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_withdrawal);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtBalance = findViewById(R.id.txt_balance);
        txtAccountLabel = findViewById(R.id.txt_account_label);

        chipDana = findViewById(R.id.chip_dana);
        chipGopay = findViewById(R.id.chip_gopay);
        chipOvo = findViewById(R.id.chip_ovo);
        chipShopeepay = findViewById(R.id.chip_shopeepay);

        chipBca = findViewById(R.id.chip_bca);
        chipMandiri = findViewById(R.id.chip_mandiri);
        chipBri = findViewById(R.id.chip_bri);
        chipBni = findViewById(R.id.chip_bni);

        edAmount = findViewById(R.id.edt_amount);
        edAccount = findViewById(R.id.edt_account);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupChips();

        View btnSubmit = findViewById(R.id.btn_submit_withdrawal);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submit());
        }

        updateUI();
        loadBalance();
    }

    private void setupChips() {
        if (chipDana != null) chipDana.setOnClickListener(v -> selectMethod("DANA", "Nomor HP DANA *"));
        if (chipGopay != null) chipGopay.setOnClickListener(v -> selectMethod("GoPay", "Nomor HP GoPay *"));
        if (chipOvo != null) chipOvo.setOnClickListener(v -> selectMethod("OVO", "Nomor HP OVO *"));
        if (chipShopeepay != null) chipShopeepay.setOnClickListener(v -> selectMethod("ShopeePay", "Nomor HP ShopeePay *"));

        if (chipBca != null) chipBca.setOnClickListener(v -> selectMethod("Bank BCA", "Nomor Rekening BCA *"));
        if (chipMandiri != null) chipMandiri.setOnClickListener(v -> selectMethod("Bank Mandiri", "Nomor Rekening Mandiri *"));
        if (chipBri != null) chipBri.setOnClickListener(v -> selectMethod("Bank BRI", "Nomor Rekening BRI *"));
        if (chipBni != null) chipBni.setOnClickListener(v -> selectMethod("Bank BNI", "Nomor Rekening BNI *"));
    }

    private void selectMethod(String method, String labelText) {
        selectedMethod = method;
        if (txtAccountLabel != null) txtAccountLabel.setText(labelText);
        updateUI();
    }

    private void updateUI() {
        setChipStyle(chipDana, "DANA".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipGopay, "GoPay".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipOvo, "OVO".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipShopeepay, "ShopeePay".equalsIgnoreCase(selectedMethod));

        setChipStyle(chipBca, "Bank BCA".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipMandiri, "Bank Mandiri".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipBri, "Bank BRI".equalsIgnoreCase(selectedMethod));
        setChipStyle(chipBni, "Bank BNI".equalsIgnoreCase(selectedMethod));
    }

    private void setChipStyle(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setBackgroundResource(active ? R.drawable.bg_btn : R.drawable.bg_chip);
        chip.setTextColor(active ? Color.WHITE : getResources().getColor(R.color.textMuted));
        chip.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadBalance() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("user").document(uid).get().addOnSuccessListener(doc -> {
            if (isFinishing() || doc == null || !doc.exists()) return;
            Long balanceVal = doc.getLong("saldo");
            if (balanceVal == null) balanceVal = doc.getLong("balance");
            long bal = balanceVal == null ? 0 : balanceVal;

            if (txtBalance != null) {
                txtBalance.setText(rupiah(bal));
            }
        });
    }

    private void submit() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String amount = edAmount != null ? edAmount.getText().toString().trim() : "";
        String account = edAccount != null ? edAccount.getText().toString().trim() : "";

        if (amount.isEmpty()) {
            if (edAmount != null) edAmount.setError("Nominal penarikan wajib diisi");
            Toast.makeText(this, "Masukkan nominal penarikan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (account.isEmpty()) {
            if (edAccount != null) edAccount.setError("Nomor tujuan wajib diisi");
            Toast.makeText(this, "Masukkan nomor HP E-Wallet atau rekening bank tujuan", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("uid", auth.getCurrentUser().getUid());
        m.put("method", selectedMethod);
        m.put("amount", amount);
        m.put("account", account);
        m.put("status", "Menunggu Proses");
        m.put("createdAt", FieldValue.serverTimestamp());

        Toast.makeText(this, "Mengirim pengajuan penarikan...", Toast.LENGTH_SHORT).show();

        db.collection("withdrawals").add(m)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Pengajuan penarikan ke " + selectedMethod + " berhasil dikirim!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    private String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("in", "ID")).format(v).replace(",00", "");
    }
}
