package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Register extends AppCompatActivity {
    EditText nameInput, emailInput, passwordInput;
    Button reg_btn;
    TextView logNow;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), Home.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CommonFeatures.setBackExit(this, this, getOnBackPressedDispatcher());

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        reg_btn = findViewById(R.id.register_btn);
        logNow = findViewById(R.id.log_now);
        logNow.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        reg_btn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (name.isEmpty()) {
                nameInput.setError("Name is required");
                return;
            } else nameInput.setError(null);

            if (email.isEmpty() || !email.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                emailInput.setError("Invalid email format");
                return;
            } else emailInput.setError(null);

            if (password.isEmpty() || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
                passwordInput.setError("At least 8 characters, one capital letter, one number and one special character");
                return;
            } else passwordInput.setError(null);

            // Ask cash balance
            askInitialBalance("cash", (cashValue, cashDate) -> {
                // Ask credit balance
                askInitialBalance("credit card", (creditValue, creditDate) -> fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = fAuth.getCurrentUser();
                        DocumentReference dr = fStore.collection("Users").document(Objects.requireNonNull(user).getUid());

                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("name", name);
                        userInfo.put("email", email);

                        Map<String, Object> balances = new HashMap<>();
                        balances.put("save_perc", 0.3); // 30% del restante 45% di spesa variabile (circa 15% di tutto)
                        balances.put("fixed_income", 0);

                        Map<String, Object> cashBalance = new HashMap<>();
                        cashBalance.put("value", cashValue);
                        cashBalance.put("date", cashDate);
                        balances.put("cash", cashBalance);

                        Map<String, Object> creditCardBalance = new HashMap<>();
                        creditCardBalance.put("value", creditValue);
                        creditCardBalance.put("date", creditDate);
                        balances.put("credit_card", creditCardBalance);

                        userInfo.put("Balances", balances);

                        Map<String, Object> categories = new HashMap<>();
                        categories.put("food", createCategory("icc_food", "expense", true));
                        categories.put("home", createCategory("icc_home", "expense", false));
                        categories.put("sport", createCategory("icc_sport", "expense", false));
                        categories.put("wellness", createCategory("icc_wellness", "expense", false));
                        categories.put("clothes", createCategory("icc_clothes", "expense", false));
                        categories.put("transport", createCategory("icc_transport", "expense", false));
                        categories.put("subscriptions", createCategory("icc_subscriptions", "expense", false));
                        categories.put("out of home", createCategory("icc_out_of_home", "expense", true));
                        categories.put("entertainment", createCategory("icc_entertainment", "expense", true));
                        categories.put("salary", createCategory("icc_job", "income", true));
                        categories.put("gift", createCategory("icc_giftcard", "income", false));
                        categories.put("loan", createCategory("icc_handshake", "income", true));

                        userInfo.put("Categories", categories);

                        dr.set(userInfo)
                                .addOnFailureListener(e -> Log.e("UserCreation", "Error saving user and categories", e))
                                .addOnSuccessListener(aVoid -> Log.d("UserCreation", "User and categories saved successfully"));

                        Toast.makeText(Register.this, "Account created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), Home.class));
                        finish();

                    } else {
                        Log.w("UserCreation", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(Register.this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                    }
                }));
            });
        });
    }

    private static Map<String, Object> createCategory(String icon, String type, boolean fav) {
        Map<String, Object> category = new HashMap<>();
        category.put("icon", icon);
        category.put("type", type);
        category.put("fav", fav);
        if (type.equals("salary") || type.equals("loan"))
            category.put("sum", 0);

        return category;
    }

    private void askInitialBalance(String type, final java.util.function.BiConsumer<Double, java.util.Date> callback) {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("0.0");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Initial " + type + " balance")
            .setMessage("Enter your initial " + type + " balance:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Next", (dialog, which) -> {
                String valueStr = input.getText().toString().trim();
                try {
                    double value = Double.parseDouble(valueStr);
                    if (value < 0) throw new NumberFormatException();

                    // Show date picker after balance input
                    DatePickerDialog datePickerDialog = getDatePickerDialog(callback, value);
                    datePickerDialog.show();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid positive number", Toast.LENGTH_SHORT).show();
                    askInitialBalance(type, callback); // Retry
                }
            }).setNegativeButton("Cancel", (dialog, which) -> {
                callback.accept(0.0, new java.util.Date()); // fallback to now
            }).show();
    }

    private @NonNull DatePickerDialog getDatePickerDialog(BiConsumer<Double, Date> callback, double value) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    callback.accept(value, calendar.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setCancelable(false);
        return datePickerDialog;
    }
}