package com.example.financemanagement.models.charts;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.financemanagement.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PieChartFragment extends Fragment {
    private DocumentReference userRef;
    private PieChart pieChart;
    private Boolean isExpense = false;
    private Boolean favOption = false;
    private String method;

    public PieChartFragment() { }

    public PieChartFragment(FirebaseFirestore fStore, String userId) {
        this.userRef = fStore.collection("Users").document(userId);
    }

    public PieChartFragment(FirebaseFirestore fStore, String userId, Boolean isExpense, Boolean favOption) {
        this.userRef = fStore.collection("Users").document(userId);
        this.isExpense = isExpense;
        this.favOption = favOption;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fgmt_pie_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        pieChart = view.findViewById(R.id.pie_chart);
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

        userRef.get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) return;

            // Get favorite categories
            Map<String, Object> categories = (Map<String, Object>) userDoc.get("Categories");
            List<String> selectedCategories = new ArrayList<>();
            for (Map.Entry<String, Object> entry : Objects.requireNonNull(categories).entrySet()) {
                Map<String, Object> category = (Map<String, Object>) entry.getValue();

                // Get favourite expenses/incomes only if favOption is true
                boolean typeTransaction = isExpense ? "expense".equals(category.get("type")) : "income".equals(category.get("type"));
                boolean isFav = Boolean.TRUE.equals(category.get("fav"));

                if (typeTransaction && (!favOption || isFav))
                    selectedCategories.add(entry.getKey());
            }

            // Fetch all expense transactions for the current month
            userRef.collection("Transactions")
                .whereGreaterThanOrEqualTo("date", startOfMonth).whereLessThanOrEqualTo("date", endOfMonth)
                .get().addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> categorySums = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String category = doc.getString("category");
                        Number amount = doc.getDouble("amount");

                        if (selectedCategories.contains(category) && amount != null)
                            categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount.floatValue());
                        Log.d("PieChart Debug", "Category: " + category + ", Amount: " + amount + " | Category sums: " + categorySums);
                    }

                    // Build PieChart entries
                    ArrayList<PieEntry> entries = new ArrayList<>();
                    for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                        entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    }

                    PieDataSet dataSet = new PieDataSet(entries, null);
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                    pieChart.setData(new PieData(dataSet));
                    pieChart.getDescription().setEnabled(false);
                    pieChart.getLegend().setEnabled(false);
                    pieChart.setCenterText("Monthly Expenses");
                    pieChart.animateY(1000);
                    pieChart.setTouchEnabled(false);
                    pieChart.setRotationEnabled(false);
                    pieChart.invalidate();
            });
        });
    }
}
