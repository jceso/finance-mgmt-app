package com.example.financemanagement;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

import android.os.Handler;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

public class Home extends AppCompatActivity {

    private Switch darkLightMode;

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
        pieChart.setCenterText("My Pie Chart");
        pieChart.animateY(1000);
        pieChart.invalidate();

        ImageView avatar = findViewById(R.id.avatar);
        ImageView settings = findViewById(R.id.settings);
        Button incomeBtn = findViewById(R.id.income);
        Button expensesBtn = findViewById(R.id.expenses);

        settings.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Settings.class));
        });

        incomeBtn.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, AddTransaction.class));
        });

        expensesBtn.setOnClickListener(view -> {
            startActivity(new Intent(Home.this, AddTransaction.class));
        });
    }
}