package com.example.mediamatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CircleImageView imgProfile;
    private TextView txtName, txtRole, txtOrder, txtProgress, txtRating;
    private String role = "client";
    private String photoUrl = "";
    private boolean loggingOut = false;

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_profile);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) { openLogin(); return; }

        imgProfile = findViewById(R.id.img_profile);
        txtName = findViewById(R.id.txt_name);
        txtRole = findViewById(R.id.txt_role);
        txtOrder = findViewById(R.id.txt_order_count);
        txtProgress = findViewById(R.id.txt_progress_count);
        txtRating = findViewById(R.id.txt_rating_count);

        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            TransitionUtil.slideRight(this);
        });

        findViewById(R.id.row_service).setOnClickListener(v -> {
            startActivity(new Intent(this, EditServiceActivity.class));
            TransitionUtil.slideRight(this);
        });

        findViewById(R.id.row_rules).setOnClickListener(v -> {
            if ("editor".equalsIgnoreCase(role)) showPricingRulesDialog();
            else showClientRulesDialog();
        });

        findViewById(R.id.row_dispute).setOnClickListener(v -> {
            showReportDialog();
        });

        findViewById(R.id.row_logout).setOnClickListener(v -> confirmLogout());

        findViewById(R.id.btn_setting).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            TransitionUtil.slideUp(this);
        });

        NavUtil.setupClientNav(this, 3);
        ChatUnreadUtil.listenUnreadBadge((TextView) findViewById(R.id.nav_chat), false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
        loadProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!loggingOut && auth != null && auth.getCurrentUser() != null) {
            PresenceUtil.offline();
        }
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("user").document(uid).addSnapshotListener((doc, e) -> {
            if (doc == null || !doc.exists()) return;
            bindProfile(doc);
        });
    }

    private void bindProfile(DocumentSnapshot doc) {
        role = safe(doc.getString("role"), "client");
        NavUtil.saveRole(this, role);
        String nama = safe(doc.getString("nama"), "Pengguna");
        photoUrl = safe(doc.getString("fotoProfil"), "");
        txtName.setText(nama);
        txtRole.setText(role.equalsIgnoreCase("editor") ? "Editor • Bronze User" : "Client • Bronze User");

        boolean isEditor = "editor".equalsIgnoreCase(role);

        View rowService = findViewById(R.id.row_service);
        if (rowService != null) {
            rowService.setVisibility(isEditor ? View.VISIBLE : View.GONE);
        }

        TextView txtRulesTitle = findViewById(R.id.txt_rules_title);
        if (txtRulesTitle != null) {
            txtRulesTitle.setText(isEditor ? "Aturan Tarif & Paket Editor" : "Aturan & Panduan Klien");
        }

        ImageLoader.load(imgProfile, photoUrl);
        loadActivityStats();
    }

    private void loadActivityStats() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        TextView txtStat3Label = findViewById(R.id.txt_stat_3_label);

        if ("editor".equalsIgnoreCase(role)) {
            if (txtStat3Label != null) txtStat3Label.setText("Rating");

            // Load Orders count for Editor
            db.collection("orders").whereEqualTo("editorId", uid).addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                int total = snap.size();
                int progress = 0;
                for (DocumentSnapshot d : snap) {
                    String st = safe(d.getString("status"), "");
                    if (!st.equalsIgnoreCase("selesai") && !st.equalsIgnoreCase("done")) progress++;
                }
                if (txtOrder != null) txtOrder.setText(String.valueOf(total));
                if (txtProgress != null) txtProgress.setText(String.valueOf(progress));
            });

            // Load Real Editor Rating Score on 5.0 scale
            db.collection("ratings").whereEqualTo("editorId", uid).addSnapshotListener((snap, e) -> {
                if (snap == null || snap.isEmpty()) {
                    if (txtRating != null) txtRating.setText("0.0");
                    return;
                }
                int count = snap.size();
                double sum = 0;
                for (DocumentSnapshot d : snap) {
                    Double r = d.getDouble("rating");
                    if (r == null) {
                        Long lv = d.getLong("rating");
                        if (lv != null) r = lv.doubleValue();
                    }
                    if (r != null) sum += r;
                }
                double avg = count == 0 ? 0.0 : Math.min(5.0, Math.max(0.0, sum / count));
                if (txtRating != null) txtRating.setText(String.format(java.util.Locale.US, "%.1f", avg));
            });
        } else {
            if (txtStat3Label != null) txtStat3Label.setText("Selesai");

            // Client Stats: Total Orders | Active Process | Completed Orders
            db.collection("orders").whereEqualTo("clientId", uid).addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                int total = snap.size();
                int progress = 0, done = 0;
                for (DocumentSnapshot d : snap) {
                    String st = safe(d.getString("status"), "");
                    if (st.equalsIgnoreCase("selesai") || st.equalsIgnoreCase("done")) done++;
                    else progress++;
                }
                if (txtOrder != null) txtOrder.setText(String.valueOf(total));
                if (txtProgress != null) txtProgress.setText(String.valueOf(progress));
                if (txtRating != null) txtRating.setText(String.valueOf(done));
            });
        }
    }

    private void showReportDialog() {
        String[] options = {"Laporkan Kendala Order / Proyek", "Pusat Bantuan & Layanan", "Buka Halaman Revisi / Sengketa"};
        new AlertDialog.Builder(this)
                .setTitle("Laporkan Masalah / Bantuan")
                .setItems(options, (d, i) -> {
                    if (i == 2) {
                        startActivity(new Intent(this, RevisionDisputeActivity.class));
                        TransitionUtil.slideRight(this);
                    } else {
                        Toast.makeText(this, "Fitur laporan kendala dan bantuan dapat diakses melalui menu Chat atau Customer Service.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Yakin ingin keluar dari akun ini?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Keluar", (d, w) -> logout())
                .show();
    }

    private void logout() {
        loggingOut = true;
        LogoutUtil.logout(this);
    }

    private void openLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showPricingRulesDialog() {
        String message = "Kebijakan & Aturan Tarif Paket Editor KuEditin:\n\n" +
                "1. Biaya Layanan Platform (Potongan 5%)\n" +
                "• Setiap transaksi proyek/paket jasa editor yang selesai dikenakan potongan biaya layanan platform sebesar 5%.\n" +
                "• Pendapatan bersih editor adalah 95% dari total nilai transaksi order.\n\n" +
                "2. Pencairan Saldo (Withdrawal)\n" +
                "• Pendapatan bersih dapat ditarik kapan saja melalui Pengaturan -> Penarikan Uang / Saldo ke DANA atau Rekening Bank.\n\n" +
                "3. Status Editor Pro vs Standar\n" +
                "• Status 'Editor Pro' dan 'Editor Standar' diverifikasi oleh Admin HRD berdasarkan portofolio & kualitas hasil kerja.\n\n" +
                "4. Waktu Pengerjaan & Revisi Bebas\n" +
                "• Waktu pengerjaan & jumlah revisi disepakati fleksibel bersama klien.\n\n" +
                "5. Rentang Harga Paket\n" +
                "• Editor Standar: Rp50.000 – Rp300.000 per proyek.\n" +
                "• Editor Pro: Rp300.000 – Rp2.000.000+ per proyek.\n\n" +
                "6. Biaya Tambahan & Diskon\n" +
                "• Editor berhak mendiskusikan biaya kustom tambahan jika proyek memiliki tingkat kesulitan tinggi.";

        new AlertDialog.Builder(this)
                .setTitle("Aturan Tarif & Paket Editor")
                .setMessage(message)
                .setPositiveButton("Saya Mengerti", null)
                .show();
    }

    private void showClientRulesDialog() {
        String message = "Panduan & Aturan Penggunaan Klien KuEditin:\n\n" +
                "1. Pemesanan & Detail Brief\n" +
                "• Klien memberikan instruksi/brief pesanan secara jelas saat membuat order atau berdiskusi dengan editor.\n\n" +
                "2. Komunikasi & Chat\n" +
                "• Diskusi proyek, pengiriman materi, dan instruksi dilakukan secara langsung melalui fitur chat resmi aplikasi KuEditin.\n\n" +
                "3. Keamanan Transaksi\n" +
                "• Pembayaran diproses secara aman melalui sistem KuEditin sebelum pengerjaan proyek dimulai.\n\n" +
                "4. Pengajuan Revisi\n" +
                "• Pengajuan revisi dilakukan secara fleksibel sesuai kesepakatan bersama editor melalui menu order/revisi.";

        new AlertDialog.Builder(this)
                .setTitle("Aturan & Panduan Klien")
                .setMessage(message)
                .setPositiveButton("Saya Mengerti", null)
                .show();
    }

    private static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
}
