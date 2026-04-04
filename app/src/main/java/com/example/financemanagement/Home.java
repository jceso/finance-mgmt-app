package com.example.financemanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.Category;
import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.charts.ChartPagerAdapter;
import com.example.financemanagement.models.notifications.PaymentNotificationListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.Manifest;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import me.relex.circleindicator.CircleIndicator3;

public class Home extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private DocumentReference userRef;
    private float monthlyVarInc = 0.0F, monthlyVarExp = 0.0F, incTotal = 0.0F;

    @Override
    protected void onResume() {
        super.onResume();

        if (!isNotificationServiceEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permesso richiesto")
                    .setMessage("L'app ha bisogno dell'accesso alle notifiche per funzionare.")
                    .setPositiveButton("Abilita", (dialog, which) -> {
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        } else {
            // Forza reconnect se non già fatto
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean alreadyFixed = prefs.getBoolean("listener_fixed", false);
            if (!alreadyFixed) {
                forceReconnect();
                prefs.edit().putBoolean("listener_fixed", true).apply();
            }
        }

        // Controlla permesso notifiche (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
            "transactions",                        // id
                "Transazioni",                        // nome visibile all'utente
                NotificationManager.IMPORTANCE_HIGH); // priorità visibile
            channel.setDescription("Notifiche per le transazioni rilevate");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

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

        // Risolve listener notifiche
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean alreadyFixed = prefs.getBoolean("listener_fixed", false);
        if (isNotificationServiceEnabled() && !alreadyFixed) {
            forceReconnect();
            prefs.edit().putBoolean("listener_fixed", true).apply();
        }

        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Redirect to login or handle unauthorized access
            Log.e("Home", "User is not logged in!");
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        }
        userRef = fStore.collection("Users").document(user.getUid());

        moneySavingsRetrieving();
        loadCategoriesForAddTransactions();
        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackExit(this, this, getOnBackPressedDispatcher());

        Log.d("Home", "Prima di viewpageradapter");
        // Initialize the ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager != null) {
            Log.d("Home","ViewPager2 is not null and is going to ChartPagerAdapter");
            ChartPagerAdapter adapter = new ChartPagerAdapter(this, true);
            viewPager.setAdapter(adapter);
        }
        // Initialize the CircleIndicator3
        CircleIndicator3 circleIndicator = findViewById(R.id.circle_indicator);
        if (circleIndicator != null)
            circleIndicator.setViewPager(viewPager);

        // Cibo, Casa, Sport, Benessere, Vestiti, Trasporti, Abbonamenti, Cibo fuori casa, Svago
        // Da mantenere tra i preferiti Cibo-CiboFuoriCasa-Svago

        // 55% Fixed expenses - House, Food, Transport
        // 30% Variable expenses - Entertainment, Out of home food
        // 15% Savings or Emergencies

        ImageView avatar = findViewById(R.id.avatar);
        ImageView settings = findViewById(R.id.settings);

        CommonFeatures.checkUserAndSetNameButton(Home.this, avatar);
        settings.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Settings.class));
            finish();
        });
    }

    private void moneySavingsRetrieving() {
        //Money settings
        OpeningBalances balances = new OpeningBalances();

        balances.cardBalance = getIntent().getDoubleExtra("CARD_BALANCE", 0f);
        balances.cashBalance = getIntent().getDoubleExtra("CASH_BALANCE", 0f);
        balances.salaryBalance = getIntent().getDoubleExtra("SALARY_BALANCE", 0);
        balances.loanBalance = getIntent().getDoubleExtra("LOAN_BALANCE", 0);

        //Savings settings
        balances.fixedIncome = getIntent().getDoubleExtra("FIXED_INCOME", 0);
        balances.savePerc = getIntent().getFloatExtra("SAVE_PERC", 0);

        if (balances.cardBalance == 0 || balances.cashBalance == 0 || balances.salaryBalance == 0 || balances.loanBalance == 0 || balances.fixedIncome == 0 || balances.savePerc == 0) {
            Log.d("Home", "Devo ricalcolare i vari dati");
            moneyResearching().addOnSuccessListener(newNumbers -> {
                    balances.cardBalance = newNumbers.cardBalance;
                    balances.cashBalance = newNumbers.cashBalance;
                    balances.salaryBalance = newNumbers.salaryBalance;
                    balances.loanBalance = newNumbers.loanBalance;
                    balances.fixedIncome = newNumbers.fixedIncome;
                    balances.savePerc = newNumbers.savePerc;

                    TextClickSettings(balances);
                    savingsSetting(balances.fixedIncome, balances.savePerc);
                }).addOnFailureListener(e -> Log.e("Money","Errore nel recupero dati", e));
        } else {
            TextClickSettings(balances);
            savingsSetting(balances.fixedIncome, balances.savePerc);
        }
    }

    private void savingsSetting(double fixedIncome, float savePerc) {
        ImageView pigImage = findViewById(R.id.summary);

        // 1. Fetch all the transactions
        userRef.collection("Transactions").get().addOnSuccessListener(queryDocumentSnapshots -> {
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

            // 2. Calculate totals
            incTotal = (float) (fixedIncome + monthlyVarInc);
            float currSavings = ((savePerc /100) * incTotal);
            float maxCurExpenses = incTotal - currSavings;
            //Log.d("HomeSavings", "Totale guadagni variabili: " + monthlyVarInc + " | Risparmio fisso: " + fixedIncome + " | Risparmio netto: " + incTotal);
            //Log.d("HomeSavings", "Savings: " + savePerc + "% | You can spend up to " + maxCurExpenses + " euro");

            // 3. Modify pig image
            if (monthlyVarExp > maxCurExpenses) {
                //Log.d("HomeSavings", "You wasted money, you broke the pig :(\n  This month you had " + incTotal + " of income, but you spent " + monthlyVarExp + "\n  You still have " + (incTotal-monthlyVarExp) + " compared to the savings theory " + currSavings);
                pigImage.setBackgroundResource(R.drawable.pig_mood_sad);
                pigImage.setImageResource(R.drawable.pig_sad_original);
            } else {
                //Log.d("HomeSavings", "You saved money, the pig is safe :)\n  This month you had " + incTotal + " of income, but you spent " + monthlyVarExp + "\n  You still have " + (incTotal-monthlyVarExp) + " compared to the savings theory " + currSavings);
                pigImage.setBackgroundResource(R.drawable.pig_mood_happy);
                pigImage.setImageResource(R.drawable.pig_happy);
            }
        }).addOnFailureListener(e -> Log.e("Savings", "Error retrieving transactions", e));

        pigImage.setOnClickListener(v -> {
            Intent intentSvg = new Intent(Home.this, Savings.class);
            intentSvg.putExtra("curr_incomes", incTotal);
            intentSvg.putExtra("curr_expenses", monthlyVarExp);
            startActivity(intentSvg);
            finish();
        });
    }

    private void TextClickSettings(OpeningBalances balances) {
        TextView cardMoney = findViewById(R.id.card_money);
        TextView cashMoney = findViewById(R.id.cash_money);
        TextView loanMoney = findViewById(R.id.loan_money);
        TextView salaryMoney = findViewById(R.id.salary_money);

        cardMoney.setText(CommonFeatures.formatAmount(balances.cardBalance));
        cashMoney.setText(CommonFeatures.formatAmount(balances.cashBalance));
        salaryMoney.setText(String.format(Locale.getDefault(), "€ %d", (int) balances.salaryBalance));
        loanMoney.setText(String.format(Locale.getDefault(), "€ %d", (int) balances.loanBalance));

        cardMoney.setOnClickListener(v -> {
            Intent intentCc = new Intent(Home.this, TransactionsShow.class);
            intentCc.putExtra("type", "credit_card");
            intentCc.putExtra("money", String.format(Locale.getDefault(), "€%.2f", balances.cardBalance));
            startActivity(intentCc);
            finish();
        });
        cashMoney.setOnClickListener(v -> {
            Intent intentCash = new Intent(Home.this, TransactionsShow.class);
            intentCash.putExtra("type", "cash");
            intentCash.putExtra("money", String.format(Locale.getDefault(), "€%.2f", balances.cashBalance));
            startActivity(intentCash);
            finish();
        });
    }

    private Task<OpeningBalances> moneyResearching() {
        DocumentReference userRef = fStore.collection("Users").document(user.getUid());

        return userRef.get().continueWith(task -> {
            OpeningBalances results = new OpeningBalances();

            if (!task.isSuccessful())
                throw task.getException();

            DocumentSnapshot documentSnapshot = task.getResult();
            if (documentSnapshot != null && documentSnapshot.exists()) {
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");
                if (balances != null) {
                    Map<String, Object> creditCard = (Map<String, Object>) balances.get("credit_card");
                    if (creditCard != null)
                        results.cardBalance = ((Number) creditCard.get("value")).doubleValue();

                    Map<String, Object> cash = (Map<String, Object>) balances.get("cash");
                    if (cash != null)
                        results.cashBalance = ((Number) cash.get("value")).doubleValue();

                    results.fixedIncome = ((Number) balances.get("fixed_income")).doubleValue();
                    results.savePerc = ((Number) balances.get("save_perc")).floatValue();
                }

                Map<String, Object> incomes = (Map<String, Object>) documentSnapshot.get("Categories");
                if (incomes != null) {
                    Map<String, Object> salary = (Map<String, Object>) incomes.get("salary");
                    if (salary != null)
                        results.salaryBalance = ((Number) salary.get("sum")).doubleValue();

                    Map<String, Object> loan = (Map<String, Object>) incomes.get("loan");
                    if (loan != null)
                        results.loanBalance = ((Number) loan.get("sum")).doubleValue();
                }
            }

            return results;
        });
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");

        return flat != null && flat.contains(pkgName);
    }

    private void forceReconnect() {
        ComponentName cn = new ComponentName(this, PaymentNotificationListener.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void loadCategoriesForAddTransactions() {
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

                    Button incomeBtn = findViewById(R.id.income);
                    Button expensesBtn = findViewById(R.id.expenses);
                    Intent intent = new Intent(Home.this, AddTransaction.class);
                    intent.putParcelableArrayListExtra("CATEGORIES", new ArrayList<>(allCategories));

                    incomeBtn.setOnClickListener(v -> {
                        intent.putExtra("TAB_INDEX", 0); // Prima pagina (Income)
                        startActivity(intent);
                        finish();
                    });
                    expensesBtn.setOnClickListener(view -> {
                        intent.putExtra("TAB_INDEX", 1); // Seconda pagina (Expenses)
                        startActivity(intent);
                        finish();
                    });
                }
            }
        }).addOnFailureListener(e -> Log.e("Home", "Error in retrieving categories", e));
    }
}