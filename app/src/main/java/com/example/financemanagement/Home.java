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
    private String userId;
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

        if (user != null) {
            userId = user.getUid();
            // Proceed with logic
        } else {
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

        // 55% Spese fisse - Casa, Cibo, Trasporti
        // 30% Spese variabili - Svago, Cibo fuori casa
        // 15% Risparmio o Emergenze

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

        // Retrieve user infos
        fStore.collection("Users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
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
                    } else {
                        cardMoney.setText("€0");
                        cashMoney.setText("€0");
                    }

                    // Retrieve incomes
                }
            }).addOnFailureListener(e -> {
                cardMoney.setText("Error");
                cashMoney.setText("Error");
        });
    }

    private void savingsSetting() {
        DocumentReference userRef = fStore.collection("Users").document(user.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // 1. Fetch fixed_income and save_perc
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");
                double fixedIncome;
                int savePerc;

                if (balances != null && balances.get("fixed_income") instanceof Map) {
                    Log.d("HomeSavings", "Fixed income found! " + balances.get("fixed_income"));
                    Map<String, Object> fixedIncomeMap = (Map<String, Object>) balances.get("fixed_income");

                    Object valueMonthlyObj = Objects.requireNonNull(fixedIncomeMap).get("value_monthly");
                    Object savePercObj = fixedIncomeMap.get("save_perc");

                    if (valueMonthlyObj instanceof Number)
                        fixedIncome = ((Number) valueMonthlyObj).doubleValue();
                    else
                        fixedIncome = 0;
                    if (savePercObj instanceof Number)
                        savePerc = ((Number) savePercObj).intValue();
                    else
                        savePerc = 10;
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
                    Log.d("HomeSavings", "Risparmio del " + savePerc + "% | Si possono spendere " + maxCurExpenses + " euro");

                    // 4. Modify pig image
                    ImageView pigImage = findViewById(R.id.summary);
                    if (monthlyVarExp > maxCurExpenses) {
                        Log.d("HomeSavings", "Hai sprecato soldi, hai rotto il porco :(\n  Questo mese avevi " + incTotal + " di guadagno, ma hai speso " + monthlyVarExp + "\n  Ti restano " + (incTotal-monthlyVarExp) + " rispetto al risparmio teorico di " + currSavings);

                        pigImage.setBackgroundResource(R.drawable.pig_mood_sad);
                        pigImage.setImageResource(R.drawable.pig_sad_original);
                    } else {
                        Log.d("HomeSavings", "Hai risparmiato soldi, il porco è salvo :)\n  Questo mese avevi " + incTotal + " di guadagno e hai speso " + monthlyVarExp + "\n  Ti restano " + (incTotal-monthlyVarExp) + " rispetto al risparmio teorico di " + currSavings);

                        pigImage.setBackgroundResource(R.drawable.pig_mood_happy);
                        pigImage.setImageResource(R.drawable.pig_happy);
                    }
                }).addOnFailureListener(e -> Log.e("Savings", "Errore nel recupero transazioni", e));
            }
        }).addOnFailureListener(e -> Log.e("Savings", "Errore nel recupero dati utente", e));
    }
}