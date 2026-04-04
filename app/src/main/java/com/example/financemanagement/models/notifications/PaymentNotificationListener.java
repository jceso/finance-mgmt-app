package com.example.financemanagement.models.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.financemanagement.AddTransaction;
import com.example.financemanagement.R;
import com.example.financemanagement.Splash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentNotificationListener extends NotificationListenerService {
    private static final Map<String, Long> recentTransactions = new HashMap<>();
    private static final long DUPLICATE_WINDOW = 120000; // 60 secondi
    String packageName;
    Pattern amtPattern = Pattern.compile("(?:€|EUR)?\\s*" +                           // valuta opzionale prima
                                        "((?:\\d{1,3}([.,\\s])\\d{3}(?:\\2\\d{3})*|\\d+)" + // numero con o senza migliaia
                                        "(?:[.,]\\d{2})?)" +                                // decimali opzionali
                                        "\\s*(?:€|EUR)?",                                   // valuta opzionale dopo
                                        Pattern.CASE_INSENSITIVE);
    Pattern merchantPattern = Pattern.compile("(?:by|at|da|presso)\\s([\\p{L}0-9\\s]{2,30})");
    Pattern expensePattern = Pattern.compile("\\b(addebito|pagato|acquisto|spesa|bonifico uscito|autorizzato|debit|paid|purchase|spent|payment sent|authorized)\\b", Pattern.CASE_INSENSITIVE);
    Pattern incomePattern = Pattern.compile("\\b(accredito|ricevuto|bonifico in arrivo|rimborso|deposito|credit|received|incoming transfer|refund|deposit)\\b", Pattern.CASE_INSENSITIVE);

    Set<String> SUPPORTED_APPS = new HashSet<>(Arrays.asList(
            // TEST
            "com.mand.notitest",
            // BANKING
            "eu.unicredit.internet.buddybank",          //Buddy UniCredit
            "com.latuabancaperandroid",                 //Intesa Sanpaolo
            "com.unicredit",                            //Unicredit
            "it.ing.banking",                           //ING Italia
            "com.posteitaliane.spim",                   //PosteItaliane
            "it.gruppobper.ams.android.bper",           //BPER Banca
            "it.caitalia.apphub",                       //Credit Agricole
            "com.americanexpress.android.acctsvcs.it",  //American Express Italia
            // E-WALLET / ONLINE PAYMENTS / P2P
            "com.paypal.android.p2pmobile",             //PayPal
            "com.revolut.revolut",                      //Revolut
            "de.number26.android",                      //N26
            "it.hype.app",                              //Hype
            "com.transferwise.android",                 //TransferWise
            "com.imaginecurve.curve.prd",               //Curve
            "com.bunq.android",                         //Bunq
            "com.moneybookers.skrillpayments",          //Skrill
            "com.myklarnamobile",                       // Klarna
            // APP DI PAGAMENTI E WALLET NFC))
            "com.google.android.apps.walletnfcrel",     //Google Wallet
            "com.samsung.android.spay",                 //Samsung Pay
            "com.satispay.customer",                    //Satispay
            "com.sisal.sisalpay",                       //Mooney
            "it.tinaba.app",                            //Tinaba
            "pt.newvision.flowe",                       //Flowe
            //Bizum
            "pt.sibs.android.mbway",                    //MB Way
            // SERVIZI DI NOTIFICHE DI PAGAMENTI E UTILITY
            "it.pagopa.io.app",                     //PagoPA
            "it.enel",                              //Enel
            "com.eni.enigaseluce",                  //Eni Plenitude
            "com.eonenergia.eon",                   //EON
            "it.engie.appengie",                    //Engie
            // OPERTATORI DI TELEFONIA
            "it.telecomitalia.centodiciannove",     //Tim
            "it.vodafone.my190",                    //Vodafone
            "it.wind.myWind",                       //WindTre
            "it.homobile.ho",                       //Ho Mobile
            "it.iliad",                             //Iliad
            "com.effortel.unomobile",               //1Mobile
            "it.fastweb.myfastweb"                  //MyFastweb
        ));

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        packageName = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "");
        CharSequence textChar = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        if (!SUPPORTED_APPS.contains(packageName))
            return;

        String message = (title != null ? title : "") + " " +
                         (textChar != null ? textChar : "") + " " +
                         (bigText != null ? bigText : "");
        ParsedNotification parsed = parseNotification(message);
        if (parsed == null)
            return;
        // Previene la duplicazione
        if (isDuplicate(String.valueOf(parsed.amount)))
            return;

        int notificationId = (int) System.currentTimeMillis();
        Intent editIntent = new Intent(this, Splash.class);
        editIntent.putExtra("notificationId", notificationId);
        editIntent.putExtra("AMOUNT", parsed.amount);
        editIntent.putExtra("MERCHANT", parsed.merchant);
        editIntent.putExtra("IS_INCOME", parsed.isIncome);
        editIntent.putExtra("INSTRUCTION", "notification");
        editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent editPendingIntent = PendingIntent.getActivity(
                                                this,
                                                notificationId + 1,
                                                editIntent,
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, CancelNotificationReceiver.class);
        cancelIntent.putExtra("notificationId", notificationId);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                                                this,
                                                notificationId,
                                                cancelIntent,
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String amountText = String.format(Locale.getDefault(), "%.2f €", parsed.amount);
        NotificationCompat.Builder builder = new NotificationCompat
                                                    .Builder(this, "transactions")
                                                    .setSmallIcon(R.drawable.pig_happy)
                                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                                                    .setContentTitle("Transazione rilevata")
                                                    .setContentText(amountText + " da " + parsed.merchant)
                                                    .setContentIntent(editPendingIntent)
                                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                    .addAction(R.drawable.ic_cancel, "Modifica", editPendingIntent)
                                                    .addAction(R.drawable.ic_cancel, "Annulla", cancelPendingIntent);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private boolean isDuplicate(String amount) {
        long now = System.currentTimeMillis();

        Log.d("Notifica", "Amount: " + amount + " | Now: " + now);
        if (recentTransactions.containsKey(amount)) {
            long lastTime = recentTransactions.get(amount);
            if (now - lastTime < DUPLICATE_WINDOW)
                return true;
        }

        recentTransactions.put(amount, now);
        return false;
    }

    public ParsedNotification parseNotification(String message) {
        if (message == null || message.isEmpty())
            return null;

        // Pulizia simboli corrotti ---
        message = message.replace("â‚¬", "€");
        double amountValue = 0;

        Matcher matcher = amtPattern.matcher(message);
        if (matcher.find()) {
            String raw = matcher.group(1); // SOLO NUMERO
            raw = raw.replace(" ", "");

            // CAPISCI FORMATO
            if (raw.contains(",") && raw.contains("."))
            {
                int lastComma = raw.lastIndexOf(",");
                int lastDot = raw.lastIndexOf(".");

                if (lastComma > lastDot)        // FORMATO EU → 1.750,26
                    raw = raw.replace(".", "").replace(",", ".");
                else                            // FORMATO US → 12,500.26
                    raw = raw.replace(",", "");
            } else if (raw.contains(","))       // Formato EU → 12,34
                raw = raw.replace(".", "").replace(",", ".");
            else if (raw.contains(".")) {       // SOLO PUNTO → capire se decimale US o migliaia
                int lastDot = raw.lastIndexOf(".");
                if (raw.length() - lastDot - 1 != 2)    // MIGLIAIA → 1.700
                    raw = raw.replace(".", "");
            }

            amountValue = Double.parseDouble(raw);
        }

        // Cerca parole dopo by/at/da/presso
        matcher = merchantPattern.matcher(message);
        String merchant = matcher.find() ? matcher.group(1).trim() : "Unknown";

        matcher = expensePattern.matcher(message);
        if (matcher.find())
            return new ParsedNotification(amountValue, merchant, true);
        matcher = incomePattern.matcher(message);
        if (matcher.find())
            return new ParsedNotification(amountValue, merchant, true);

        return new ParsedNotification(amountValue, merchant, false);
    }

    public static class ParsedNotification {
        public double amount;
        public String merchant;
        public boolean isIncome;

        public ParsedNotification(double amount, String merchant, boolean isIncome) {
            this.amount = amount;
            this.merchant = merchant;
            this.isIncome = isIncome;
        }
    }
}