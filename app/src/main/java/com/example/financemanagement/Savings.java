package com.example.financemanagement;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.Transaction;
import com.example.financemanagement.models.TransactionAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;
import java.util.Map;

public class Savings extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private float currIncomes, currExpenses;
    private TextView perc;
    private TextView thCurrSavings;
    private TextView monthlyIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_savings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initial setting
        CommonFeatures.initialSettings(this);
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());
        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        currIncomes = getIntent().getFloatExtra("curr_incomes", 0);
        currExpenses = getIntent().getFloatExtra("curr_expenses", 0);

        // Set basic buttons
        summarySetting();

        Log.d("Savings", "Savings activity after FireStore call");
    }

    private void summarySetting() {
        LinearLayout summary = findViewById(R.id.summary);
        ImageView pigImage = findViewById(R.id.pig);
        TextView currSavings = findViewById(R.id.curr_savings);
        perc = findViewById(R.id.perc);
        monthlyIncome = findViewById(R.id.monthly_income);
        thCurrSavings = findViewById(R.id.th_curr_savings);

        DocumentReference userDocRef = fStore.collection("Users").document(user.getUid());

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long savePercLong = documentSnapshot.getLong("Balances.save_perc");
                int savePerc = savePercLong != null ? savePercLong.intValue() : 0;
                float calcSavings = (float) (((double) savePerc/100)*currIncomes);
                float maxCurExpenses = currIncomes - calcSavings;
                Log.d("Savings", "Save percentage: " + savePerc + "%");

                currSavings.setText(String.format(Locale.getDefault(), "€%.2f", (currIncomes-currExpenses)));
                perc.setText(String.format(Locale.getDefault(), "%d%%", savePerc));
                thCurrSavings.setText(String.format("%s %s", getString(R.string.curr_savings), String.format(Locale.getDefault(), "€%.2f", calcSavings)));
                monthlyIncome.setText(String.format("%s %s", getString(R.string.monthly_income), String.format(Locale.getDefault(), "€%.2f", currIncomes)));

                if (currExpenses > maxCurExpenses) {
                    Log.d("HomeSavings", "Hai sprecato soldi, hai rotto il porco :(\n  Questo mese avevi " + currIncomes + " di guadagno, ma hai speso " + currExpenses + "\n  Ti restano " + (currIncomes-currExpenses) + " rispetto al risparmio teorico di " + calcSavings);

                    pigImage.setBackgroundResource(R.drawable.pig_mood_sad);
                    pigImage.setImageResource(R.drawable.pig_sad_original);
                } else {
                    Log.d("HomeSavings", "Hai risparmiato soldi, il porco è salvo :)\n  Questo mese avevi " + currIncomes + " di guadagno e hai speso " + currExpenses + "\n  Ti restano " + (currIncomes-currExpenses) + " rispetto al risparmio teorico di " + calcSavings);

                    pigImage.setBackgroundResource(R.drawable.pig_mood_happy);
                    pigImage.setImageResource(R.drawable.pig_happy);
                }
            }
        });

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> balances = (Map<String, Object>) documentSnapshot.get("Balances");
                if (balances != null) {
                    Map<String, Object> fixedIncome = (Map<String, Object>) balances.get("fixed_income");

                    // Retrieve fixed income by database
                    if (fixedIncome != null) {
                        Long savePercLong = (Long) fixedIncome.get("save_perc");
                        int savePerc = savePercLong != null ? savePercLong.intValue() : 0;
                        Log.d("Savings", "Fixed Income: " + fixedIncome + " | Save percentage: " + savePerc + "%");

                        perc.setText(String.format(Locale.getDefault(), "%d%%", savePerc));
                        thCurrSavings.setText(String.format("%s %s", getString(R.string.curr_savings), String.format(Locale.getDefault(), "€%.2f", (currIncomes/100) * savePerc)));
                        monthlyIncome.setText(String.format("%s %s", getString(R.string.monthly_income), String.format(Locale.getDefault(), "€%.2f", currIncomes)));
                    } else
                        Log.d("Savings", "Property 'fixed_income' not found in 'Balances'");
                } else
                    Log.d("Savings", "Property 'Balances' not found");
            } else
                Log.d("Savings", "User infos not found");
        }).addOnFailureListener(e -> Log.e("Savings", "Error in fetching data", e));
    }

    private static void getTransactions(String orderByField, LifecycleOwner lcOwner, Activity activity) {
        TransactionAdapter adapter = new TransactionAdapter(CommonFeatures.getTransactions(orderByField, lcOwner, "savings", "date"));
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        TextView emptyList = activity.findViewById(R.id.empty_list);
        if (adapter.getItemCount() == 0)
            emptyList.setVisibility(View.GONE);
        else
            emptyList.setVisibility(View.VISIBLE);
    }
}