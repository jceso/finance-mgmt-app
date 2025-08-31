package com.example.financemanagement;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.example.financemanagement.models.CommonFeatures;
import com.example.financemanagement.models.TransactionAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Objects;

public class Savings extends AppCompatActivity {
    private static DocumentReference userDocRef;
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userDocRef = FirebaseFirestore.getInstance().collection("Users").document(Objects.requireNonNull(user).getUid());
        currIncomes = getIntent().getFloatExtra("curr_incomes", 0);
        currExpenses = getIntent().getFloatExtra("curr_expenses", 0);

        summarySetting();
        getTransactions(this, this);

        Button change = findViewById(R.id.change);
        change.setOnClickListener(v -> showDialog());
        Log.d("Savings", "Savings activity after FireStore call");
    }

    private void summarySetting() {
        LinearLayout summary = findViewById(R.id.summary);
        ImageView pigImage = findViewById(R.id.pig);
        TextView currSavings = findViewById(R.id.curr_savings);
        perc = findViewById(R.id.perc);
        monthlyIncome = findViewById(R.id.monthly_income);
        thCurrSavings = findViewById(R.id.th_curr_savings);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Object savePercObj = documentSnapshot.get("Balances.save_perc");
                int savePerc = (int) (((Number) Objects.requireNonNull(savePercObj)).doubleValue() * 100);
                float calcSavings = (float) (((double) savePerc/100)*currIncomes);
                float maxCurExpenses = currIncomes - calcSavings;
                Log.d("Savings", "Save percentage after: " + savePerc + "%");

                currSavings.setText(String.format(Locale.getDefault(), "€%.2f", (currIncomes-currExpenses)));
                perc.setText(String.format(Locale.getDefault(), "%d%%", savePerc));
                thCurrSavings.setText(String.format("%s %s", getString(R.string.curr_savings), String.format(Locale.getDefault(), "€%.2f", calcSavings)));
                monthlyIncome.setText(String.format("%s %s", getString(R.string.monthly_income), String.format(Locale.getDefault(), "€%.2f", currIncomes)));

                if (currExpenses > maxCurExpenses) {
                    Log.d("HomeSavings", "You wasted money, you broke the pig :(\n  This month you had " + currIncomes + " of income, but you spent " + currExpenses + "\n  You're left with " + (currIncomes-currExpenses) + " instead of the theoretic savings " + calcSavings);

                    summary.setBackgroundResource(R.drawable.rounded_negative);
                    pigImage.setImageResource(R.drawable.pig_sad_original);
                } else {
                    Log.d("HomeSavings", "You saved money, the pig is safe :)\n  This month you had " + currIncomes + " of income, but you spent " + currExpenses + "\n  You're left with " + (currIncomes-currExpenses) + " instead of the theoretic savings " + calcSavings);

                    summary.setBackgroundResource(R.drawable.rounded_positive);
                    pigImage.setImageResource(R.drawable.pig_happy);
                }
            }
        });
    }

    private static void getTransactions(LifecycleOwner lcOwner, Activity activity) {
        TransactionAdapter adapter = new TransactionAdapter(CommonFeatures.getTransactions("date", lcOwner, "savings"));
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        TextView emptyList = activity.findViewById(R.id.empty_list);
        if (adapter.getItemCount() == 0)
            emptyList.setVisibility(View.GONE);
        else
            emptyList.setVisibility(View.VISIBLE);
    }

    private void showDialog() {
        // Inflate the custom layout from XML
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_perc, null);
        builder.setView(dialogView);

        EditText amountEditText = dialogView.findViewById(R.id.perc);
        Button editButton = dialogView.findViewById(R.id.edit_btn);
        AppCompatImageButton cancelButton = dialogView.findViewById(R.id.cancel_btn);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // "Edit" button updates the value in FireStore
        editButton.setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                Integer percValue = Integer.valueOf(amountStr);
                userDocRef.update("Balances.save_perc", percValue)
                    .addOnSuccessListener(aVoid -> Log.d("FireStore", "save_perc updated successfully"))
                    .addOnFailureListener(e -> Log.e("FireStore", "Updating error: " + e.getMessage()));

                dialog.dismiss();
            } else  // Show an error message if the input is empty
                Toast.makeText(Savings.this, "Insert a valid amount", Toast.LENGTH_SHORT).show();
        });
    }
}