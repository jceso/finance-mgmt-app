package com.example.financemanagement.models;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.financemanagement.R;
import com.example.financemanagement.models.notifications.UndoTransactionReceiver;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TransactionWorker extends Worker {
    public TransactionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference userRef = db.collection("Users").document(userId);
            QuerySnapshot snapshot = Tasks.await(userRef.collection("RepeatedTransactions").get());

            for (DocumentSnapshot doc : snapshot) {
                Transaction t = doc.toObject(Transaction.class);

                if (shouldExecute(t)) {
                    Transaction newTransaction = new Transaction();
                    newTransaction.setType(t.getType());
                    newTransaction.setAmount(t.getAmount());
                    newTransaction.setCategory(t.getCategory());
                    newTransaction.setNote(t.getNote());
                    newTransaction.setMethod(t.getMethod());
                    newTransaction.setDate();
                    newTransaction.setFrequency(t.getFrequency());
                    // Salva transazione e aggiorna ultima esecuzione
                    DocumentReference ref = Tasks.await(userRef.collection("Transactions").add(newTransaction));
                    Tasks.await(doc.getReference().update("lastExecuted", System.currentTimeMillis()));

                    sendNotification(t, ref.getId());
                }
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private boolean shouldExecute(Transaction t) {
        LocalDateTime last = Instant.ofEpochMilli(t.getLastExecuted())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
        LocalDateTime next;
        switch (t.getFrequency()) {
            case "Daily": next = last.plusDays(1); break;
            case "Weekly": next = last.plusWeeks(1); break;
            case "Monthly": next = last.plusMonths(1); break;
            case "Yearly": next = last.plusYears(1); break;
            default: return false;
        }

        return !LocalDateTime.now().isBefore(next);
    }

    private void sendNotification(Transaction t, String transactionId) {
        Context context = getApplicationContext();
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        // Crea channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channel_id",
                    "Transazioni automatiche",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Intent per annullare
        int notificationId = new Random().nextInt();
        Intent undoIntent = new Intent(context, UndoTransactionReceiver.class);
        undoIntent.putExtra("transactionId", transactionId);
        undoIntent.putExtra("notificationId", notificationId);

        PendingIntent undoPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                undoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "channel_id")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Transazione automatica")
                        .setContentText("Aggiunti " + t.getAmount())
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .addAction(R.drawable.ic_launcher_foreground, "Annulla", undoPendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return;     // niente permesso → niente notifica
        }

        manager.notify(notificationId, builder.build());
    }
}
