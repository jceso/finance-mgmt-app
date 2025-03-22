package com.example.financemanagement;

import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
}