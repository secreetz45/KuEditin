package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class SuccessRegisterActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FirebaseAuth.getInstance().signOut();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#F8FAFC"));
        setContentView(root);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(34), 0, dp(34), 0);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        root.addView(content, cp);

        FrameLayout badge = new FrameLayout(this);
        badge.setBackgroundResource(R.drawable.circle_soft);
        content.addView(badge, new LinearLayout.LayoutParams(dp(236), dp(236)));

        TextView circle = new TextView(this);
        circle.setText("✓");
        circle.setTextColor(Color.WHITE);
        circle.setTextSize(86);
        circle.setTypeface(null, 1);
        circle.setGravity(Gravity.CENTER);
        circle.setBackgroundResource(R.drawable.circle_blue);
        FrameLayout.LayoutParams tickP = new FrameLayout.LayoutParams(dp(146), dp(146), Gravity.CENTER);
        badge.addView(circle, tickP);

        TextView title = tv("Registrasi Berhasil", 34, "#0F172A", true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(46), 0, 0);
        content.addView(title, tp);

        TextView sub = tv("Akun kamu sudah berhasil dibuat\ndan siap digunakan.", 18, "#64748B", false);
        sub.setGravity(Gravity.CENTER);
        sub.setLineSpacing(dp(6), 1);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(14), 0, 0);
        content.addView(sub, sp);

        TextView btn = tv("Lanjut ke Login", 18, "#FFFFFF", true);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(R.drawable.bg_btn);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(64));
        bp.setMargins(0, dp(110), 0, 0);
        content.addView(btn, bp);
        btn.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private TextView tv(String s, int size, String color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color)); if (bold) t.setTypeface(null,1); return t;
    }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}
