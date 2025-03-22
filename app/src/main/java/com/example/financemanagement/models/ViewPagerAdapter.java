package com.example.financemanagement.models;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.financemanagement.R;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private FragmentActivity activity;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.activity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0)
            return new IncomeFragment();
        else
            return new ExpensesFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // We have two fragments, Income and Expenses
    }

    public static class IncomeFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);

            view.findViewById(R.id.expense_btn).setVisibility(View.GONE);
            view.findViewById(R.id.income_btn).setVisibility(View.VISIBLE);

            return view;
        }
    }

    public static class ExpensesFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);

            view.findViewById(R.id.income_btn).setVisibility(View.GONE);
            view.findViewById(R.id.expense_btn).setVisibility(View.VISIBLE);

            return view;
        }
    }
}