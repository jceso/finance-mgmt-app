package com.example.financemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.CommonFeatures;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;

public class UserProfile extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseUser user;
    private HashSet<String> salesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set basic buttons
        CommonFeatures.setBackToHome(this, this, getOnBackPressedDispatcher());
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> CommonFeatures.handleLogoutButton(UserProfile.this));
        ImageView name_btn = findViewById(R.id.user);
        CommonFeatures.checkUserAndSetNameButton(UserProfile.this, name_btn);

        showDetails();
    }

    private void showDetails() {
        user = FirebaseAuth.getInstance().getCurrentUser();

        TextView username = findViewById(R.id.username);
        TextView email = findViewById(R.id.email);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get infos from the FireStore document
                    username.setText(documentSnapshot.getString("name"));
                    email.setText(documentSnapshot.getString("email"));
                    Log.d("User Info", "User " + username.getText() + " | " + documentSnapshot);
                } else
                    Log.d("User Info", "No such document");
        });
    }

    /*
    private void btnSetting() {
        Button edit_btn = findViewById(R.id.edit_btn);
        Button delete_btn = findViewById(R.id.delete_btn);

        // EDIT button
        edit_btn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), EditProfile.class));
            finish();
        });

        /*
        // DELETE button
        delete_btn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(UserProfile.this).inflate(R.layout.warning_dialog, null);
            EditText pw = dialogView.findViewById(R.id.currPw);
            Button delete = dialogView.findViewById(R.id.delete_btn);
            Button cancel = dialogView.findViewById(R.id.cancel_btn);

            AlertDialog alertDialog = new AlertDialog.Builder(UserProfile.this)
                    .setView(dialogView)
                    .create();
            alertDialog.show();

            // Cancel button
            cancel.setOnClickListener(v1 -> alertDialog.dismiss());

            // Delete confirmation button
            delete.setOnClickListener(v2 -> {
                salesList = new HashSet<>();
                database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
                String password = pw.getText().toString().trim();

                // Check if title, place, and description are empty
                if (password.isEmpty()) {
                    pw.setError("Password is required");
                } else if (user != null) {
                    pw.setError(null);
                    TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

                    // Searching events organized by user
                    database.getReference().child("event").orderByChild("organizer").equalTo(user.getUid()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                            for (DataSnapshot event : eventSnapshot.getChildren()) {
                                // Search for sales related to organized events
                                searchSalesForEvents(event.getKey(), taskCompletionSource);
                            }
                            // Search for sales bought from the current user
                            searchSalesForUser(user.getUid(), taskCompletionSource);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            taskCompletionSource.setException(error.toException());
                        }
                    });

                    taskCompletionSource.getTask().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                deleteSales(salesList, password);
                            else
                                Log.e("Delete Error", "Failed to fetch sales: " + task.getException().getMessage());
                        }
                    });
                }
            });
        });

    }
    */

    private void deleteUserData() {
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Users").document(user.getUid()).delete().addOnSuccessListener(lambda -> {
            Log.d("Delete User", "User document deleted from Firestore.");

            user.delete().addOnSuccessListener(lambda1 -> {
                Log.d("Delete User", "User deleted from Firebase Authentication.");
                Toast.makeText(UserProfile.this, "User has been deleted successfully", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(UserProfile.this, Login.class));
                finish();
            }).addOnFailureListener(e -> {
                Log.e("Delete User", "Failed to delete user from Firebase Authentication.", e);
                Toast.makeText(UserProfile.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> Log.e("Delete User", "Failed to delete user document from Firestore.", e));
    }
}