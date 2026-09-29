package com.example.mediamatch;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        LinearLayout content = findViewById(R.id.settings_content);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (content != null) {
            content.addView(section("Keamanan & Akun"));
            rowIcon(content, 0, "Keamanan & Ubah Password", "Ganti kata sandi login akun kamu", () -> {
                startActivity(new Intent(this, ChangePasswordActivity.class));
                TransitionUtil.slideRight(this);
            });

            content.addView(section("Tampilan & Tema"));

            String themeSub = ThemeUtil.getThemeName(this);
            rowIcon(content, 0, "Mode Tema Aplikasi", themeSub, this::showThemeDialog);

            if (!NavUtil.isEditor(this)) {
                int currentCols = getSharedPreferences("app_settings", MODE_PRIVATE).getInt("feed_grid_mode", 2);
                String modeSub = currentCols == 1 ? "Mode 1 Kolom (List Full-Width)" : "Mode 2 Kolom (Grid Side-by-Side)";
                rowIcon(content, 0, "Mode Tampilan Beranda Klien", modeSub, this::showFeedModeDialog);
            }

            content.addView(section("Keuangan & Saldo"));
            rowIcon(content, 0, "Penarikan Uang / Saldo", "Cairkan saldo hasil pengerjaan proyek", () -> {
                startActivity(new Intent(this, WithdrawalActivity.class));
                TransitionUtil.slideRight(this);
            });

            content.addView(section("Aplikasi"));
            rowIcon(content, R.drawable.ic_setting_about, "Tentang KuEditin", "Versi 0.8.9 Beta", () -> {
                showInfo("Tentang KuEditin", "KuEditin v0.8.9 Beta\nPlatform marketplace jasa kreatif multimedia profesional.\n\nFitur: Dark Mode, Order kustom, live chat, upload portofolio, dan sistem keamanan akun.");
            });

            rowIcon(content, R.drawable.ic_setting_logout, "Keluar dari Akun", "Logout dari sesi aktif saat ini", this::confirmLogout);
        }
    }

    private void showThemeDialog() {
        String current = getSharedPreferences("app_settings", MODE_PRIVATE).getString("theme_mode", "light");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Pilih Tema");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.textPrimary));
        title.setPadding(0, 0, 0, dp(16));
        box.addView(title);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        View optionLight = createSelectionCard(0, "Mode Terang", "", "light".equalsIgnoreCase(current), () -> {
            ThemeUtil.setTheme(this, "light");
            Toast.makeText(this, "Tema diubah ke Mode Terang", Toast.LENGTH_SHORT).show();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            androidx.core.app.ActivityCompat.recreate(this);
        });
        box.addView(optionLight);

        View optionDark = createSelectionCard(0, "Mode Gelap", "", "dark".equalsIgnoreCase(current), () -> {
            ThemeUtil.setTheme(this, "dark");
            Toast.makeText(this, "Tema diubah ke Mode Gelap", Toast.LENGTH_SHORT).show();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            androidx.core.app.ActivityCompat.recreate(this);
        });
        box.addView(optionDark);

        View optionSystem = createSelectionCard(0, "Ikuti Sistem HP", "", "system".equalsIgnoreCase(current), () -> {
            ThemeUtil.setTheme(this, "system");
            Toast.makeText(this, "Tema disesuaikan dengan sistem HP", Toast.LENGTH_SHORT).show();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            androidx.core.app.ActivityCompat.recreate(this);
        });
        box.addView(optionSystem);

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("Batal", null)
                .create();

        dialogHolder[0].show();
    }

    private void showFeedModeDialog() {
        int currentCols = getSharedPreferences("app_settings", MODE_PRIVATE).getInt("feed_grid_mode", 2);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(getResources().getColor(R.color.cardSurface));

        TextView title = new TextView(this);
        title.setText("Mode Layout Beranda Klien");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.textPrimary));
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Pilih bagaimana daftar editor ditampilkan di beranda");
        sub.setTextSize(12);
        sub.setTextColor(getResources().getColor(R.color.textMuted));
        sub.setPadding(0, dp(4), 0, dp(16));
        box.addView(sub);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        View optionGrid = createSelectionCard(R.drawable.ic_layout_grid, "Mode 2 Kolom (Grid Side-by-Side)", "Tampilkan 2 kartu editor berdampingan per baris", currentCols == 2, () -> {
            getSharedPreferences("app_settings", MODE_PRIVATE).edit().putInt("feed_grid_mode", 2).apply();
            Toast.makeText(this, "Mode beranda diubah ke Grid 2 Kolom", Toast.LENGTH_SHORT).show();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            recreate();
        });
        box.addView(optionGrid);

        View optionList = createSelectionCard(R.drawable.ic_layout_list, "Mode 1 Kolom (List Full-Width)", "Tampilkan 1 kartu editor lebar penuh per baris", currentCols == 1, () -> {
            getSharedPreferences("app_settings", MODE_PRIVATE).edit().putInt("feed_grid_mode", 1).apply();
            Toast.makeText(this, "Mode beranda diubah ke List 1 Kolom", Toast.LENGTH_SHORT).show();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            recreate();
        });
        box.addView(optionList);

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("Batal", null)
                .create();

        dialogHolder[0].show();
    }

    private View createSelectionCard(int iconRes, String titleText, String subtitleText, boolean selected, Runnable onSelect) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(dp(selected ? 3 : 1));
        card.setStrokeColor(selected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        card.setCardBackgroundColor(selected ? getResources().getColor(R.color.bg_soft_blue) : getResources().getColor(R.color.cardSurface));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(row);

        if (iconRes != 0) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(iconRes);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(26), dp(26));
            ivLp.setMargins(0, 0, dp(12), 0);
            row.addView(iv, ivLp);
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
        row.addView(texts, tlp);

        TextView t = new TextView(this);
        t.setText(titleText);
        t.setTextSize(14);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(selected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textPrimary));
        texts.addView(t);

        if (subtitleText != null && !subtitleText.trim().isEmpty()) {
            TextView sub = new TextView(this);
            sub.setText(subtitleText);
            sub.setTextSize(12);
            sub.setTextColor(selected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textMuted));
            sub.setPadding(0, dp(2), 0, 0);
            texts.addView(sub);
        }

        TextView radioDot = new TextView(this);
        radioDot.setText(selected ? "●" : "○");
        radioDot.setTextSize(18);
        radioDot.setTextColor(selected ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.textMuted));
        row.addView(radioDot);

        card.setOnClickListener(v -> onSelect.run());
        return card;
    }

    private void rowIcon(LinearLayout parent, int iconRes, String title, String subtitle, Runnable action) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getResources().getColor(R.color.cardSurface));
        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setStrokeColor(getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(row);

        if (iconRes != 0) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(iconRes);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(iv, new LinearLayout.LayoutParams(dp(28), dp(28)));
        }

        LinearLayout txts = new LinearLayout(this);
        txts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
        tlp.setMargins(iconRes != 0 ? dp(14) : 0, 0, dp(8), 0);
        row.addView(txts, tlp);

        boolean isLogout = title.contains("Keluar") || title.contains("Logout");

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(isLogout ? Color.parseColor("#EF4444") : getResources().getColor(R.color.textPrimary));
        txts.addView(t);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextSize(12);
        sub.setTextColor(getResources().getColor(R.color.textMuted));
        sub.setPadding(0, dp(2), 0, 0);
        txts.addView(sub);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(20);
        arrow.setTextColor(isLogout ? Color.parseColor("#EF4444") : getResources().getColor(R.color.hint_color));
        row.addView(arrow);

        card.setOnClickListener(v -> action.run());
        parent.addView(card);
    }

    private TextView section(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(getResources().getColor(R.color.textPrimary));
        t.setPadding(dp(4), dp(16), 0, dp(10));
        return t;
    }

    private void showInfo(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Yakin ingin logout dari KuEditin?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Keluar", (d, w) -> logout())
                .show();
    }

    private void logout() {
        try {
            LogoutUtil.logout(this);
        } catch (Exception e) {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
