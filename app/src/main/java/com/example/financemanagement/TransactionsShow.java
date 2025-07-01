package com.example.financemanagement;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.financemanagement.models.Category;
import com.example.financemanagement.models.CategoryAdapter;
import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.Transaction;
import com.example.financemanagement.models.TransactionAdapter;
import com.example.financemanagement.models.charts.ChartPagerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
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

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());

        // Get the type of transactions to show
        TextView type = findViewById(R.id.type);
        TextView amount = findViewById(R.id.amount);
        String transactionsType = getIntent().getStringExtra("type");
        String moneyAmount = getIntent().getStringExtra("money");
        Log.d("TransactionsShow", "Transactions type: " + transactionsType + ", Money: " + moneyAmount);
        type.setText(!Objects.equals(transactionsType, "credit_card") ? "Cash" : "Card");
        amount.setText(moneyAmount);

        // Initialize the ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager == null)
            Log.e("TransactionsShow", "ViewPager2 is null. Check the layout.");
        else {
            ChartPagerAdapter adapter = new ChartPagerAdapter(this, false);
            viewPager.setAdapter(adapter);
        }
        // Initialize the CircleIndicator3
        CircleIndicator3 circleIndicator = findViewById(R.id.circle_indicator);
        if (circleIndicator == null)
            Log.e("TransactionsShow", "CircleIndicator3 is null. Check the layout.");
        else
            circleIndicator.setViewPager(viewPager);

        getTransactions("date", this, this);    // Chiamata iniziale
        orderSpinnerSetup(this, this, this);
    }

    private static void getTransactions(String orderByField, LifecycleOwner lcOwner, Activity activity) {
        Query query = db.collection("Users")
            .document(user.getUid())
            .collection("Transactions")
            .orderBy(orderByField, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Transaction> options = new FirestoreRecyclerOptions.Builder<Transaction>()
            .setQuery(query, Transaction.class).setLifecycleOwner(lcOwner).build();

        TransactionAdapter adapter = new TransactionAdapter(options);
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        TextView emptyList = activity.findViewById(R.id.empty_list);
        if (adapter.getItemCount() == 0)
            emptyList.setVisibility(View.GONE);
        else
            emptyList.setVisibility(View.VISIBLE);
    }

    private void orderSpinnerSetup(Context context, AppCompatActivity activity, LifecycleOwner lcOwner) {
        Spinner order_spinner = activity.findViewById(R.id.order_spinner);
        List<String> orderOptions = new ArrayList<>();
        orderOptions.add("Date");
        orderOptions.add("Amount");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, orderOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        order_spinner.setAdapter(adapter);

        // Listener per aggiornare l'adapter quando cambia selezione
        order_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Aggiorna il RecyclerView con il nuovo ordine ("date" o "amount")
                String selected = orderOptions.get(position).toLowerCase();
                getTransactions(selected, lcOwner, activity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void ctgSpinnerSetup(Activity activity, Boolean isExpense, Context context) {
        Spinner category_spinner = activity.findViewById(R.id.category_spinner);

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

}