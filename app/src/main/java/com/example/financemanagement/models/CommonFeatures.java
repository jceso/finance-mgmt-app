package com.example.financemanagement.models;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.lifecycle.LifecycleOwner;

import com.example.financemanagement.Home;
import com.example.financemanagement.Login;
import com.example.financemanagement.R;
import com.example.financemanagement.Settings;
import com.example.financemanagement.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommonFeatures {
    private static long lastBackPressedTime = 0;
    private static Toast backToast;

    // Apply theme based on saved preferences
    public static void setAppTheme(Boolean isDarkMode, Activity activity) {
        if (isDarkMode) {
            Toast.makeText(activity, "Dark mode enabled", Toast.LENGTH_SHORT).show();
            activity.setTheme(R.style.Theme_FinanceManagement_Night);
            Log.d("DarkLight","Dark mode enabled " + isDarkMode + R.style.Theme_FinanceManagement_Night);
        } else {
            Toast.makeText(activity, "Light mode enabled", Toast.LENGTH_SHORT).show();
            activity.setTheme(R.style.Theme_FinanceManagement);
            Log.d("DarkLight","Light mode enabled " + isDarkMode + R.style.Theme_FinanceManagement);
        }
    }

    // Static method to handle back press
    public static void setBackToHome(final Activity activity, LifecycleOwner lifecycleOwner, OnBackPressedDispatcher dispatcher) {
        dispatcher.addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(new Intent(activity, Home.class));
                activity.finish();
            }
        });
    }

    // Static method to handle back press in starting views
    public static void setBackExit(final Activity activity, LifecycleOwner lifecycleOwner, OnBackPressedDispatcher dispatcher) {
        dispatcher.addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long currentTime = System.currentTimeMillis();

                if (lastBackPressedTime + 2000 > currentTime) {
                    if (backToast != null)
                        backToast.cancel();
                    activity.finish(); // Close the activity
                } else {
                    backToast = Toast.makeText(activity, "Press again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                    lastBackPressedTime = currentTime;
                }
            }
        });
    }

    // Check user authentication and set user button text
    public static void checkUserAndSetNameButton(Context context, ImageView userAvatar) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


        if (user == null) {
            context.startActivity(new Intent(context, Login.class));
            if (context instanceof Activity)
                ((Activity) context).finish();
        } else {
            // If the user is authenticated, get user info
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the name from the FireStore document and set to button
                            String name = documentSnapshot.getString("Name");
                            if (name != null && !name.isEmpty()) {
                                // First letter of the name
                            }
                        } else
                            Log.d("User Info", "No such document");
                    });
        }

        userAvatar.setOnClickListener(v -> context.startActivity(new Intent(context, UserProfile.class)));
    }

    // Logout functionality
    public static void handleLogoutButton(Context context) {
        FirebaseAuth.getInstance().signOut();
        context.startActivity(new Intent(context, Login.class));
        if (context instanceof Activity)
            ((Activity) context).finish();
    }
}
