package com.example.mediamatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterRoleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_register_role);

        LogoutUtil.clearManualLogout(this);
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                TransitionUtil.slideLeft(this);
            });
        }

        View logoRole = findViewById(R.id.logo_role);
        if (logoRole != null) {
            logoRole.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
        }

        View cardClient = findViewById(R.id.card_client);
        if (cardClient != null) {
            cardClient.setOnClickListener(v -> openRegisterClient());
        }

        View cardEditor = findViewById(R.id.card_editor);
        if (cardEditor != null) {
            cardEditor.setOnClickListener(v -> openRegisterEditor());
        }

        View btnMasuk = findViewById(R.id.btn_masuk);
        if (btnMasuk != null) {
            btnMasuk.setOnClickListener(v -> {
                finish();
                TransitionUtil.slideLeft(this);
            });
        }
    }

    private void openRegisterClient() {
        try {
            Intent i = new Intent(this, RegisterClientActivity.class);
            startActivity(i);
            TransitionUtil.slideRight(this);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka pendaftaran client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openRegisterEditor() {
        try {
            Intent i = new Intent(this, RegisterEditorActivity.class);
            startActivity(i);
            TransitionUtil.slideRight(this);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka pendaftaran editor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        TransitionUtil.slideLeft(this);
    }
}
