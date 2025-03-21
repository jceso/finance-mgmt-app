package com.example.financemanagement;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.financemanagement.models.Transaction;

import java.time.LocalDateTime;
import java.util.Locale;

public class AddTransaction extends AppCompatActivity {
    private EditText price, note;
    private Spinner category;
    private Button btn_date, btn_time, save;
    private int[] dateInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_transaction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting


        price = findViewById(R.id.priceET);
        note = findViewById(R.id.noteET);
        category = findViewById(R.id.category_spinner);
        btn_date = findViewById(R.id.btn_date);
        btn_time = findViewById(R.id.btn_time);
        save = findViewById(R.id.save);

        // Set date and time picker
        dateDialog(btn_date, btn_time, LocalDateTime.now());

        save.setOnClickListener(v -> {
            boolean isValid = true;

            // Format check
            if (price.getText().toString().trim().isEmpty() && !price.getText().toString().trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                price.setError("Price must be a valid number (ex 12, 12.3, 12.34");
                isValid = false;
            } else
                price.setError(null);

            if (btn_date.getText().toString().equals("Date")) {
                btn_date.setError("Date is required");
                isValid = false;
            } else
                btn_date.setError(null);

            // If any input is invalid, don't dismiss the dialog
            if (isValid) {
                Transaction transaction = new Transaction();

                transaction.setAmount(Float.parseFloat(price.getText().toString()));
                transaction.setNote(note.getText().toString());
                transaction.setDate(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);

                // Save event to Firebase



                // Dismiss the dialog
                finish();
            }
        });
    }

    private void dateDialog(Button btn_date, Button btn_time, LocalDateTime editDate) {
        dateInfos = new int[] { editDate.getDayOfMonth(), editDate.getMonthValue(), editDate.getYear(), editDate.getHour(), editDate.getMinute() };

        // Date picker
        btn_date.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(AddTransaction.this, (view, year, month, day) -> {
                dateInfos[0] = day;
                dateInfos[1] = month+1;   //Convert 0-based month to 1-based (DatePickerDialog -> LocalDateTime)
                dateInfos[2] = year;
                String dateText = day + "/" + (month+1) + "/" + year;

                // Ensure month is valid (1-12)
                if (dateInfos[1] < 1 || dateInfos[1] > 12) {
                    // If the month is invalid, set to a default valid month, e.g., January (1)
                    Log.d("DatePickerDialog", "Invalid month: " + dateInfos[1]);
                }

                Log.d("DatePickerDialog", "Selected date: " + dateInfos[0] + "/" + dateInfos[1] + "/" + dateInfos[2]);
                btn_date.setText(dateText);
            }, editDate.getYear(), editDate.getMonthValue()-1, editDate.getDayOfMonth());
            dialog.show();
        });

        // Time picker
        btn_time.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(AddTransaction.this, (view, hour, minute) -> {
                dateInfos[3] = hour;
                dateInfos[4] = minute;
                String timeText = hour + ":" + String.format(Locale.getDefault(), "%02d", minute);

                btn_time.setText(timeText);
            }, editDate.getHour(), editDate.getMinute(), true);
            dialog.show();
        });
    }
}