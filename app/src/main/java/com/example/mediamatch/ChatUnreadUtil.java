package com.example.mediamatch;

import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChatUnreadUtil {
    public static void listenUnreadBadge(TextView navChat, boolean active) {
        if (navChat == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    
                    long total = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        Object v = d.get("unread_" + uid);
                        if (v instanceof Number) total += ((Number) v).longValue();
                    }

                    // NavUtil is the only owner of the tab's tint and active state. Updating it
                    // here raced with the entry animation and could hide the Pesan navigation.
                });
    }
}
