package com.example.mediamatch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.os.Build;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int RC_GOOGLE_SIGN_IN = 4501;
    private static final String PREF_AUTH = "mediamatch_auth";
    private static final String KEY_MANUAL_LOGOUT = "manual_logout";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private boolean isNavigatingToRegister = false;

    private boolean isFreshLaunch = false;

    @Override
    public void onCreate(Bundle b) {
        ThemeUtil.initTheme(this);
        super.onCreate(b);
        isFreshLaunch = (b == null);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupGoogleLogin();
        applyEntranceAnimations();
        applyBackgroundBlur();

        View logoContainer = findViewById(R.id.logo_container);
        if (logoContainer != null) {
            logoContainer.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
        }

        findViewById(R.id.btn_register_main).setOnClickListener(v -> {
            isNavigatingToRegister = true;
            LogoutUtil.clearManualLogout(this);
            try { auth.signOut(); } catch (Exception ignored) {}
            startActivity(new Intent(this, RegisterRoleActivity.class));
            TransitionUtil.slideRight(this);
        });

        findViewById(R.id.btn_login).setOnClickListener(v -> loginFirebase());
        findViewById(R.id.btn_forgot_password).setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
            TransitionUtil.slideUp(this);
        });
        findViewById(R.id.btn_google_login).setOnClickListener(v -> loginWithGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isNavigatingToRegister || !isFreshLaunch) {
            return;
        }
        boolean manualLogout = LogoutUtil.isManualLogout(this) || getIntent().getBooleanExtra("from_logout", false);
        if (manualLogout) {
            try { auth.signOut(); } catch (Exception ignored) {}
            try { if (googleSignInClient != null) googleSignInClient.signOut(); } catch (Exception ignored) {}
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            routeByRole(user.getUid(), false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNavigatingToRegister) {
            isNavigatingToRegister = false;
        }
    }

    private void setupGoogleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void applyBackgroundBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            View bgView = findViewById(R.id.bg_blur_view);
            if (bgView != null) {
                bgView.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP));
            }
        }
    }

    private void applyEntranceAnimations() {
        View logo = findViewById(R.id.logo_container);
        View card = findViewById(R.id.login_card);
        View footer = findViewById(R.id.register_prompt);

        // Set initial states for a smoother staggered entrance
        logo.setAlpha(0f);
        logo.setTranslationY(60);
        card.setAlpha(0f);
        card.setTranslationY(100);
        footer.setAlpha(0f);
        footer.setTranslationY(40);

        // Sequence: Logo -> Card -> Footer
        logo.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(900)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();

        card.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(1000)
                .setStartDelay(450)
                .setInterpolator(new DecelerateInterpolator(1.2f))
                .start();

        footer.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(800)
                .setStartDelay(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void loginFirebase() {
        LogoutUtil.clearManualLogout(this);
        EditText email = findViewById(R.id.edt_email_login);
        EditText password = findViewById(R.id.edt_password_login);
        TextView errEmail = findViewById(R.id.error_email_login);
        TextView errPass = findViewById(R.id.error_password_login);
        View card = findViewById(R.id.login_card);

        String e = email.getText().toString().trim();
        String p = password.getText().toString().trim();

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        // Setup TextWatchers to clear errors while typing
        if (email.getTag() == null) {
            setupLiveValidation(email, errEmail);
            setupLiveValidation(password, errPass);
            email.setTag(true);
        }

        boolean hasError = false;

        if (e.isEmpty()) {
            email.setBackgroundResource(R.drawable.bg_input_error);
            errEmail.setVisibility(View.VISIBLE);
            errEmail.setAlpha(1.0f);
            errEmail.setTranslationY(0);
            hasError = true;
        }

        if (p.isEmpty()) {
            showErrorLabel(password, errPass, shake);
            hasError = true;
        }

        if (hasError) {
            // Only shake card if password is empty (to respect "no animation for email" request)
            if (!p.isEmpty()) {
                card.startAnimation(shake);
            }
            return;
        }

        auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Login gagal", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    routeByRole(user.getUid(), true);
                })
                .addOnFailureListener(e1 -> Toast.makeText(this, "Login gagal: " + e1.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showErrorLabel(EditText field, TextView label, Animation shake) {
        field.setBackgroundResource(R.drawable.bg_input_error);
        field.startAnimation(shake);
        
        if (label.getVisibility() != View.VISIBLE) {
            label.setAlpha(0f);
            label.setTranslationY(-20);
            label.setVisibility(View.VISIBLE);
            label.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    private void setupLiveValidation(EditText field, TextView errorLabel) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    field.setBackgroundResource(R.drawable.bg_input);
                    errorLabel.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loginWithGoogle() {
        LogoutUtil.clearManualLogout(this);
        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.startsWith("PASTE_WEB_CLIENT_ID_HERE")) {
            new AlertDialog.Builder(this)
                    .setTitle("Google Login belum siap")
                    .setMessage("Tambahkan SHA-1 dan SHA-256 di Firebase, download ulang google-services.json, lalu pastikan default_web_client_id sudah terisi otomatis.")
                    .setPositiveButton("Mengerti", null)
                    .show();
            return;
        }
        googleSignInClient.signOut().addOnCompleteListener(task ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN)
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                Toast.makeText(this, "Login Google gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account == null || account.getIdToken() == null) {
            Toast.makeText(this, "Token Google tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Login Google gagal", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    checkGoogleUserProfile(user);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Firebase Google gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkGoogleUserProfile(FirebaseUser user) {
        db.collection("user").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.getString("role") != null) {
                        routeByRole(user.getUid(), true);
                    } else {
                        showChooseRoleDialog(user);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal cek akun Google: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChooseRoleDialog(FirebaseUser user) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(18));
        root.setBackground(round("#FFFFFF", 26, 0, "#FFFFFF"));

        TextView title = tv("Selamat Datang 👋", 24, "#0F172A", true);
        root.addView(title);

        TextView desc = tv("Login Google berhasil. Pilih bagaimana kamu ingin menggunakan KuEditin.", 14, "#64748B", false);
        desc.setPadding(0, dp(8), 0, dp(16));
        root.addView(desc);

        root.addView(roleCard(0, "Saya Editor", "Terima order, kelola progres, dan kirim hasil pekerjaan", "Masuk sebagai Editor", true, () -> {
            dialog.dismiss();
            goToGoogleRegistration(user, "editor");
        }));

        root.addView(roleCard(0, "Saya Klien", "Cari editor terbaik, pesan jasa, chat, dan pantau progres", "Masuk sebagai Client", false, () -> {
            dialog.dismiss();
            goToGoogleRegistration(user, "client");
        }));

        TextView cancel = tv("Batal", 15, "#64748B", true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(10), 0, 0);
        cancel.setOnClickListener(v -> {
            getSharedPreferences(PREF_AUTH, MODE_PRIVATE).edit().putBoolean(KEY_MANUAL_LOGOUT, true).apply();
            FirebaseAuth.getInstance().signOut();
            try { googleSignInClient.signOut(); } catch (Exception ignored) {}
            dialog.dismiss();
        });
        root.addView(cancel);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90f), -2);
        }
    }

    private LinearLayout roleCard(int iconRes, String title, String subtitle, String button, boolean primary, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(primary ? "#1A73E8" : "#FFFFFF", 22, 1, primary ? "#1A73E8" : "#E2E8F0"));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);
        card.setOnClickListener(v -> action.run());

        FrameLayout icContainer = new FrameLayout(this);
        icContainer.setLayoutParams(new LinearLayout.LayoutParams(dp(58), dp(58)));
        icContainer.setBackground(round(primary ? "#4285F4" : "#EAF4FF", 999, 0, "#FFFFFF"));
        
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams ivp = new FrameLayout.LayoutParams(dp(40), dp(40));
        ivp.gravity = Gravity.CENTER;
        iv.setLayoutParams(ivp);
        
        icContainer.addView(iv);
        card.addView(icContainer);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(14), 0, 0, 0);
        card.addView(texts, lp);
        texts.addView(tv(title, 18, primary ? "#FFFFFF" : "#0F172A", true));
        TextView sub = tv(subtitle, 13, primary ? "#EAF4FF" : "#64748B", false);
        sub.setPadding(0, dp(3), 0, dp(6));
        texts.addView(sub);
        texts.addView(tv(button, 13, primary ? "#FFFFFF" : "#1A73E8", true));
        return card;
    }

    private void goToGoogleRegistration(FirebaseUser user, String role) {
        Intent intent;
        if ("editor".equals(role)) {
            intent = new Intent(this, RegisterEditorActivity.class);
        } else {
            intent = new Intent(this, RegisterClientActivity.class);
        }
        intent.putExtra("isGoogleLogin", true);
        intent.putExtra("googleName", user.getDisplayName());
        intent.putExtra("googleEmail", user.getEmail());
        intent.putExtra("googlePhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        startActivity(intent);
    }

    private void saveGoogleUserRole(FirebaseUser user, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("nama", user.getDisplayName() == null ? "Pengguna Google" : user.getDisplayName());
        data.put("email", user.getEmail() == null ? "" : user.getEmail());
        data.put("noHp", "");
        data.put("role", role);
        data.put("fotoProfil", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
        data.put("provider", "google");
        data.put("rating", 0);
        data.put("hargaMulai", 0);
        data.put("createdAt", System.currentTimeMillis());

        db.collection("user").document(user.getUid()).set(data)
                .addOnSuccessListener(unused -> {
                    LogoutUtil.clearManualLogout(this);
                    routeByRole(user.getUid(), true);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan akun Google: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void routeByRole(String uid, boolean showError) {
        if (isNavigatingToRegister) return;
        db.collection("user").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (isNavigatingToRegister || isFinishing()) return;
                    if (!snapshot.exists()) {
                        try { auth.signOut(); } catch (Exception ignored) {}
                        if (showError) Toast.makeText(this, "Data profil tidak ditemukan. Silakan login atau daftar ulang.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String role = snapshot.getString("role");
                    NavUtil.saveRole(this, role);
                    boolean onboardingComplete = snapshot.getBoolean("onboarding_complete") != null && snapshot.getBoolean("onboarding_complete");

                    String accountStatus = snapshot.getString("account_status");
                    if (accountStatus == null || accountStatus.isEmpty()) {
                        accountStatus = snapshot.getString("status");
                    }
                    String warningNote = snapshot.getString("warning_note");

                    if ("banned".equalsIgnoreCase(accountStatus)) {
                        Toast.makeText(this, "AKUN DIBLOKIR: " + (warningNote != null && !warningNote.isEmpty() ? warningNote : "Akun Anda telah diblokir permanen oleh Admin."), Toast.LENGTH_LONG).show();
                        LogoutUtil.logout(this);
                        return;
                    } else if ("suspended".equalsIgnoreCase(accountStatus)) {
                        Toast.makeText(this, "AKUN DIBEKUKAN: " + (warningNote != null && !warningNote.isEmpty() ? warningNote : "Akun Anda sedang dibekukan sementara oleh Admin."), Toast.LENGTH_LONG).show();
                        LogoutUtil.logout(this);
                        return;
                    } else if (warningNote != null && !warningNote.isEmpty()) {
                        Toast.makeText(this, "PERINGATAN ADMIN: " + warningNote, Toast.LENGTH_LONG).show();
                    }

                    PresenceUtil.online();
                    AppNotificationUtil.start(this);

                    if ("client".equalsIgnoreCase(role)) {
                        if (!onboardingComplete) {
                            Intent intent = new Intent(this, ClientOnboardingActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(this, HomeClientActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    } else if ("editor".equalsIgnoreCase(role)) {
                        String status = snapshot.getString("status");
                        String s = status == null ? "" : status.toLowerCase().trim();
                        boolean isApproved = snapshot.getBoolean("isApproved") != null && snapshot.getBoolean("isApproved");
                        if (snapshot.getBoolean("isVerified") != null && snapshot.getBoolean("isVerified")) isApproved = true;

                        boolean isPending = ("pending_hrd".equals(s) || "pending".equals(s) || "menunggu".equals(s)) && !isApproved;

                        if (isPending) {
                            startActivity(new Intent(this, WaitingApprovalActivity.class));
                        } else {
                            Intent intent = new Intent(this, EditorHomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    } else if (showError) {
                        Toast.makeText(this, "Role akun belum valid", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (showError) Toast.makeText(this, "Gagal membaca data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private TextView tv(String text, int sp, String color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor(color));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable round(String color, int radius, int stroke, String strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radius));
        if (stroke > 0) g.setStroke(dp(stroke), Color.parseColor(strokeColor));
        return g;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

}
