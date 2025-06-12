package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.charts.ChartPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import me.relex.circleindicator.CircleIndicator3;

public class Home extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private String userId;
    private TextView cardMoney, cashMoney;
    private long lastBackPressedTime = 0;
    private Toast backToast;

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

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
            ChartPagerAdapter adapter = new ChartPagerAdapter(this, false);
            viewPager.setAdapter(adapter);
        }
        // Initialize the CircleIndicator3
        CircleIndicator3 circleIndicator = findViewById(R.id.circle_indicator);
        if (circleIndicator == null)
            Log.e("MainActivity", "CircleIndicator3 is null. Check the layout.");
        else
            circleIndicator.setViewPager(viewPager);

        moneySetting();

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
            startActivity(new Intent(Home.this, Savings.class));
            finish();
        });

        cardMoney = findViewById(R.id.card_money);
        cardMoney.setOnClickListener(v -> {
            Intent intentCc = new Intent(Home.this, TransactionsShow.class);
            intentCc.putExtra("type", "credit_card");
            startActivity(intentCc);
            finish();
        });

        cashMoney = findViewById(R.id.cash_money);
        cashMoney.setOnClickListener(v -> {
            Intent intentCash = new Intent(Home.this, TransactionsShow.class);
            intentCash.putExtra("type", "cash");
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
                        long cardBalance = 0L;
                        long cashBalance = 0L;

                        Object creditCardObj = balancesMap.get("credit_card");
                        if (creditCardObj instanceof Map) {
                            Map<String, Object> creditCardMap = (Map<String, Object>) creditCardObj;
                            Object value = creditCardMap.get("value");
                            if (value instanceof Number) {
                                cardBalance = ((Number) value).longValue();
                            }
                        }

                        Object cashObj = balancesMap.get("cash");
                        if (cashObj instanceof Map) {
                            Map<String, Object> cashMap = (Map<String, Object>) cashObj;
                            Object value = cashMap.get("value");
                            if (value instanceof Number) {
                                cashBalance = ((Number) value).longValue();
                            }
                        }

                        cardMoney.setText(String.format(Locale.getDefault(), "€%d", cardBalance));
                        cashMoney.setText(String.format(Locale.getDefault(), "€%d", cashBalance));
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
}