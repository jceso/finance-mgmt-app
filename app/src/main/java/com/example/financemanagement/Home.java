package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class Home extends AppCompatActivity {

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

        PieChart pieChart = findViewById(R.id.chart);
        ArrayList<PieEntry> entiers = new ArrayList<>();

        entiers.add(new PieEntry(80f, "Maths"));
        entiers.add(new PieEntry(90f, "Science"));
        entiers.add(new PieEntry(75f, "English"));
        entiers.add(new PieEntry(60f, "IT"));

        // Cibo, Casa, Sport, Benessere, Vestiti, Trasporti, Abbonamenti, Cibo fuori casa, Svago
        // Da mantenere tra i preferiti Cibo-CiboFuoriCasa-Svago

        // 50% Spese fisse - Casa, Cibo, Trasporti
        // 30% Spese variabili - Aperitivi, Cibo fuori casa
        // 20% Risparmio o Emergenze

        PieDataSet pieDataSet = new PieDataSet(entiers, "Subjects");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Favorite Expenses");
        pieChart.animateY(1000);
        pieChart.invalidate();

        ImageView avatar = findViewById(R.id.avatar);
        ImageView settings = findViewById(R.id.settings);
        CommonFeatures.checkUserAndSetNameButton(Home.this, avatar);

        ImageView pig = findViewById(R.id.summary);
        Button incomeBtn = findViewById(R.id.income);
        Button expensesBtn = findViewById(R.id.expenses);

        pig.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Savings.class));
        });

        moneySetting();

        settings.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Settings.class));
        });

        incomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 0); // Prima pagina (Income)
            startActivity(intent);
        });

        expensesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Home.this, AddTransaction.class);
            intent.putExtra("TAB_INDEX", 1); // Seconda pagina (Expenses)
            startActivity(intent);
        });
    }

    private void moneySetting() {
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();

        TextView cardMoney = findViewById(R.id.card_money);
        TextView cashMoney = findViewById(R.id.cash_money);

        // Recupera il documento dell'utente
        fStore.collection("Users").document(fAuth.getCurrentUser().getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Leggi credit_card
                    Map<String, Object> creditCardMap = (Map<String, Object>) documentSnapshot.get("Balances.credit_card");
                    Long cardBalance = 0L;
                    if (creditCardMap != null && creditCardMap.get("value") instanceof Number) {
                        cardBalance = ((Number) creditCardMap.get("value")).longValue();
                    }
                    // Leggi cash
                    Map<String, Object> cashMap = (Map<String, Object>) documentSnapshot.get("Balances.cash");
                    Long cashBalance = 0L;
                    if (cashMap != null && cashMap.get("value") instanceof Number) {
                        cashBalance = ((Number) cashMap.get("value")).longValue();
                    }

                    // Imposta i saldi nei TextView, gestendo valori nulli
                    cardMoney.setText(String.format("€%d", cardBalance));
                    cashMoney.setText(String.format("€%d", cashBalance));
                }
            }).addOnFailureListener(e -> {
                cardMoney.setText("Errore");
                cashMoney.setText("Errore");
        });
    }
}