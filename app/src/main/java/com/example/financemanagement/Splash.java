package com.example.financemanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.Category;
import com.example.financemanagement.models.CategoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Splash extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private static DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView logo = findViewById(R.id.logo);
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        logo.startAnimation(rotate);

        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Redirect to login or handle unauthorized access
            Log.e("Splash", "User is not logged in!");
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }
        userRef = fStore.collection("Users").document(user.getUid());

        // CARICAMENTO DATI PRINCIPALI
        String instruction = getIntent().getStringExtra("INSTRUCTION");
        if (instruction == null)
            loadDataAndStartHome();
        else {
            switch (instruction) {
                case "settings": loadDataAndStartSettings();
                                break;
                case "savings": loadDataAndStartSavings();
                                break;
                case "notification": loadDataAndStartAddTransactions();
            }
        }
    }

    private void loadDataAndStartHome() {
        // MONEY SETTING
        final OpeningBalances openingBalances = new OpeningBalances();
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");

                if (balances != null) {
                    Object creditCardObj = balances.get("credit_card");
                    if (creditCardObj instanceof Map) {
                        Map<String, Object> creditCardMap = (Map<String, Object>) creditCardObj;
                        Object value = creditCardMap.get("value");
                        if (value instanceof Number)
                            openingBalances.cardBalance = ((Number) value).doubleValue();
                    }

                    Object cashObj = balances.get("cash");
                    if (cashObj instanceof Map) {
                        Map<String, Object> cashMap = (Map<String, Object>) cashObj;
                        Object value = cashMap.get("value");
                        if (value instanceof Number)
                            openingBalances.cashBalance = ((Number) value).doubleValue();
                    }

                    Object fixedIncomeObj = balances.get("fixed_income");
                    Object savePercObj = balances.get("save_perc");

                    openingBalances.fixedIncome = ((Number) Objects.requireNonNull(fixedIncomeObj)).doubleValue();
                    openingBalances.savePerc = (float) ((Number) Objects.requireNonNull(savePercObj)).doubleValue();
                }

                // Retrieve incomes
                Map<String, Object> incomes = (Map<String, Object>) documentSnapshot.get("Categories");

                if (incomes != null) {
                    Object salaryObj = incomes.get("salary");
                    if (salaryObj instanceof Map) {
                        Map<String, Object> salaryMap = (Map<String, Object>) salaryObj;
                        Object value = salaryMap.get("sum");
                        if (value instanceof Number)
                            openingBalances.salaryBalance = ((Number) value).doubleValue();
                    }

                    Object loanObj = incomes.get("loan");
                    if (loanObj instanceof Map) {
                        Map<String, Object> loanMap = (Map<String, Object>) loanObj;
                        Object value = loanMap.get("sum");
                        if (value instanceof Number)
                            openingBalances.loanBalance = ((Number) value).doubleValue();
                    }
                }

                Intent intent = new Intent(Splash.this, Home.class);
                intent.putExtra("CARD_BALANCE", openingBalances.cardBalance);
                intent.putExtra("CASH_BALANCE", openingBalances.cashBalance);
                intent.putExtra("SALARY_BALANCE", openingBalances.salaryBalance);
                intent.putExtra("LOAN_BALANCE", openingBalances.loanBalance);
                intent.putExtra("FIXED_INCOME", openingBalances.fixedIncome);
                intent.putExtra("SAVE_PERC", openingBalances.savePerc);

                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e -> {
            // Anche in caso di errore, apri comunque la Home
            startActivity(new Intent(Splash.this, MainActivity.class));
            finish();
        });
    }

    private void loadDataAndStartSettings() {
    }

    private void loadDataAndStartSavings() {
    }

    private void loadDataAndStartAddTransactions() {
        double amount = getIntent().getDoubleExtra("AMOUNT", 0);
        String merchant = getIntent().getStringExtra("MERCHANT");
        boolean isIncome = getIntent().getBooleanExtra("IS_INCOME", false);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Map<String, Object>> categoriesMap = (Map<String, Map<String, Object>>) documentSnapshot.get("Categories");

                if (categoriesMap != null) {
                    List<Category> allCategories = new ArrayList<>();

                    for (Map.Entry<String, Map<String, Object>> entry : categoriesMap.entrySet()) {
                        String name = entry.getKey();
                        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                        Map<String, Object> cat = entry.getValue();
                        String type = (String) cat.get("type");
                        String icon = (String) cat.get("icon");

                        allCategories.add(new Category(type, name, icon));
                    }

                    Intent intent = new Intent(Splash.this, AddTransaction.class);
                    intent.putExtra("AMOUNT", amount);
                    intent.putExtra("MERCHANT", merchant);
                    intent.putExtra("TAB_INDEX", isIncome ? 0 : 1);
                    intent.putParcelableArrayListExtra("CATEGORIES", new ArrayList<>(allCategories));

                    startActivity(intent);
                    finish();
                }
            }
        }).addOnFailureListener(e -> Log.e("Splash", "Error in retrieving categories", e));
    }
}

class OpeningBalances {
    double cardBalance = 0;
    double cashBalance = 0;
    double salaryBalance = 0;
    double loanBalance = 0;
    double fixedIncome = 0;
    float savePerc = 0;
}