package com.example.financemanagement;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.Category;
import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Objects;

public class AddTransaction extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_transaction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());

        // Get amount, merchant, tab index and categories from home/notification
        double amount = getIntent().getDoubleExtra("AMOUNT", 0);
        String merchant = getIntent().getStringExtra("MERCHANT");
        int tabIndex = getIntent().getIntExtra("TAB_INDEX", 1);     // INCOME = 0  |  EXPENSES = 1
        ArrayList<Category> allCategories = getIntent().getParcelableArrayListExtra("CATEGORIES");
        Log.d("AddTransaction", "Amount: " + amount + ", Merchant: " + merchant + ", Tab index: " + tabIndex);

        ViewPagerAdapter adapter;
        adapter = new ViewPagerAdapter(this, amount, merchant, allCategories);

        // Setting tabs
        tabLayout = findViewById(R.id.tabLayout);
        viewPager2 = findViewById(R.id.viewPager);
        viewPager2.setAdapter(adapter);
        viewPager2.post(() -> viewPager2.setCurrentItem(tabIndex, false));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });
    }
}