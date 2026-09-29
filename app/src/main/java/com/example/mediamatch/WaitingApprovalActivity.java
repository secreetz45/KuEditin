package com.example.mediamatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WaitingApprovalActivity extends Activity {
    private ListenerRegistration statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_approval);

        findViewById(R.id.btn_logout_waiting).setOnClickListener(v -> {
            LogoutUtil.logout(this);
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        listenForApproval();
    }

    private void listenForApproval() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        statusListener = FirebaseFirestore.getInstance()
                .collection("user")
                .document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    boolean isApproved = checkEditorApproved(snapshot);
                    String status = snapshot.getString("status");

                    if (isApproved) {
                        Toast.makeText(this, "Selamat! Akun editor Anda telah disetujui HRD.", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(this, EditorHomeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    } else if ("rejected".equalsIgnoreCase(status) || "ditolak".equalsIgnoreCase(status)) {
                        Toast.makeText(this, "Mohon maaf, pendaftaran editor Anda belum disetujui.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean checkEditorApproved(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return false;
        Boolean approvedObj = snapshot.getBoolean("isApproved");
        if (approvedObj == null) approvedObj = snapshot.getBoolean("isVerified");
        if (approvedObj == null) approvedObj = snapshot.getBoolean("isBestEditor");
        if (approvedObj != null && approvedObj) return true;

        String status = snapshot.getString("status");
        if (status == null || status.trim().isEmpty()) return true;

        String s = status.toLowerCase().trim();
        return !"pending_hrd".equals(s) && !"pending".equals(s) && !"rejected".equals(s) && !"ditolak".equals(s) && !"banned".equals(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) {
            statusListener.remove();
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button during waiting
    }
}
