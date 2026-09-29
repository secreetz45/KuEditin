package com.example.mediamatch;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RevisionDisputeActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edOrderId, edTitle, edDesc;
    private TextView chipRevisi, chipSengketa;
    private String selectedType = "Revisi";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_revision_dispute);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edOrderId = findViewById(R.id.edt_order_id);
        edTitle = findViewById(R.id.edt_dispute_title);
        edDesc = findViewById(R.id.edt_dispute_desc);
        chipRevisi = findViewById(R.id.chip_revisi);
        chipSengketa = findViewById(R.id.chip_sengketa);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        chipRevisi.setOnClickListener(v -> { selectedType = "Revisi"; updateUI(); });
        chipSengketa.setOnClickListener(v -> { selectedType = "Sengketa"; updateUI(); });

        findViewById(R.id.btn_submit_dispute).setOnClickListener(v -> submit());
        updateUI();
    }

    private void updateUI() {
        boolean isRevisi = "Revisi".equals(selectedType);
        chipRevisi.setBackgroundResource(isRevisi ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chipRevisi.setTextColor(Color.parseColor(isRevisi ? "#1A73E8" : "#64748B"));

        chipSengketa.setBackgroundResource(!isRevisi ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chipSengketa.setTextColor(Color.parseColor(!isRevisi ? "#1A73E8" : "#64748B"));
    }

    private void submit() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderRef = edOrderId.getText().toString().trim();
        String title = edTitle.getText().toString().trim();
        String desc = edDesc.getText().toString().trim();

        if (orderRef.isEmpty()) { edOrderId.setError("Wajib diisi"); return; }
        if (title.isEmpty()) { edTitle.setError("Wajib diisi"); return; }
        if (desc.isEmpty()) { edDesc.setError("Wajib diisi"); return; }

        Map<String, Object> m = new HashMap<>();
        m.put("uid", auth.getCurrentUser().getUid());
        m.put("orderRef", orderRef);
        m.put("type", selectedType);
        m.put("title", title);
        m.put("description", desc);
        m.put("status", "Menunggu Tanggapan");
        m.put("createdAt", FieldValue.serverTimestamp());

        db.collection("disputes").add(m)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, selectedType + " berhasil dikirim", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
