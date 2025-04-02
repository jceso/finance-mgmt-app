package com.example.financemanagement.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.financemanagement.Login;
import com.example.financemanagement.R;
import com.example.financemanagement.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommonFeatures {
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
