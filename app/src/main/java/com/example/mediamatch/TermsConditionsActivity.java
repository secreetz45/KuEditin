package com.example.mediamatch;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TermsConditionsActivity extends AppCompatActivity {
    private TextView tabClient, tabEditor, tabGeneral;
    private LinearLayout termsContentBox;
    private String activeTab = "client";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnAccept = findViewById(R.id.btn_accept_terms);
        if (btnAccept != null) btnAccept.setOnClickListener(v -> finish());

        tabClient = findViewById(R.id.tab_client_terms);
        tabEditor = findViewById(R.id.tab_editor_terms);
        tabGeneral = findViewById(R.id.tab_general_terms);
        termsContentBox = findViewById(R.id.terms_content_box);

        if (tabClient != null) tabClient.setOnClickListener(v -> switchTab("client"));
        if (tabEditor != null) tabEditor.setOnClickListener(v -> switchTab("editor"));
        if (tabGeneral != null) tabGeneral.setOnClickListener(v -> switchTab("general"));

        renderTerms();
    }

    private void switchTab(String tab) {
        activeTab = tab;
        updateTabStyle();
        renderTerms();
    }

    private void updateTabStyle() {
        setTabStyle(tabClient, "client".equals(activeTab));
        setTabStyle(tabEditor, "editor".equals(activeTab));
        setTabStyle(tabGeneral, "general".equals(activeTab));
    }

    private void setTabStyle(TextView tab, boolean active) {
        if (tab == null) return;
        tab.setBackgroundResource(active ? R.drawable.bg_btn : R.drawable.bg_chip);
        tab.setTextColor(active ? Color.WHITE : getResources().getColor(R.color.textMuted));
        tab.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void renderTerms() {
        if (termsContentBox == null) return;
        termsContentBox.removeAllViews();

        if ("client".equals(activeTab)) {
            addSection("1. Keamanan Pembayaran Escrow", "Seluruh pembayaran pesanan Klien ditahan secara 100% aman oleh sistem Rekening Bersama KuEditin. Dana baru diteruskan ke Editor setelah Klien memeriksa dan menyetujui hasil pengerjaan.");
            addSection("2. Kejelasan Instruksi & Brief Proyek", "Klien wajib memberikan detail instruksi, kebutuhan format video/desain, serta melampirkan file bahan/link cloud (Google Drive/Dropbox) secara jelas sebelum pengerjaan dimulai.");
            addSection("3. Hak Cipta & Kepemilikan Karya", "Setelah pembayaran dilunasi dan proyek diterima, hak cipta penuh atas file hasil pengerjaan menjadi milik Klien sepenuhnya.");
            addSection("4. Hak Pengajuan Revisi", "Klien berhak mengajukan revisi pengerjaan sesuai dengan batas jumlah revisi yang tertera pada paket jasa editor yang dipilih.");
        } else if ("editor".equals(activeTab)) {
            addSection("1. Biaya Komisi Layanan Sistem 10%", "KuEditin mengenakan biaya komisi layanan sebesar 10% dari setiap transaksi order yang selesai. Editor mengantongi 90% pendapatan bersih yang dapat ditarik kapan saja.");
            addSection("2. Ketepatan Waktu Pengerjaan", "Editor wajib merespon pesan Klien dan menyelesaikan proyek sesuai dengan target deadline yang telah disepakati pada paket jasa.");
            addSection("3. Orisinalitas & Bebas Plagiarisme", "Editor menjamin seluruh karya, elemen grafis, musik, dan aset yang digunakan bebas dari pelanggaran hak cipta pihak ketiga.");
            addSection("4. Verifikasi & Approval Akun HRD", "Akun editor baru akan melalui tahap pemeriksaan verifikasi tim HRD KuEditin sebelum dapat tampil publik dan menerima order Klien.");
        } else {
            addSection("1. Kebijakan Penarikan Saldo Dompet", "Penarikan saldo hasil pekerjaan dapat diajukan kapan saja melalui E-Wallet DANA atau Transfer Bank tanpa biaya tersembunyi.");
            addSection("2. Larangan Konten Ilegal & SARA", "Dilarang keras membuat order atau mengunggah bahan/hasil pengerjaan yang mengandung unsur SARA, pornografi, atau melanggar hukum.");
            addSection("3. Sistem Penyelesaian Sengketa", "Jika terjadi ketidaksesuaian antara Klien dan Editor, tim Mediasi KuEditin akan meninjau bukti obrolan & brief untuk memberikan keputusan adil.");
        }
    }

    private void addSection(String title, String description) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(getResources().getColor(R.color.textPrimary));

        TextView d = new TextView(this);
        d.setText(description);
        d.setTextSize(13);
        d.setTextColor(getResources().getColor(R.color.textSecondary));
        d.setLineSpacing(dp(4), 1.1f);
        d.setPadding(0, dp(4), 0, dp(16));

        termsContentBox.addView(t);
        termsContentBox.addView(d);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
