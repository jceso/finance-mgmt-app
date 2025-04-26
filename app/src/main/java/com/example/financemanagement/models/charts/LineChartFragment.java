package com.example.financemanagement.models.charts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.financemanagement.R;
import com.github.mikephil.charting.charts.LineChart;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LineChartFragment extends Fragment {
    private DocumentReference userRef;
    private LineChart lineChart;

    public LineChartFragment() { }

    public LineChartFragment(FirebaseFirestore fStore, String userId) {
        this.userRef = fStore.collection("Users").document(userId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fgmt_line_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        lineChart = view.findViewById(R.id.line_chart);
        // Set up your PieChart here
    }
}
