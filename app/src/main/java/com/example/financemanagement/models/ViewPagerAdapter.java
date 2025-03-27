package com.example.financemanagement.models;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.financemanagement.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static int[] dateInfos;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0)
            return new IncomeFragment();
        else
            return new ExpensesFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // We have two fragments, Income and Expenses
    }

    public static class IncomeFragment extends Fragment {
        private FirebaseFirestore fstore;
        private EditText amount, note;
        private Spinner category_spinner;
        private Button date_btn, time_btn, income_btn;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);
            FirebaseApp.initializeApp(requireContext());
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Cibo, Casa, Sport, Benessere, Vestiti, Trasporti, Abbonamenti, Cibo fuori casa, Svago

            income_btn = view.findViewById(R.id.income_btn);
            view.findViewById(R.id.expense_btn).setVisibility(View.GONE);
            income_btn.setVisibility(View.VISIBLE);

            amount = view.findViewById(R.id.amount);
            category_spinner = view.findViewById(R.id.category_spinner);
            note = view.findViewById(R.id.note);
            date_btn = view.findViewById(R.id.btn_date);
            time_btn = view.findViewById(R.id.btn_time);

            dateDialog(date_btn, time_btn, LocalDateTime.now(), requireContext());

            income_btn.setOnClickListener(v -> {
                boolean isValid = true;

                Log.d("IncomeFragment", "È un'entrata");
                // Check if amount, note, date and time are empty
                if (!amount.getText().toString().trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                    amount.setError("Amount must be a valid number (ex 12, 12.3, 12.34");
                    isValid = false;
                } else
                    amount.setError(null);

                if (!note.getText().toString().trim().isEmpty() && note.getText().toString().length() > 100) {
                    note.setError("Note is not required but it must have less than 100 characters");
                    isValid = false;
                } else
                    note.setError(null);

                if (date_btn.getText().toString().equals("Date")) {
                    date_btn.setError("Date is required");
                    isValid = false;
                } else
                    date_btn.setError(null);

                if (time_btn.getText().toString().equals("Time")) {
                    time_btn.setError("Time is required");
                    isValid = false;
                } else
                    time_btn.setError(null);

                // If any input is invalid, don't dismiss the dialog
                if (isValid) {
                    Transaction transaction = new Transaction();

                    // Transaction setting
                    transaction.setAmount(Float.parseFloat(amount.getText().toString()));
                    transaction.setNote(note.getText().toString());
                    transaction.setDate(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);

                    // Salva la transazione in Firestore (nella collection "Transactions")
                    db.collection("Transactions")
                            .add(transaction)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("Firestore", "Transazione salvata con ID: " + documentReference.getId());
                            })
                            .addOnFailureListener(e ->
                                    Log.e("Firestore", "Errore nel salvataggio", e));
                }
            });

            return view;
        }
    }

    public static class ExpensesFragment extends Fragment {
        private EditText amount, note;
        private Spinner category_spinner;
        private Button date_btn, time_btn, expense_btn;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);

            expense_btn = view.findViewById(R.id.expense_btn);
            view.findViewById(R.id.income_btn).setVisibility(View.GONE);
            expense_btn.setVisibility(View.VISIBLE);

            amount = view.findViewById(R.id.amount);
            category_spinner = view.findViewById(R.id.category_spinner);
            note = view.findViewById(R.id.note);
            date_btn = view.findViewById(R.id.btn_date);
            time_btn = view.findViewById(R.id.btn_time);

            dateDialog(date_btn, time_btn, LocalDateTime.now(), requireContext());

            return view;
        }
    }

    private static void dateDialog(Button btn_date, Button btn_time, LocalDateTime editDate, Context context) {
        dateInfos = new int[] { editDate.getDayOfMonth(), editDate.getMonthValue(), editDate.getYear(), editDate.getHour(), editDate.getMinute() };

        // Date picker
        btn_date.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(context, (view, year, month, day) -> {
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
                btn_date.setTextSize(22);
            }, editDate.getYear(), editDate.getMonthValue()-1, editDate.getDayOfMonth());

            dialog.getDatePicker().setCalendarViewShown(false);
            dialog.show();
        });

        // Time picker
        btn_time.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(context,
                    android.R.style.Theme_Holo_Light_Dialog_NoActionBar, (view, hour, minute) -> {
                dateInfos[3] = hour;
                dateInfos[4] = minute;
                String timeText = hour + ":" + String.format(Locale.getDefault(), "%02d", minute);

                btn_time.setText(timeText);
            }, editDate.getHour(), editDate.getMinute(), true);
            dialog.show();
        });
    }
}