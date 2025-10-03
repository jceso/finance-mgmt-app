package com.example.financemanagement;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

        ImageButton filterButton = findViewById(R.id.filters);
        filterButton.setOnClickListener(v -> showDialog());
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
        Spinner typeSpinner = activity.findViewById(R.id.type_spinner);
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

    private void showDialog() {
        // Inflate the custom layout from XML
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filters, null);
        builder.setView(dialogView);

        ChipGroup chipGroup = dialogView.findViewById(R.id.selected_categories);
        EditText categorySearch = dialogView.findViewById(R.id.category_search);
        Button findButton = dialogView.findViewById(R.id.find_btn);
        AppCompatImageButton cancelButton = dialogView.findViewById(R.id.cancel_btn);

        // Recupera le categorie da Firestore
        db.collection("Users").document(Objects.requireNonNull(user).getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> categories = (Map<String, Object>) documentSnapshot.get("Categories");

                    // Aggiungi la chip "X" nascosta all'inizio
                    Chip clearChip = new Chip(this);
                    clearChip.setText("X");
                    clearChip.setCheckable(false);
                    clearChip.setChipBackgroundColorResource(R.color.negative);
                    clearChip.setTextColor(getResources().getColor(R.color.white));
                    clearChip.setVisibility(View.GONE);
                    chipGroup.addView(clearChip);

                    // Aggiungi chip per ogni categoria
                    for (String categoryName : Objects.requireNonNull(categories).keySet()) {
                        Chip chip = new Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice);
                        chip.setText(String.format("%s%s", categoryName.substring(0, 1).toUpperCase(), categoryName.substring(1).toLowerCase()));
                        chip.setCheckable(true);
                        chip.setClickable(true);

                        chipGroup.addView(chip);
                    }

                    clearChip.setOnClickListener(v1 -> {
                        // Deseleziona tutti i chip
                        for (int i = 0; i < chipGroup.getChildCount(); i++) {
                            View child = chipGroup.getChildAt(i);
                            if (child instanceof Chip && ((Chip) child).isChecked()) {
                                ((Chip) child).setChecked(false);
                            }
                        }
                        clearChip.setVisibility(View.GONE);
                    });

                    // Listener per mostrare/nascondere la chip "X"
                    chipGroup.setOnCheckedStateChangeListener((group, checkedId) -> {
                        boolean hasSelection = false;
                        for (int i = 0; i < group.getChildCount(); i++) {
                            View child = group.getChildAt(i);
                            if (child instanceof Chip && ((Chip) child).isChecked()) {
                                hasSelection = true;
                                break;
                            }
                        }
                        clearChip.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
                    });
                }
            }).addOnFailureListener(e -> Log.e("CategoriesLoad", "Errore caricando categorie", e));

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}