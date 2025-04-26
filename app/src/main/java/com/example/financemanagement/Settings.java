package com.example.financemanagement;

import static com.example.financemanagement.models.CommonFeatures.setAppTheme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

public class Settings extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private List<String> categoryList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private Switch switcherMode;
    private SharedPreferences sharedPreferences;
    private static final String PREFERENCES_FILE = "com.financemgmt.preferences";
    private static final String THEME_KEY = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        setAppTheme(isDarkMode, this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Button card_stg = findViewById(R.id.card_stg);
        Button cash_stg = findViewById(R.id.cash_stg);
        card_stg.setOnClickListener(view -> {
            showDialog("credit_card");
        });
        cash_stg.setOnClickListener(view -> {
            showDialog("cash");
        });

        // Initialize the switch and check current mode
        switcherMode = findViewById(R.id.switcher_mode);
        switcherMode.setChecked(isDarkMode);

        switcherMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save user's preference, apply theme and restart the activity
                sharedPreferences.edit().putBoolean(THEME_KEY, isChecked).apply();
                setAppTheme(isChecked, Settings.this);
                recreate();
            }
        });

        Button expCatBtn = findViewById(R.id.exp_categories);
        expCatBtn.setOnClickListener(v -> {
            showCategoriesDialog("expense");
        });


        /*
         * PAGE TO SCHEDULE A LOCAL NOTIFICATION (ex. Every Saturday and Sunday at 9.30pm)
         *
         * Schedule Local Notifications Android Studio Kotlin Tutorial:
         * https://www.youtube.com/watch?app=desktop&v=_Z2S63O-1HE
         *
         * AlarmManager in Android Studio || Notification using AlarmManager is Android Studio || 2023:
         * https://www.youtube.com/watch?v=5RcDWnNgkQg
         *
         * Local Notifications in Android - The Full Guide (Android Studio Tutorial):
         * https://www.youtube.com/watch?v=LP623htmWcI
         * */

    }

    private void showDialog(String type) {
        // Create the AlertDialog.Builder instance
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (type.equals("credit_card"))
            builder.setTitle("Insert credit card money");
        else
            builder.setTitle("Insert cash money");

        // Inflate the custom layout from XML
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_card_cash, null);
        builder.setView(dialogView);

        EditText amountEditText = dialogView.findViewById(R.id.amount);
        DatePicker date_picker = dialogView.findViewById(R.id.datePicker);
        Button editButton = dialogView.findViewById(R.id.edit_btn);
        AppCompatImageButton cancelButton = dialogView.findViewById(R.id.cancel_btn);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Set an onClickListener for the "Edit" button (you can handle the functionality here)
        editButton.setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                Float creditCardValue = Float.valueOf(amountStr);
                Date currentDateTime = new Date();

                // Prepare the data
                Map<String, Object> creditCardData = new HashMap<>();
                creditCardData.put("value", creditCardValue);
                creditCardData.put("date", currentDateTime);

                // Update the value in Firestore
                db.collection("Users").document(user.getUid())
                    .update("Balances." + type, creditCardData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Settings.this, type + " balance updated", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                    Toast.makeText(Settings.this, "Error while saving " + type + " balance", Toast.LENGTH_SHORT).show();
                });
                dialog.dismiss();
            } else  // Show an error message if the input is empty
                Toast.makeText(Settings.this, "Insert a valid amount", Toast.LENGTH_SHORT).show();
        });
    }

    private void showCategoriesDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(type.equals("expense") ? "Expense Categories" : "Income Categories");

        ListView listView = new ListView(this);
        categoryList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        fetchCategories(type, adapter);

        builder.create().show();
    }

    private void fetchCategories(String type, ArrayAdapter<String> adapter) {
        categoryList.clear(); // pulisci la lista

        db.collection("Users").document(user.getUid())
                .collection("Categories")
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) categoryList.add(name);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Settings.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                });
    }
}