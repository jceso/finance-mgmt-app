package com.example.financemanagement.models.charts;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChartPagerAdapter extends FragmentStateAdapter {

    public ChartPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        Log.d("ChartPagerAdapter", "ChartPagerAdapter constructor called");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d("ChartPagerAdapter", "createFragment called with position: " + position);

        switch (position) {
            case 1:
                return new BarChartFragment(fStore, userId);
            case 2:
                return new LineChartFragment(fStore, userId);
            case 0:
            default:
                return new PieChartFragment(fStore, userId);
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Total number of pages
    }
}