package com.example.financemanagement.models.charts;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class ChartPagerAdapter extends FragmentStateAdapter {
    private Boolean isHome;

    public ChartPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ChartPagerAdapter(@NonNull FragmentActivity fragmentActivity, Boolean isHome) {
        super(fragmentActivity);
        this.isHome = isHome;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Log.d("ChartPagerAdapter", "createFragment called with position: " + position);

        if (!isHome) {
            switch (position) {
                case 0:
                default:
                    return new PieChartFragment(fStore, userId);
                case 1:
                    return new BarChartFragment(fStore, userId);
                case 2:
                    return new LineChartFragment(fStore, userId);
            }
        } else {    // Statistics or TransactionsShow view
            switch (position) {
                case 0:
                default:
                    return new PieChartFragment(fStore, userId, true, true);
                case 1:
                    return new BarChartFragment(fStore, userId);
                case 2:
                    return new LineChartFragment(fStore, userId);
            }
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Total number of pages
    }
}