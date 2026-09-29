package com.example.mediamatch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodsActivity extends Activity {
    private String selected = "DANA";
    private EditText edDana, edBankName, edBankNumber, edQris;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final List<TextView> chips = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.parseColor("#F8FAFC")); setContentView(root);
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(24), dp(42), dp(24), dp(20)); header.setBackgroundColor(Color.parseColor("#1A73E8")); root.addView(header, new LinearLayout.LayoutParams(-1, dp(178)));
        TextView title = text("‹  Metode Pembayaran", 29, "#FFFFFF", true); title.setOnClickListener(v -> finish()); header.addView(title);
        TextView sub = text("Simpan metode pembayaran utama untuk simulasi transaksi KuEditin.", 14, "#EAF4FF", false); sub.setPadding(0, dp(10),0,0); header.addView(sub);

        ScrollView sv = new ScrollView(this); sv.setPadding(dp(22), dp(22), dp(22), dp(28)); root.addView(sv, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); sv.addView(c);

        LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); c.addView(chips);
        chip(chips, "DANA"); chip(chips, "QRIS"); chip(chips, "BANK");
        edDana = input(c, "Nomor DANA", "08xxxxxxxxxx");
        edQris = input(c, "Nama QRIS/Bisnis", "KuEditin / Nama toko");
        edBankName = input(c, "Nama Bank", "BCA / Mandiri / BRI");
        edBankNumber = input(c, "Nomor Rekening", "1234567890");

        TextView info = text("Catatan: pembayaran pada aplikasi ini adalah simulasi QR nominal untuk presentasi. Untuk pembayaran asli butuh payment gateway.", 13, "#64748B", false);
        info.setPadding(0, dp(14), 0, dp(14)); c.addView(info);

        TextView save = text("Simpan Metode Pembayaran", 16, "#FFFFFF", true); save.setGravity(Gravity.CENTER); save.setBackground(round("#1A73E8", 18, 0, "#1A73E8")); c.addView(save, new LinearLayout.LayoutParams(-1, dp(56))); save.setOnClickListener(v -> saveData());
        loadData();
    }
    private void chip(LinearLayout parent, String s) {
        TextView t = text(s,14,"#1A73E8",true);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(48),1);
        lp.setMargins(dp(4),0,dp(4),dp(16));
        parent.addView(t,lp);
        chips.add(t);
        t.setOnClickListener(v -> {
            selected = s;
            updateChips();
            Toast.makeText(this,"Metode utama: "+s,Toast.LENGTH_SHORT).show();
        });
        updateChips();
    }

    private void updateChips() {
        for (TextView t : chips) {
            boolean active = t.getText().toString().equalsIgnoreCase(selected);
            t.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#1A73E8"));
            t.setBackground(round(active ? "#1A73E8" : "#FFFFFF",999,1,active ? "#1A73E8" : "#E2E8F0"));
        }
    }
    private EditText input(LinearLayout parent, String label, String hint) { TextView l=text(label,15,"#0F172A",true); l.setPadding(0,dp(8),0,dp(7)); parent.addView(l); EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setTextColor(Color.parseColor("#0F172A")); e.setHintTextColor(Color.parseColor("#94A3B8")); e.setTextSize(15); e.setPadding(dp(18),0,dp(18),0); e.setBackground(round("#FFFFFF",18,1,"#E2E8F0")); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,0,0,dp(10)); parent.addView(e,lp); return e; }
    private void saveData(){ if(auth.getCurrentUser()==null)return; Map<String,Object> map=new HashMap<>(); map.put("paymentDefault",selected); map.put("dana",edDana.getText().toString().trim()); map.put("qrisName",edQris.getText().toString().trim()); map.put("bankName",edBankName.getText().toString().trim()); map.put("bankNumber",edBankNumber.getText().toString().trim()); db.collection("user").document(auth.getCurrentUser().getUid()).update(map).addOnSuccessListener(v->Toast.makeText(this,"Metode pembayaran disimpan",Toast.LENGTH_SHORT).show()).addOnFailureListener(e->Toast.makeText(this,"Gagal menyimpan",Toast.LENGTH_SHORT).show()); }
    private void loadData(){ if(auth.getCurrentUser()==null)return; db.collection("user").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{ if(!d.exists())return; edDana.setText(safe(d.getString("dana"))); edQris.setText(safe(d.getString("qrisName"))); edBankName.setText(safe(d.getString("bankName"))); edBankNumber.setText(safe(d.getString("bankNumber"))); selected=safe(d.getString("paymentDefault"),"DANA"); updateChips(); }); }
    @Override protected void onStart(){ super.onStart(); PresenceUtil.online(); }
    private static String safe(String v){return v==null?"":v;} private static String safe(String v,String f){return v==null||v.isEmpty()?f:v;}
    private TextView text(String s,int sp,String color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.parseColor(color));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(String color,int radius,int stroke,String strokeColor){GradientDrawable g=new GradientDrawable();g.setColor(Color.parseColor(color));g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(stroke),Color.parseColor(strokeColor));return g;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} }
