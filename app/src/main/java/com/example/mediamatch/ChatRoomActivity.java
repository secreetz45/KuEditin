package com.example.mediamatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends androidx.appcompat.app.AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String chatId;
    private String myId;
    private String otherId = "";

    private LinearLayout messageContainer;
    private EditText input;
    private ScrollView scroll;
    private CircleImageView imgOther;
    private TextView title, status;

    // Attachment Preview Views
    private View cardAttachmentPreview;
    private ImageView imgAttachmentThumb;
    private TextView txtAttachmentName;

    private static final int REQ_PICK_IMAGE = 701;
    private static final int REQ_CAMERA = 702;

    private Uri pendingImageUri = null;
    private String pendingFileName = "";

    @Override
    protected void onCreate(Bundle b) {
        ThemeUtil.applyTheme(this);
        super.onCreate(b);
        setContentView(R.layout.activity_chat_room);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatId = getIntent().getStringExtra("chatId");
        otherId = getIntent().getStringExtra("otherId") == null ? "" : getIntent().getStringExtra("otherId");
        myId = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();

        bindViews();

        if (chatId == null || chatId.trim().isEmpty()) {
            if (otherId == null || otherId.trim().isEmpty()) {
                Toast.makeText(this, "Chat tidak ditemukan", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            openDirectChatWithUser(otherId);
            return;
        }

        loadChatInfo();
        loadMessages();
    }

    private void bindViews() {
        messageContainer = findViewById(R.id.message_container);
        input = findViewById(R.id.input_message);
        scroll = findViewById(R.id.message_scroll);
        imgOther = findViewById(R.id.img_other_profile);
        title = findViewById(R.id.chat_room_title);
        status = findViewById(R.id.chat_room_status);

        cardAttachmentPreview = findViewById(R.id.card_attachment_preview);
        imgAttachmentThumb = findViewById(R.id.img_attachment_thumb);
        txtAttachmentName = findViewById(R.id.txt_attachment_name);

        title.setText(getIntent().getStringExtra("otherName") == null ? "Chat" : getIntent().getStringExtra("otherName"));
        status.setText("Offline");
        status.setTextColor(Color.parseColor("#64748B"));

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_chat_gallery).setOnClickListener(v -> pickImage());

        View btnRemoveAttachment = findViewById(R.id.btn_remove_attachment);
        if (btnRemoveAttachment != null) {
            btnRemoveAttachment.setOnClickListener(v -> clearPendingAttachment());
        }
    }

    private void clearPendingAttachment() {
        pendingImageUri = null;
        pendingFileName = "";
        if (cardAttachmentPreview != null) {
            cardAttachmentPreview.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceUtil.online();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PresenceUtil.offline();
    }

    private void openDirectChatWithUser(String targetId) {
        if (myId == null || myId.trim().isEmpty()) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String a = myId.compareTo(targetId) <= 0 ? myId : targetId;
        String b = myId.compareTo(targetId) <= 0 ? targetId : myId;
        chatId = a + "_" + b;

        db.collection("user").document(myId).get().addOnSuccessListener(me -> {
            db.collection("user").document(targetId).get().addOnSuccessListener(other -> {
                String myRole = safe(me.getString("role"), "client");

                Map<String, Object> data = new HashMap<>();
                data.put("participantIds", Arrays.asList(myId, targetId));
                data.put("updatedAt", FieldValue.serverTimestamp());
                if ("editor".equalsIgnoreCase(myRole)) {
                    data.put("editorId", myId);
                    data.put("editorName", safe(me.getString("nama"), "Editor"));
                    data.put("editorPhoto", safe(me.getString("fotoProfil"), ""));
                    data.put("clientId", targetId);
                    data.put("clientName", safe(other.getString("nama"), "Client"));
                    data.put("clientPhoto", safe(other.getString("fotoProfil"), ""));
                } else {
                    data.put("clientId", myId);
                    data.put("clientName", safe(me.getString("nama"), "Client"));
                    data.put("clientPhoto", safe(me.getString("fotoProfil"), ""));
                    data.put("editorId", targetId);
                    data.put("editorName", safe(other.getString("nama"), "Editor"));
                    data.put("editorPhoto", safe(other.getString("fotoProfil"), ""));
                }
                data.put("lastMessage", "");
                db.collection("chats").document(chatId).set(data, SetOptions.merge())
                        .addOnSuccessListener(x -> {
                            loadChatInfo();
                            loadMessages();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Gagal membuka chat: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal membuka chat: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadChatInfo() {
        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            String clientId = doc.getString("clientId");
            String editorId = doc.getString("editorId");
            otherId = getOtherId(doc, clientId, editorId);
            String fallbackName = myId.equals(clientId) ? safe(doc.getString("editorName"), "Editor") : safe(doc.getString("clientName"), "Client");
            String fallbackPhoto = myId.equals(clientId) ? safe(doc.getString("editorPhoto"), "") : safe(doc.getString("clientPhoto"), "");

            markChatRead();
            title.setText(fallbackName);
            if (!fallbackPhoto.isEmpty()) {
                ImageLoader.load(imgOther, fallbackPhoto);
            }

            if (!otherId.isEmpty()) {
                db.collection("user").document(otherId).get().addOnSuccessListener(user -> {
                    if (user != null && user.exists()) {
                        String nama = safe(user.getString("nama"), fallbackName);
                        String foto = safe(user.getString("fotoProfil"), fallbackPhoto);
                        title.setText(nama);
                        listenPresence(otherId);
                        if (!foto.isEmpty()) ImageLoader.load(imgOther, foto);
                    }
                });
            }
        });
    }

    private void listenPresence(String uid) {
        if (uid == null || uid.trim().isEmpty() || uid.equals(myId)) {
            status.setText("Offline");
            status.setTextColor(Color.parseColor("#64748B"));
            return;
        }
        FirebaseDatabase.getInstance().getReference("status").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String state = snapshot.child("state").getValue(String.class);
                        Long last = snapshot.child("lastChanged").getValue(Long.class);
                        if (PresenceUtil.isFreshOnline(state, last)) {
                            status.setText("Online");
                            status.setTextColor(Color.parseColor("#22C55E"));
                        } else {
                            status.setText(PresenceUtil.lastSeenText(last));
                            status.setTextColor(Color.parseColor("#64748B"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        status.setText("Offline");
                    }
                });
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

    private void markChatRead() {
        if (chatId == null || myId == null || myId.trim().isEmpty()) return;
        Map<String, Object> read = new HashMap<>();
        read.put("unread_" + myId, 0);
        read.put("lastRead_" + myId, FieldValue.serverTimestamp());
        db.collection("chats").document(chatId).set(read, SetOptions.merge());
    }

    private void loadMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    messageContainer.removeAllViews();
                    if (e != null) {
                        messageContainer.addView(messageText("Gagal memuat pesan: " + e.getMessage(), false, ""));
                        return;
                    }
                    if (snap == null || snap.isEmpty()) {
                        View empty = messageText("Belum ada pesan. Mulai percakapan sekarang.", false, "");
                        messageContainer.addView(empty);
                        return;
                    }
                    markChatRead();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String text = doc.getString("message");
                        String senderId = doc.getString("senderId");
                        String time = "";
                        Date d = doc.getTimestamp("createdAt") == null ? null : doc.getTimestamp("createdAt").toDate();
                        if (d != null) time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d);

                        String type = safe(doc.getString("type"), "text");
                        String imageUrl = doc.getString("imageUrl");
                        String base64 = doc.getString("imageBase64");

                        boolean isMine = myId.equals(senderId);

                        if ("image".equals(type) || (imageUrl != null && !imageUrl.isEmpty())) {
                            messageContainer.addView(messageImage(imageUrl, base64, isMine, time, text));
                        } else {
                            messageContainer.addView(messageText(text, isMine, time));
                        }
                    }
                    scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
                });
    }

    private void sendMessage() {
        String msg = input.getText().toString().trim();

        if (pendingImageUri != null) {
            // Upload pending image with caption text
            uploadAndSendMessageWithImage(pendingImageUri, msg);
            return;
        }

        if (msg.isEmpty()) return;

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", myId);
        data.put("senderEmail", auth.getCurrentUser().getEmail() == null ? "" : auth.getCurrentUser().getEmail());
        data.put("message", msg);
        data.put("type", "text");
        data.put("createdAt", FieldValue.serverTimestamp());

        input.setText("");

        db.collection("chats").document(chatId).collection("messages").add(data)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("lastMessage", msg);
                    upd.put("lastSenderId", myId);
                    upd.put("updatedAt", FieldValue.serverTimestamp());
                    if (otherId != null && !otherId.trim().isEmpty() && !otherId.equals(myId)) {
                        upd.put("unread_" + otherId, FieldValue.increment(1));
                    }
                    upd.put("unread_" + myId, 0);
                    db.collection("chats").document(chatId).set(upd, SetOptions.merge());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal kirim: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadAndSendMessageWithImage(Uri uri, String caption) {
        if (auth.getCurrentUser() == null || uri == null) return;

        clearPendingAttachment();
        input.setText("");

        // 1. Instant 0.1s Base64 sending
        String base64 = imageUriToBase64(uri);

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", myId);
        data.put("senderEmail", auth.getCurrentUser().getEmail() == null ? "" : auth.getCurrentUser().getEmail());
        data.put("type", "image");
        data.put("message", caption.isEmpty() ? "[Gambar]" : caption);
        data.put("imageBase64", base64);
        data.put("imageUrl", "");
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).collection("messages").add(data).addOnSuccessListener(ref -> {
            Map<String, Object> upd = new HashMap<>();
            upd.put("lastMessage", caption.isEmpty() ? "📷 Gambar" : "📷 " + caption);
            upd.put("lastSenderId", myId);
            upd.put("updatedAt", FieldValue.serverTimestamp());
            if (otherId != null && !otherId.trim().isEmpty() && !otherId.equals(myId)) {
                upd.put("unread_" + otherId, FieldValue.increment(1));
            }
            upd.put("unread_" + myId, 0);
            db.collection("chats").document(chatId).set(upd, SetOptions.merge());

            // 2. Cloudinary high-res background upload
            if (CloudinaryUtil.isConfigured(this)) {
                CloudinaryUtil.uploadUri(this, uri, "mediamatch/chat", new CloudinaryUtil.Callback() {
                    @Override
                    public void onSuccess(String url, String publicId, String name, String mime) {
                        Map<String, Object> imgUpd = new HashMap<>();
                        imgUpd.put("imageUrl", url);
                        ref.update(imgUpd);
                    }

                    @Override
                    public void onError(String message) {}
                });
            }
        });
    }

    private String imageUriToBase64(Uri uri) {
        try {
            java.io.InputStream input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (input != null) input.close();
            if (bitmap == null) return "";

            int maxSize = 420;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                bitmap = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(width * ratio)), Math.max(1, Math.round(height * ratio)), true);
            }

            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 55, output);
            byte[] bytes = output.toByteArray();
            return "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == REQ_PICK_IMAGE) {
            pendingImageUri = data.getData();
            pendingFileName = getFileName(pendingImageUri);

            if (imgAttachmentThumb != null) {
                imgAttachmentThumb.setImageURI(pendingImageUri);
            }
            if (txtAttachmentName != null) {
                txtAttachmentName.setText(pendingFileName);
            }
            if (cardAttachmentPreview != null) {
                cardAttachmentPreview.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "Gambar dipilih";
    }

    private View messageText(String msg, boolean mine, String time) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(mine ? 3 : 1));
        card.setStrokeColor(mine ? Color.parseColor("#5D63FF") : getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(mine ? 0 : 1));
        card.setCardBackgroundColor(mine ? Color.parseColor("#5D63FF") : getResources().getColor(R.color.cardSurface));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.addView(box);

        TextView tv = new TextView(this);
        tv.setText(msg == null ? "" : msg);
        tv.setTextSize(14);
        tv.setTextColor(mine ? Color.WHITE : getResources().getColor(R.color.textPrimary));
        tv.setLineSpacing(dp(3), 1.1f);
        box.addView(tv);

        if (!time.isEmpty()) {
            TextView t = new TextView(this);
            t.setText(time);
            t.setTextSize(10);
            t.setTextColor(mine ? Color.parseColor("#EEEEFF") : getResources().getColor(R.color.textMuted));
            t.setGravity(Gravity.END);
            t.setPadding(0, dp(4), 0, 0);
            box.addView(t);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(mine ? dp(60) : 0, 0, mine ? 0 : dp(60), dp(12));
        lp.gravity = mine ? Gravity.END : Gravity.START;
        card.setLayoutParams(lp);

        return card;
    }

    private View messageImage(String imageUrl, String base64, boolean mine, String time, String caption) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(mine ? 3 : 1));
        card.setStrokeColor(mine ? Color.parseColor("#5D63FF") : getResources().getColor(R.color.borderLight));
        card.setStrokeWidth(dp(mine ? 0 : 1));
        card.setCardBackgroundColor(mine ? Color.parseColor("#5D63FF") : getResources().getColor(R.color.cardSurface));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(6), dp(6), dp(8));
        card.addView(box);

        // Image Container - Full Aspect Ratio WhatsApp Style
        MaterialCardView imgCard = new MaterialCardView(this);
        imgCard.setRadius(dp(14));
        imgCard.setCardElevation(dp(0));
        imgCard.setStrokeWidth(dp(0));

        ImageView img = new ImageView(this);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setMaxHeight(dp(360));
        img.setMinimumHeight(dp(160));
        img.setLayoutParams(new LinearLayout.LayoutParams(dp(250), -2));

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ImageLoader.load(img, imageUrl.trim());
            card.setOnClickListener(v -> showFullscreenMediaDialog(imageUrl.trim()));
        } else {
            Bitmap bmp = FileBase64Util.bitmapFromBase64(base64);
            if (bmp != null) img.setImageBitmap(bmp);
        }

        imgCard.addView(img);
        box.addView(imgCard);

        // Caption Text (if present and not default "[Gambar]")
        if (caption != null && !caption.isEmpty() && !"[Gambar]".equals(caption)) {
            TextView capTv = new TextView(this);
            capTv.setText(caption);
            capTv.setTextSize(14);
            capTv.setTextColor(mine ? Color.WHITE : getResources().getColor(R.color.textPrimary));
            capTv.setLineSpacing(dp(2), 1.1f);
            capTv.setPadding(dp(8), dp(8), dp(8), 0);
            box.addView(capTv);
        }

        // Time stamp
        if (!time.isEmpty()) {
            TextView t = new TextView(this);
            t.setText(time);
            t.setTextSize(10);
            t.setTextColor(mine ? Color.parseColor("#EEEEFF") : getResources().getColor(R.color.textMuted));
            t.setGravity(Gravity.END);
            t.setPadding(0, dp(4), dp(6), 0);
            box.addView(t);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(mine ? dp(50) : 0, 0, mine ? 0 : dp(50), dp(12));
        lp.gravity = mine ? Gravity.END : Gravity.START;
        card.setLayoutParams(lp);

        return card;
    }

    private void showFullscreenMediaDialog(String url) {
        if (url == null || url.trim().isEmpty() || isFinishing()) return;

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#0A0F1D"));

        // Main Fullscreen Image View
        ImageView img = new ImageView(this);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imgLp.gravity = Gravity.CENTER;
        img.setLayoutParams(imgLp);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setPadding(dp(16), dp(80), dp(16), dp(16));
        ImageLoader.load(img, url.trim());
        root.addView(img);

        // Top Header Bar Overlay
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), dp(48), dp(16), dp(14));
        topBar.setBackgroundColor(Color.parseColor("#B30A0F1D"));
        topBar.setClipChildren(false);
        topBar.setClipToPadding(false);

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        root.addView(topBar, topLp);

        // Back Circle Button
        ImageView btnClose = new ImageView(this);
        btnClose.setImageResource(R.drawable.ic_back_ios);
        btnClose.setColorFilter(Color.parseColor("#5D63FF"));
        btnClose.setPadding(dp(10), dp(10), dp(10), dp(10));
        btnClose.setBackgroundResource(R.drawable.circle_white_elevated);
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        topBar.addView(btnClose, closeLp);

        TextView titleTv = new TextView(this);
        titleTv.setText("Pratinjau Foto");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(17);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(dp(14), 0, 0, 0);
        topBar.addView(titleTv, titleLp);

        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        img.setOnClickListener(v -> dialog.dismiss());
        root.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static String safe(String v) { return v == null ? "" : v; }
    private static String safe(String v, String f) { return v == null || v.trim().isEmpty() ? f : v; }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
