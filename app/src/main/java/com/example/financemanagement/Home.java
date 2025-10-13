package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.charts.ChartPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import me.relex.circleindicator.CircleIndicator3;

public class Home extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private TextView cardMoney, cashMoney;
    private double cardBalance = 0L, cashBalance = 0L;
    private float monthlyVarExp = 0, incTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackExit(this, this, getOnBackPressedDispatcher());


        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // Redirect to login or handle unauthorized access
            Log.e("Home", "User is not logged in!");
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        }


        // Initialize the ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager == null)
            Log.e("MainActivity", "ViewPager2 is null. Check the layout.");
        else {
            ChartPagerAdapter adapter = new ChartPagerAdapter(this, true);
            viewPager.setAdapter(adapter);
        }
        // Initialize the CircleIndicator3
        CircleIndicator3 circleIndicator = findViewById(R.id.circle_indicator);
        if (circleIndicator == null)
            Log.e("MainActivity", "CircleIndicator3 is null. Check the layout.");
        else
            circleIndicator.setViewPager(viewPager);

        moneySetting();
        savingsSetting();

        // Cibo, Casa, Sport, Benessere, Vestiti, Trasporti, Abbonamenti, Cibo fuori casa, Svago
        // Da mantenere tra i preferiti Cibo-CiboFuoriCasa-Svago

        // 55% Fixed expenses - House, Food, Transport
        // 30% Variable expenses - Entertainment, Out of home food
        // 15% Savings or Emergencies

        ImageView avatar = findViewById(R.id.avatar);
        Log.d("Avatar", "Avatar: " + avatar);
        ImageView settings = findViewById(R.id.settings);
        // CommonFeatures.checkUserAndSetNameButton(Home.this, avatar);

        ImageView pig = findViewById(R.id.summary);
        Button incomeBtn = findViewById(R.id.income);
        Button expensesBtn = findViewById(R.id.expenses);

        pig.setOnClickListener(v -> {
            Intent intentSvg = new Intent(Home.this, Savings.class);
            intentSvg.putExtra("curr_incomes", incTotal);
            intentSvg.putExtra("curr_expenses", monthlyVarExp);
            startActivity(intentSvg);
            finish();
        });

        cardMoney = findViewById(R.id.card_money);
        cardMoney.setOnClickListener(v -> {
            Intent intentCc = new Intent(Home.this, TransactionsShow.class);
            intentCc.putExtra("type", "credit_card");
            intentCc.putExtra("money", String.format(Locale.getDefault(), "€%.2f", cardBalance));
            startActivity(intentCc);
            finish();
        });

        cashMoney = findViewById(R.id.cash_money);
        cashMoney.setOnClickListener(v -> {
            Intent intentCash = new Intent(Home.this, TransactionsShow.class);
            intentCash.putExtra("type", "cash");
            intentCash.putExtra("money", String.format(Locale.getDefault(), "€%.2f", cashBalance));
            startActivity(intentCash);
            finish();
        });

        settings.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Settings.class));
            finish();
        });

        incomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 0); // Prima pagina (Income)
            startActivity(intent);
            finish();
        });

        expensesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Home.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 1); // Seconda pagina (Expenses)
            startActivity(intent);
            finish();
        });
    }

    private void moneySetting() {
        TextView loanMoney = findViewById(R.id.loan_money);
        TextView salaryMoney = findViewById(R.id.salary_money);
        DocumentReference userRef = fStore.collection("Users").document(user.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Retrieve balances
                Object balancesObj = documentSnapshot.get("Balances");
                if (balancesObj instanceof Map) {
                    Map<String, Object> balancesMap = (Map<String, Object>) balancesObj;

                    Object creditCardObj = balancesMap.get("credit_card");
                    if (creditCardObj instanceof Map) {
                        Map<String, Object> creditCardMap = (Map<String, Object>) creditCardObj;
                        Object value = creditCardMap.get("value");
                        if (value instanceof Number) {
                            cardBalance = ((Number) value).doubleValue();
                        }
                    }

                    Object cashObj = balancesMap.get("cash");
                    if (cashObj instanceof Map) {
                        Map<String, Object> cashMap = (Map<String, Object>) cashObj;
                        Object value = cashMap.get("value");
                        if (value instanceof Number) {
                            cashBalance = ((Number) value).doubleValue();
                        }
                    }

                    cardMoney.setText(String.format(Locale.getDefault(), "€%d", (int) cardBalance));
                    cashMoney.setText(String.format(Locale.getDefault(), "€%d", (int) cashBalance));
                }

                // Retrieve incomes
                Object incomesObj = documentSnapshot.get("Categories");
                double salaryBalance = 0L, loanBalance = 0L;
                if (incomesObj instanceof Map) {
                    Map<String, Object> incomesMap = (Map<String, Object>) incomesObj;

                    Object salaryObj = incomesMap.get("salary");
                    if (salaryObj instanceof Map) {
                        Map<String, Object> salaryMap = (Map<String, Object>) salaryObj;
                        Object value = salaryMap.get("sum");
                        if (value instanceof Number) {
                            salaryBalance = ((Number) value).doubleValue();
                        }
                    }

                    Object loanObj = incomesMap.get("loan");
                    if (loanObj instanceof Map) {
                        Map<String, Object> loanMap = (Map<String, Object>) loanObj;
                        Object value = loanMap.get("sum");
                        if (value instanceof Number) {
                            loanBalance = ((Number) value).doubleValue();
                        }
                    }

                    Log.d("Home", "Salario: " + salaryBalance + " | Prestito: " + loanBalance);
                    salaryMoney.setText(String.format(Locale.getDefault(), "€ %d", (int) salaryBalance));
                    loanMoney.setText(String.format(Locale.getDefault(), "€ %d", (int) loanBalance));
                }
            }
        }).addOnFailureListener(e -> {
            salaryMoney.setText("Error");
            loanMoney.setText("Error");
        });
    }

    private void savingsSetting() {
        DocumentReference userRef = fStore.collection("Users").document(user.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // 1. Fetch fixed_income and save_perc
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");
                double fixedIncome;
                int savePerc;  // Using int to store percentage as an integer

                if (balances != null) {
                    Object fixedIncomeObj = balances.get("fixed_income");
                    Object savePercObj = balances.get("save_perc");

                    Log.d("HomeSavings", "Fixed income: " + fixedIncomeObj + " | Save perc: " + savePercObj);

                    fixedIncome = ((Number) Objects.requireNonNull(fixedIncomeObj)).doubleValue();
                    savePerc = (int) ((Number) Objects.requireNonNull(savePercObj)).doubleValue();

                    // Log the result as an integer value
                    Log.d("HomeSavings", "Fixed income: " + fixedIncome + " | Save perc: " + savePerc);
                } else {
                    Log.d("HomeSavings", "Fixed income not found!");
                    savePerc = 10;
                    fixedIncome = 0;
                }

                // 2. Fetch all the transactions
                userRef.collection("Transactions").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    float monthlyVarInc = 0.0F; // Guadagno variabile mensile
                    monthlyVarExp = 0.0F; // Spesa variabile mensile

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String type = doc.getString("type");
                        String frequency = doc.getString("frequency");
                        Double amount = doc.getDouble("amount");

                        if ("once".equals(frequency) && type != null && amount != null) {
                            if ("income".equals(type))
                                monthlyVarInc += amount;
                            else
                                monthlyVarExp += amount;
                        }
                    }

                    // 3. Calculate totals
                    incTotal = (float) (fixedIncome + monthlyVarInc);
                    float currSavings = (float) (((double) savePerc /100)*incTotal);
                    float maxCurExpenses = incTotal - currSavings;
                    Log.d("HomeSavings", "Totale guadagni variabili: " + monthlyVarInc + " | Risparmio fisso: " + fixedIncome + " | Risparmio netto: " + incTotal);
                    Log.d("HomeSavings", "Savings: " + savePerc + "% | You can spend up to " + maxCurExpenses + " euro");

                    // 4. Modify pig image
                    ImageView pigImage = findViewById(R.id.summary);
                    if (monthlyVarExp > maxCurExpenses) {
                        Log.d("HomeSavings", "You wasted money, you broke the pig :(\n  This month you had " + incTotal + " of income, but you spent " + monthlyVarExp + "\n  You still have " + (incTotal-monthlyVarExp) + " compared to the savings theory " + currSavings);

                        pigImage.setBackgroundResource(R.drawable.pig_mood_sad);
                        pigImage.setImageResource(R.drawable.pig_sad_original);
                    } else {
                        Log.d("HomeSavings", "You saved money, the pig is safe :)\n  This month you had " + incTotal + " of income, but you spent " + monthlyVarExp + "\n  You still have " + (incTotal-monthlyVarExp) + " compared to the savings theory " + currSavings);

                        pigImage.setBackgroundResource(R.drawable.pig_mood_happy);
                        pigImage.setImageResource(R.drawable.pig_happy);
                    }
                }).addOnFailureListener(e -> Log.e("Savings", "Error retrieving transactions", e));
            }
        }).addOnFailureListener(e -> Log.e("Savings", "Error retrieving user data", e));
    }
}