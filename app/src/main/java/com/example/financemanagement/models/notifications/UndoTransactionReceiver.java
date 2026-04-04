package com.example.financemanagement.models.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UndoTransactionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String transactionId = intent.getStringExtra("transactionId");
        int notificationId = intent.getIntExtra("notificationId", -1);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (transactionId != null)
            db.collection("Users")
                    .document(userId)
                    .collection("Transactions")
                    .document(transactionId)
                    .delete();

        if (notificationId != -1)
            NotificationManagerCompat.from(context).cancel(notificationId);
    }
}