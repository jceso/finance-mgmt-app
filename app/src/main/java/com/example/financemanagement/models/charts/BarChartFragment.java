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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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

public class BarChartFragment extends Fragment {
    private DocumentReference userRef;
    private BarChart barChart;

    public BarChartFragment() { }

    public BarChartFragment(FirebaseFirestore fStore, String userId) {
        this.userRef = fStore.collection("Users").document(userId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fgmt_bar_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        barChart = view.findViewById(R.id.bar_chart);
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

        Log.d("BarChart Debug", "Start of month: " + startOfMonth + " || End of month: " + endOfMonth);

        userRef.get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) return;

            // Get favorite categories
            Map<String, Object> categories = (Map<String, Object>) userDoc.get("categories");
            List<String> favoriteCategories = new ArrayList<>();
            for (Map.Entry<String, Object> entry : Objects.requireNonNull(categories).entrySet()) {
                Map<String, Object> category = (Map<String, Object>) entry.getValue();
                if ("expense".equals(category.get("type")))
                    favoriteCategories.add(entry.getKey());
            }

            // Fetch all expense transactions for the current month
            userRef.collection("Transactions")
                .whereGreaterThanOrEqualTo("date", startOfMonth).whereLessThanOrEqualTo("date", endOfMonth)
                .get().addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> categorySums = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String category = doc.getString("category");
                        Number amount = doc.getDouble("amount");

                        if ("expense".equals(type) && favoriteCategories.contains(category) && amount != null)
                            categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount.floatValue());
                        Log.d("PieChart Debug", "Category: " + category + ", Amount: " + amount + " | Category sums: " + categorySums);
                    }

                    // Build BarChart entries
                    ArrayList<BarEntry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    int index = 0;
                    for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                        entries.add(new BarEntry(index, entry.getValue()));
                        labels.add(entry.getKey());
                        index++;
                    }

                    BarDataSet dataSet = new BarDataSet(entries, null);
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    barChart.setData(new BarData(dataSet));

                    // X-Axis setup
                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setDrawGridLines(false);

                    barChart.getAxisRight().setEnabled(false);
                    barChart.getDescription().setEnabled(false);
                    barChart.getLegend().setEnabled(false);
                    barChart.setFitBars(true);
                    barChart.animateY(1000);
                    barChart.setTouchEnabled(false);
                    barChart.setDragEnabled(false);
                    barChart.invalidate(); // Refresh chart
            });
        });
    }
}