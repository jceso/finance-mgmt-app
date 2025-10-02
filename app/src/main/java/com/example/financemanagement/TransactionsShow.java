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
import java.util.Objects;

import me.relex.circleindicator.CircleIndicator3;

public class TransactionsShow extends AppCompatActivity {
    private static final String CAT_EVERY = "once";
    private static final String CAT_INC = "incomes";
    private static final String CAT_EXP = "expenses";

    private static FirebaseUser user;
    private static FirebaseFirestore db;
    private static String transactionsMethod, transactionOrder, transactionType;

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
        transactionsMethod = getIntent().getStringExtra("type");
        String moneyAmount = getIntent().getStringExtra("money");
        Log.d("TransactionsShow", "Transactions type: " + transactionsMethod + ", Money: " + moneyAmount);
        type.setText(!Objects.equals(transactionsMethod, "credit_card") ? "Cash" : "Card");
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

        transactionOrder = "date";
        transactionType = "everything";
        orderSpinnerSetup(this, this, this);
        typeSpinnerSetup(this, this, this);
    }

    private static void getTransactions(String orderByField, String type,  LifecycleOwner lcOwner, Activity activity) {
        Query query = db.collection("Users")
            .document(user.getUid())
            .collection("Transactions")
            .whereEqualTo("method", transactionsMethod)
            .orderBy(orderByField, Query.Direction.DESCENDING);

        if (!type.equals("everything"))
            query = query.whereEqualTo("type", type);

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
                transactionOrder = orderOptions.get(position).toLowerCase();
                getTransactions(transactionOrder, transactionType, lcOwner, activity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void typeSpinnerSetup(Context context, Activity activity, LifecycleOwner lcOwner) {
        Spinner typeSpinner = activity.findViewById(R.id.category_spinner);
        List<String> typeOptions = new ArrayList<>();
        typeOptions.add("Everything");
        typeOptions.add("Incomes");
        typeOptions.add("Expenses");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, typeOptions);
        typeSpinner.setAdapter(adapter);

        // Listener per aggiornare l'adapter quando cambia selezione
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Aggiorna il RecyclerView con il tipo di transazione
                switch (typeOptions.get(position)) {
                    case "Incomes": transactionType = "income"; break;
                    case "Expenses": transactionType = "expense"; break;
                    case "Everything": transactionType = "everything"; break;
                    // Security fallback
                    default: transactionType = "everything"; break;
                }

                getTransactions(transactionOrder, transactionType, lcOwner, activity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

}