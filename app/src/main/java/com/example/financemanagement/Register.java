package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            String name, email, password;
            name = nameInput.getText().toString().trim();
            email = emailInput.getText().toString().trim();
            password = passwordInput.getText().toString().trim();

            if (name.isEmpty()) {
                nameInput.setError("Name is required");
                return;
            } else
                nameInput.setError(null);

            if (email.isEmpty() || !email.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                emailInput.setError("Invalid email format");
                return;
            } else
                emailInput.setError(null);

            if (password.isEmpty() || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
                passwordInput.setError("At least 8 characters, one capital letter, one number and one special character");
                return;
            } else
                passwordInput.setError(null);

            fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {  // Sign in success, update UI with the signed-in user's information
                    FirebaseUser user = fAuth.getCurrentUser();
                    DocumentReference dr = fStore.collection("Users").document(Objects.requireNonNull(user).getUid());

                    // Save user data
                    Map<String,Object> userInfo = new HashMap<>();
                    userInfo.put("name", name);
                    userInfo.put("email", email);
                    // Save initial balances
                    Map<String, Object> balances = new HashMap<>();
                    balances.put("cash", 0);
                    balances.put("credit_card", 0);
                    balances.put("save_perc", 15);
                    userInfo.put("Balances", balances);

                    // Define categories (directly in the user document)
                    Map<String, Object> categories = new HashMap<>();
                    // EXPENSES
                    categories.put("Cibo", createCategory("icc_food", "expense", true));
                    categories.put("Casa", createCategory("icc_home", "expense", false));
                    categories.put("Sport", createCategory("icc_sport", "expense", false));
                    categories.put("Benessere", createCategory("icc_wellness", "expense", false));
                    categories.put("Vestiti", createCategory("icc_clothes", "expense", false));
                    categories.put("Trasporti", createCategory("icc_transport", "expense", false));
                    categories.put("Iscrizioni", createCategory("icc_subscriptions", "expense", false));
                    categories.put("Cibo fuori casa", createCategory("icc_out_of_home", "expense", true));
                    categories.put("Svago", createCategory("icc_entertainment", "expense", true));
                    // INCOMES
                    categories.put("Stipendio", createCategory("icc_job", "income", true));
                    categories.put("Regalo", createCategory("icc_giftcard", "income", false));
                    categories.put("Prestito", createCategory("icc_handshake", "income", true));

                    // Add categories to the user info and save to Firestore
                    userInfo.put("categories", categories);
                    dr.set(userInfo)
                            .addOnSuccessListener(aVoid -> Log.d("UserCreation", "User and categories saved successfully"))
                            .addOnFailureListener(e -> Log.e("UserCreation", "Error saving user and categories", e));

                    Log.w("UserCreation", "Account created correctly", task.getException());
                    Toast.makeText(Register.this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), Home.class));
                    finish();
                } else {
                    Log.w("UserCreation", "createUserWithEmail:failure", task.getException());
                    Toast.makeText(Register.this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                }

            });
        });
    }

    private static Map<String, Object> createCategory(String icon, String type, boolean fav) {
        Map<String, Object> category = new HashMap<>();
        category.put("icon", icon);
        category.put("type", type);
        category.put("fav", fav);
        return category;
    }
}