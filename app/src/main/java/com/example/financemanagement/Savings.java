package com.example.financemanagement;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;

public class Savings extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private float currIncomes;
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
        float currExpenses = getIntent().getFloatExtra("curr_expenses", 0);

        // Set basic buttons
        LinearLayout summary = findViewById(R.id.summary);
        ImageView pig = findViewById(R.id.pig);
        TextView currSavings = findViewById(R.id.curr_savings);
        currSavings.setText(String.format(Locale.getDefault(), "€%.2f", (currIncomes-currExpenses)));
        monthlyIncome = findViewById(R.id.monthly_income);
        perc = findViewById(R.id.perc);
        thCurrSavings = findViewById(R.id.th_curr_savings);
        if (currIncomes-currExpenses < 0) {
            summary.setBackgroundResource(R.drawable.rounded_negative);
            pig.setImageResource(R.drawable.pig_sad_original);
        } else {
            summary.setBackgroundResource(R.drawable.rounded_positive);
            pig.setImageResource(R.drawable.pig_happy);
        }

        summarySetting();

        Log.d("Savings", "Savings activity after FireStore call");
    }

    private void summarySetting() {
        DocumentReference userDocRef = fStore.collection("Users").document(user.getUid());

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
}