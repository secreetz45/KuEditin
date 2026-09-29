package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends androidx.appcompat.app.AppCompatActivity {
    private LinearLayout container;
    private EditText searchChat;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myId;

    private ListenerRegistration chatListener;
    private final List<DocumentSnapshot> allChatDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_chat_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        container = findViewById(R.id.chat_container);
        searchChat = findViewById(R.id.search_chat);

        NavUtil.setupClientNav(this, 2);
        ChatUnreadUtil.listenUnreadBadge((TextView) findViewById(R.id.nav_chat), true);

        View logoM = findViewById(R.id.logoM);
        if (logoM != null) {
            logoM.setOnClickListener(v -> ThemeUtil.toggleTheme(this));
        }

        if (searchChat != null) {
            searchChat.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterChats();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (auth.getCurrentUser() == null) {
            showMessage("Silakan login terlebih dahulu");
            return;
        }
        myId = auth.getCurrentUser().getUid();
        loadChats();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }

    private void loadChats() {
        if (myId == null) return;
        showMessage("Memuat chat...");

        if (chatListener != null) chatListener.remove();
        chatListener = db.collection("chats")
                .whereArrayContains("participantIds", myId)
                .addSnapshotListener((snap, e) -> {
                    if (isFinishing()) return;
                    if (e != null) {
                        showMessage("Gagal memuat chat: " + e.getMessage());
                        return;
                    }

                    allChatDocs.clear();
                    if (snap != null && !snap.isEmpty()) {
                        allChatDocs.addAll(snap.getDocuments());
                    }

                    filterChats();
                });
    }

    private void filterChats() {
        if (container == null) return;
        container.removeAllViews();

        if (allChatDocs.isEmpty()) {
            showMessage("Belum ada riwayat chat\nBuka profil editor lalu tekan tombol Chat.");
            return;
        }

        String keyword = searchChat != null ? searchChat.getText().toString().trim().toLowerCase() : "";
        int shownCount = 0;

        for (DocumentSnapshot doc : allChatDocs) {
            String clientId = doc.getString("clientId");
            String editorId = doc.getString("editorId");
            String editorName = safe(doc.getString("editorName"), "Editor");
            String clientName = safe(doc.getString("clientName"), "Client");
            String otherName = myId.equals(clientId) ? editorName : clientName;
            String lastMessage = safe(doc.getString("lastMessage"), "Belum ada pesan");

            boolean nameMatch = keyword.isEmpty() || otherName.toLowerCase().contains(keyword);

            if (nameMatch) {
                container.addView(chatCard(doc));
                shownCount++;
            }
        }

        if (shownCount == 0) {
            showMessage("Obrolan tidak ditemukan");
        }
    }

    private View chatCard(DocumentSnapshot doc) {
        String clientId = doc.getString("clientId");
        String editorId = doc.getString("editorId");
        String otherId = getOtherId(doc, clientId, editorId);
        String editorName = safe(doc.getString("editorName"), "Editor");
        String clientName = safe(doc.getString("clientName"), "Client");
        String otherName = myId.equals(clientId) ? editorName : clientName;
        String last = safe(doc.getString("lastMessage"), "Belum ada pesan");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cp);

        CircleImageView avatar = new CircleImageView(this);
        avatar.setBackgroundResource(R.drawable.circle_soft);
        card.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1);
        ip.setMargins(dp(14), 0, 0, 0);
        card.addView(info, ip);

        TextView nameView = text(otherName, 16, getResources().getColor(R.color.textPrimary), true);
        info.addView(nameView);

        TextView statusView = text("Offline", 12, getResources().getColor(R.color.textMuted), false);
        statusView.setPadding(0, dp(2), 0, 0);
        info.addView(statusView);

        TextView msg = text(last, 13, getResources().getColor(R.color.textSecondary), false);
        msg.setPadding(0, dp(4), 0, 0);
        msg.setMaxLines(1);
        msg.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(msg);

        TextView badge = text("", 11, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.circle_blue);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(26), dp(26));
        card.addView(badge, bp);

        long unread = unreadCount(doc);
        if (unread > 0) badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        else badge.setVisibility(View.GONE);

        if (!otherId.isEmpty()) {
            db.collection("user").document(otherId).get().addOnSuccessListener(user -> {
                if (user != null && user.exists()) {
                    String updatedName = safe(user.getString("nama"), otherName);
                    nameView.setText(updatedName);
                    ImageLoader.load(avatar, safe(user.getString("fotoProfil"), ""));
                }
            });

            FirebaseDatabase.getInstance().getReference("status").child(otherId)
                    .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                        @Override public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            String state = snapshot.child("state").getValue(String.class);
                            Long last = snapshot.child("lastChanged").getValue(Long.class);
                            if (PresenceUtil.isFreshOnline(state, last)) {
                                statusView.setText("Online");
                                statusView.setTextColor(Color.parseColor("#22C55E"));
                            } else {
                                statusView.setText(PresenceUtil.lastOnlineText(last));
                                statusView.setTextColor(Color.parseColor("#64748B"));
                            }
                        }
                        @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {
                            statusView.setText("Offline");
                        }
                    });
        }

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatRoomActivity.class);
            i.putExtra("chatId", doc.getId());
            i.putExtra("otherName", nameView.getText().toString());
            startActivity(i);
            TransitionUtil.slideRight(this);
        });

        return card;
    }

    private String getOtherId(DocumentSnapshot doc, String clientId, String editorId) {
        try {
            List<?> ids = (List<?>) doc.get("participantIds");
            if (ids != null) {
                for (Object id : ids) if (id != null && !id.toString().equals(myId)) return id.toString();
            }
        } catch (Exception ignored) {}
        if (clientId != null && !clientId.equals(myId)) return clientId;
        if (editorId != null && !editorId.equals(myId)) return editorId;
        return "";
    }

    private long unreadCount(DocumentSnapshot doc) {
        Object v = doc.get("unread_" + myId);
        if (v instanceof Number) return ((Number) v).longValue();
        return 0;
    }

    private void showMessage(String msg) {
        if (container == null) return;
        container.removeAllViews();
        TextView tv = text(msg, 15, Color.parseColor("#64748B"), false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(18), dp(30), dp(18), dp(30));
        tv.setBackgroundResource(R.drawable.bg_empty_state);
        container.addView(tv, new LinearLayout.LayoutParams(-1, -2));
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s == null ? "" : s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
