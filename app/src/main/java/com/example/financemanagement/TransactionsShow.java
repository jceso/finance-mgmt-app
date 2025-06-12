package com.example.financemanagement;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.Category;
import com.example.financemanagement.models.CategoryAdapter;
import com.example.financemanagement.models.charts.ChartPagerAdapter;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.relex.circleindicator.CircleIndicator3;

public class TransactionsShow extends AppCompatActivity {
    private static FirebaseUser user;
    private static FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transactions_show);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager == null)
            Log.e("MainActivity", "ViewPager2 is null. Check the layout.");
        else {
            ChartPagerAdapter adapter = new ChartPagerAdapter(this, false);
            viewPager.setAdapter(adapter);
        }
        // Initialize the CircleIndicator3
        CircleIndicator3 circleIndicator = findViewById(R.id.circle_indicator);
        if (circleIndicator == null)
            Log.e("MainActivity", "CircleIndicator3 is null. Check the layout.");
        else
            circleIndicator.setViewPager(viewPager);


        transactionSearch(this, this);
    }

    private static void orderSpinnerSetup(Spinner order_spinner, Context context) {
        List<String> orderOptions = new ArrayList<>();
        orderOptions.add("Date");
        orderOptions.add("Amount");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, orderOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        order_spinner.setAdapter(adapter);
    }

    private static void ctgSpinnerSetup(Spinner category_spinner, Boolean isExpense, Context context) {
        if (user != null) {
            String categoryType = isExpense ? "expense" : "income";
            Log.d("SpinnerSetup","Category type: " + categoryType);

            db.collection("Users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Retrieve the categories array from the user document
                    Map<String, Map<String, Object>> categoriesMap = (Map<String, Map<String, Object>>) documentSnapshot.get("categories");
                    Log.d("SpinnerSetup", "Categories: " + categoriesMap);

                    if (categoriesMap != null) {
                        List<Category> categories = new ArrayList<>();
                        categories.add(new Category(categoryType, "All", "icc_category"));
                        categories.add(new Category(categoryType, "Favourites", "ic_fav_full"));

                        for (Map.Entry<String, Map<String, Object>> entry : categoriesMap.entrySet()) {
                            String name = entry.getKey();
                            Map<String, Object> cat = entry.getValue();
                            String type = (String) cat.get("type");

                            Log.d("SpinnerSetup", "Name: " + name + ", Type: " + type + " - Asked for: " + categoryType);

                            if (Objects.equals(type, categoryType)) {
                                String icon = (String) cat.get("icon");
                                categories.add(new Category(type, name, icon));
                            }
                        }

                        CategoryAdapter adapter = new CategoryAdapter(context, categories);
                        category_spinner.setAdapter(adapter);
                    }
                }
            }).addOnFailureListener(e -> Log.e("SpinnerSetup", "Error in retrieving categories", e));
        }
    }

    private static void transactionSearch(AppCompatActivity activity, Context context) {
        Spinner order_spinner = activity.findViewById(R.id.order_spinner);
        Spinner category_spinner = activity.findViewById(R.id.category_spinner);

        orderSpinnerSetup(order_spinner, context);
        ctgSpinnerSetup(category_spinner, false, context);
    }
}