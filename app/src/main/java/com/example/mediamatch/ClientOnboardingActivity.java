package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientOnboardingActivity extends Activity {
    private TextView txtStep, txtQuestion, btnPrev, btnNext;
    private LinearLayout optionContainer;
    private View progressBar;
    
    private int currentStep = 1;
    private final int TOTAL_STEPS = 4;
    private final Map<Integer, List<String>> answers = new HashMap<>();
    
    private final String[][] questions = {
        {"Apa nuansa (vibe) karya yang kamu inginkan?", "Modern", "Minimalis", "Playful", "Vintage", "Sinematik"},
        {"Platform utama distribusi konten kamu?", "Instagram/TikTok", "YouTube", "Website/Web", "Televisi/Ads", "Internal/Pribadi"},
        {"Level editor yang kamu cari?", "Profesional Senior", "Berpengalaman", "Rising Star (Talenta Baru)", "Cepat & Ekonomis"},
        {"Jenis jasa yang paling sering dibutuhkan?", "Video Editing", "Graphic Design", "Logo & Branding", "UI/UX Design", "Motion Graphic"}
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_client_onboarding);

        txtStep = findViewById(R.id.txt_step_counter);
        txtQuestion = findViewById(R.id.txt_question);
        optionContainer = findViewById(R.id.option_container);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        progressBar = findViewById(R.id.onboarding_progress);

        btnNext.setOnClickListener(v -> nextStep());
        btnPrev.setOnClickListener(v -> prevStep());

        loadStep();
    }

    private void loadStep() {
        txtStep.setText("Langkah " + currentStep + " dari " + TOTAL_STEPS);
        txtQuestion.setText(questions[currentStep-1][0]);
        btnPrev.setVisibility(currentStep == 1 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentStep == TOTAL_STEPS ? "MULAI SEKARANG" : "LANJUT");

        // Update Progress Bar
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) progressBar.getLayoutParams();
        lp.weight = (float) currentStep / TOTAL_STEPS;
        progressBar.setLayoutParams(lp);

        optionContainer.removeAllViews();
        String[] options = questions[currentStep-1];
        
        List<String> prevAnswers = answers.get(currentStep);
        
        for (int i = 1; i < options.length; i++) {
            final String optText = options[i];
            TextView tv = new TextView(this);
            tv.setText(optText);
            tv.setTextSize(16);
            tv.setPadding(dp(20), dp(18), dp(20), dp(18));
            tv.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            tv.setBackgroundResource(R.drawable.bg_option_item);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.setMargins(0, 0, 0, dp(12));
            tv.setLayoutParams(p);
            
            if (prevAnswers != null && prevAnswers.contains(optText.toLowerCase())) {
                tv.setSelected(true);
                tv.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            }
            
            tv.setOnClickListener(v -> {
                v.setSelected(!v.isSelected());
                if (v.isSelected()) {
                    ((TextView)v).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                } else {
                    ((TextView)v).setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
                }
            });
            
            optionContainer.addView(tv);
        }
    }

    private void nextStep() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < optionContainer.getChildCount(); i++) {
            TextView tv = (TextView) optionContainer.getChildAt(i);
            if (tv.isSelected()) {
                selected.add(tv.getText().toString().toLowerCase());
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Pilih minimal satu minat", Toast.LENGTH_SHORT).show();
            return;
        }

        answers.put(currentStep, selected);

        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            loadStep();
            // Simple animation
            optionContainer.setAlpha(0f);
            optionContainer.animate().alpha(1f).setDuration(400).start();
        } else {
            saveAndFinish();
        }
    }

    private void prevStep() {
        if (currentStep > 1) {
            currentStep--;
            loadStep();
        }
    }

    private void saveAndFinish() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<String> finalInterests = new ArrayList<>();
        for (List<String> stepAnswers : answers.values()) {
            finalInterests.addAll(stepAnswers);
        }

        Map<String, Object> update = new HashMap<>();
        update.put("interests", finalInterests);
        update.put("onboarding_complete", true);

        FirebaseFirestore.getInstance().collection("user").document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Rekomendasi telah siap!", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, HomeClientActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    public void onBackPressed() {
        if (currentStep > 1) prevStep();
        else super.onBackPressed();
    }
}
