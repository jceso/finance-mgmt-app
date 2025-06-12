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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import me.relex.circleindicator.CircleIndicator3;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private String userId;
    private long lastBackPressedTime = 0;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set back button action
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long currentTime = System.currentTimeMillis();

                if (lastBackPressedTime + 2000 > currentTime) {
                    if (backToast != null)
                        backToast.cancel();
                    finish();
                } else {
                    backToast = Toast.makeText(MainActivity.this, "Press again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                    lastBackPressedTime = currentTime;
                }
            }
        });

        fStore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize the ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager == null)
            Log.e("MainActivity", "ViewPager2 is null. Check the layout.");
        else {
            ChartPagerAdapter adapter = new ChartPagerAdapter(this);
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
        // 30% Spese variabili - Aperitivi, Cibo fuori casa
        // 15% Risparmio o Emergenze

        ImageView avatar = findViewById(R.id.avatar);
        ImageView settings = findViewById(R.id.settings);
        CommonFeatures.checkUserAndSetNameButton(MainActivity.this, avatar);

        ImageView pig = findViewById(R.id.summary);
        Button incomeBtn = findViewById(R.id.income);
        Button expensesBtn = findViewById(R.id.expenses);

        pig.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Savings.class));
            finish();
        });

        settings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Settings.class));
            finish();
        });

        incomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 0); // Prima pagina (Income)
            startActivity(intent);
            finish();
        });

        expensesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 1); // Seconda pagina (Expenses)
            startActivity(intent);
            finish();
        });
    }

    private void moneySetting() {
        TextView cardMoney = findViewById(R.id.card_money);
        TextView cashMoney = findViewById(R.id.cash_money);
        TextView loanMoney = findViewById(R.id.loan_money);
        TextView salaryMoney = findViewById(R.id.salary_money);

        // Retrieve user infos
        fStore.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
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
                        if (value instanceof Number)
                            cardBalance = ((Number) value).longValue();
                    }

                    Object cashObj = balancesMap.get("cash");
                    if (cashObj instanceof Map) {
                        Map<String, Object> cashMap = (Map<String, Object>) cashObj;
                        Object value = cashMap.get("value");
                        if (value instanceof Number)
                            cashBalance = ((Number) value).longValue();
                    }

                    cardMoney.setText(String.format("€%d", cardBalance));
                    cashMoney.setText(String.format("€%d", cashBalance));
                } else {
                    cardMoney.setText("€0");
                    cashMoney.setText("€0");
                }

                // Retrieve incomes
            }
        }).addOnFailureListener(e -> {
            cardMoney.setText("Errore");
            cashMoney.setText("Errore");
        });
    }

    private void pieChartSetting() {

        Calendar calendar = Calendar.getInstance();

        // FIRST DAY OF CURRENT MONTH
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();
        // LAST DAY OF CURRENT MONTH
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfMonth = calendar.getTimeInMillis();

        Log.d("PieChart Debug", "Start of month: " + startOfMonth + " || End of month: " + endOfMonth);

        fStore.collection("Users").document(userId).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) return;

            // Get favorite categories
            Map<String, Object> categories = (Map<String, Object>) userDoc.get("categories");
            List<String> favoriteCategories = new ArrayList<>();
            for (Map.Entry<String, Object> entry : categories.entrySet()) {
                Map<String, Object> category = (Map<String, Object>) entry.getValue();
                if (Boolean.TRUE.equals(category.get("fav")) && "expense".equals(category.get("type")))
                    favoriteCategories.add(entry.getKey());
            }

            // Fetch all expense transactions for the current month
            fStore.collection("Users").document(userId).collection("Transactions")
                    .whereGreaterThanOrEqualTo("date", startOfMonth).whereLessThanOrEqualTo("date", endOfMonth)
                    .get().addOnSuccessListener(querySnapshot -> {
                        Map<String, Float> categorySums = new HashMap<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String type = doc.getString("type");
                            String category = doc.getString("category");
                            Number amount = doc.getDouble("amount");

                            if ("expense".equals(type) && favoriteCategories.contains(category) && amount != null)
                                categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount.floatValue());
                            Log.d("PieChart Debug", "Category: " + category + ", Amount: " + amount + " | Category sums: " +categorySums);
                        }

                        // Build PieChart entries
                        ArrayList<PieEntry> entries = new ArrayList<>();
                        for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                        }

                        PieDataSet dataSet = new PieDataSet(entries, null);
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                    });
        });
    }
}