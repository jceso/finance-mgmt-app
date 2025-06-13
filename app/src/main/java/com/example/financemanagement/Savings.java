package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.Objects;

public class Savings extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private TextView amount, perc, savings_amount, fixed_income;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_savings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());
        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();


        amount = findViewById(R.id.amount);
        perc = findViewById(R.id.perc);
        savings_amount = findViewById(R.id.savings_amount);
        fixed_income = findViewById(R.id.fixed_income);
        DocumentReference userDocRef = fStore.collection("Users").document(user.getUid());

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");
                if (balances != null) {
                    Map<String, Object> fixedIncome = (Map<String, Object>) balances.get("fixed_income");

                    if (fixedIncome != null) {
                        Log.d("Savings", "Fixed Income: " + fixedIncome);

                        Long savePercLong = (Long) fixedIncome.get("save_perc");
                        Double valueMonthlyDouble = (Double) fixedIncome.get("value_monthly");

                        int savePerc = savePercLong != null ? savePercLong.intValue() : 0;
                        float valueMonthly = valueMonthlyDouble != null ? valueMonthlyDouble.floatValue() : 0f;
                        Log.d("Savings", "save_perc: " + savePerc + " | value_monthly: " + valueMonthly);
                        String percText = savePerc + "%";
                        String savingsAmountText = "Your savings should be ";

                        perc.setText(percText);
                        savings_amount.setText(String.format("It should be €%s", String.format("%.2f", (savePerc / 100f) * valueMonthly)));
                        fixed_income.setText(String.format("Because fixed income is €%s", String.format("%.2f", valueMonthly)));
                    } else
                        Log.d("Savings", "Campo 'fixed_income' non trovato in 'Balances'");
                } else
                    Log.d("Savings", "Campo 'Balances' non trovato");
            } else
                Log.d("Savings", "Documento utente non trovato");
        }).addOnFailureListener(e -> Log.e("Savings", "Errore nel recupero del documento", e));


        Log.d("Savings", "Savings activity after FireStore call");
    }
}