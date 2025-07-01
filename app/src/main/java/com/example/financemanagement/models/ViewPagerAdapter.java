package com.example.financemanagement.models;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.financemanagement.Home;
import com.example.financemanagement.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static int[] dateInfos;
    private static FirebaseUser user;
    private static FirebaseFirestore db;

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);

            Button income_btn = view.findViewById(R.id.income_btn);
            view.findViewById(R.id.expense_btn).setVisibility(View.GONE);
            income_btn.setVisibility(View.VISIBLE);

            checkAndSaveTransaction(view, income_btn, false, requireContext());

            return view;
        }
    }

    public static class ExpensesFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fgmt_inc_exp, container, false);

            Button expense_btn = view.findViewById(R.id.expense_btn);
            view.findViewById(R.id.income_btn).setVisibility(View.GONE);
            expense_btn.setVisibility(View.VISIBLE);

            checkAndSaveTransaction(view, expense_btn, true, requireContext());

            return view;
        }
    }

    private static void spinnerSetup(Spinner category_spinner, Boolean isExpense, Context context) {
        if (user != null) {
            String categoryType = isExpense ? "expense" : "income";
            Log.d("SpinnerSetup","Category type: " + categoryType);

            db.collection("Users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the categories array from the user document
                        Map<String, Map<String, Object>> categoriesMap = (Map<String, Map<String, Object>>) documentSnapshot.get("categories");
                        Log.d("SpinnerSetup", "Categories: " + categoriesMap);

                        if (categoriesMap != null) {
                            List<Category> categories = new ArrayList<>();

                            for (Map.Entry<String, Map<String, Object>> entry : categoriesMap.entrySet()) {
                                String name = entry.getKey();
                                Map<String, Object> cat = entry.getValue();
                                String type = (String) cat.get("type");

                                Log.d("SpinnerSetup", "Name: " + name + ", Type: " + type + " - Asked for: " + categoryType);

                                if (Objects.equals(type, categoryType)) {
                                    String icon = (String) cat.get("icon");
                                    categories.add(new Category(type, name, icon));
                                }
                            }

                            CategoryAdapter adapter = new CategoryAdapter(context, categories);
                            category_spinner.setAdapter(adapter);
                        }
                    }
            }).addOnFailureListener(e -> Log.e("SpinnerSetup", "Error in retrieving categories", e));
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
                String dateText = day + "/" + (month+1) + "/" + (year % 100);

                // Ensure month is valid (1-12)
                if (dateInfos[1] < 1 || dateInfos[1] > 12) {
                    // If the month is invalid, set to a default valid month, e.g., January (1)
                    Log.d("DatePickerDialog", "Invalid month: " + dateInfos[1]);
                }

                Log.d("DatePickerDialog", "Selected date: " + dateInfos[0] + "/" + dateInfos[1] + "/" + dateInfos[2]);
                btn_date.setText(dateText);
            }, editDate.getYear(), editDate.getMonthValue()-1, editDate.getDayOfMonth());

            dialog.getDatePicker();
            dialog.show();
        });

        // Time picker
        btn_time.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(context, (view, hour, minute) -> {
                dateInfos[3] = hour;
                dateInfos[4] = minute;
                String timeText = hour + ":" + String.format(Locale.getDefault(), "%02d", minute);

                btn_time.setText(timeText);
            }, editDate.getHour(), editDate.getMinute(), true);
            dialog.show();
        });
    }

    public static void checkAndSaveTransaction(View view, Button save_btn, Boolean isExpense, Context context) {
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        EditText amount = view.findViewById(R.id.amount);
        Spinner category_spinner = view.findViewById(R.id.category_spinner);
        EditText note = view.findViewById(R.id.note);
        Button date_btn = view.findViewById(R.id.btn_date);
        Button time_btn = view.findViewById(R.id.btn_time);
        Button repeat = view.findViewById(R.id.repeat);
        RadioGroup card_or_cash = view.findViewById(R.id.card_cashGroup);

        spinnerSetup(category_spinner, isExpense, context);
        dateDialog(date_btn, time_btn, LocalDateTime.now(), context);
        repeat.setOnClickListener(v -> {
            String[] repeatOptions = {"Not repeated", "Daily", "Weekly", "Monthly", "Yearly"};

            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Repeat transaction")
                .setItems(repeatOptions, (dialog, which) -> {
                    String selectedOption = repeatOptions[which];
                    repeat.setText(selectedOption);

                    if (!selectedOption.equalsIgnoreCase("Not repeated"))
                        repeat.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_check, 0);
                    else
                        repeat.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_circle, 0);
            }).show();
        });

        save_btn.setOnClickListener(v -> {
            boolean isValid = true, isFixed;

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

            if (card_or_cash.getCheckedRadioButtonId() == -1) {
                Toast.makeText(context, "Please select a card or cash", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            if (isValid) {
                save_btn.setEnabled(false);

                // Payment method setting
                String pmt_method;
                if (card_or_cash.getCheckedRadioButtonId() == R.id.r_card)
                    pmt_method = "credit_card";
                else
                    pmt_method = "cash";
                Log.d("BalanceDebug", "Selected method: " + pmt_method);

                // Transaction setting
                float transactionAmount = Float.parseFloat(amount.getText().toString());
                String frequency = repeat.getText().toString().toLowerCase();
                float fixed_income = 0.0F;
                switch (repeat.getText().toString()) {
                    case "Not repeated": frequency = "once"; break;
                    case "Daily": fixed_income = transactionAmount*30; break;
                    case "Weekly": fixed_income = transactionAmount*4; break;
                    case "Monthly": fixed_income = transactionAmount; break;
                    case "Yearly": fixed_income = transactionAmount/12; break;
                }

                Transaction transaction = new Transaction();
                transaction.setType(isExpense ? "expense" : "income");
                transaction.setAmount(transactionAmount);
                transaction.setCategory(((Category)category_spinner.getSelectedItem()).getName());
                transaction.setNote(note.getText().toString());
                transaction.setDate(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);
                transaction.setMethod(pmt_method);
                transaction.setFrequency(frequency);
                if (!"once".equals(frequency))
                    transaction.setLastExecuted(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);

                DocumentReference userRef = db.collection("Users").document(user.getUid());
                // Save transaction in FirestoreFirebase
                userRef.collection("Transactions").add(transaction)
                    .addOnSuccessListener(documentReference -> Log.d("SaveTransaction", "Saved transaction with ID: " + documentReference.getId() + " | Category " + transaction.getCategory()))
                    .addOnFailureListener(e -> Log.e("SaveTransaction", "Saving error", e));

                // Update Balance
                isFixed = !repeat.getText().toString().equals("Not repeated");
                float finalFixed_income = fixed_income;
                db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) firestoreTransaction -> {
                    DocumentSnapshot snapshot = firestoreTransaction.get(userRef);

                    Map<String, Object> methodBalance = (Map<String, Object>) snapshot.get("Balances." + pmt_method);
                    Double oldValueObj = (Double) Objects.requireNonNull(methodBalance).get("value");
                    if (oldValueObj == null)
                        throw new IllegalStateException(pmt_method + " missing 'value' field!");

                    double oldValue = oldValueObj;
                    double newValue = isExpense ? oldValue - transaction.getAmount()
                                                : oldValue + transaction.getAmount();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("Balances." + pmt_method + ".value", newValue);
                    updates.put("Balances." + pmt_method + ".date", new Date());

                    // If fixed income/expense, it updates the value_monthly field
                    if (isFixed) {
                        Map<String, Object> fixedIncomeMap = (Map<String, Object>) snapshot.get("Balances.fixed_income");

                        if (fixedIncomeMap != null) {
                            Number oldFixedIncome = (Number) fixedIncomeMap.get("value_monthly");

                            float currentFixedIncome = oldFixedIncome != null ? oldFixedIncome.floatValue() : 0f;
                            float newFixedIncome = isExpense
                                    ? currentFixedIncome - finalFixed_income
                                    : currentFixedIncome + finalFixed_income;

                            firestoreTransaction.update(userRef, "Balances.fixed_income.value_monthly", newFixedIncome);
                        }
                    }

                    firestoreTransaction.update(userRef, updates);
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    Log.d("BalanceDebug", "Balance and date successfully updated");
                    Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show();

                    context.startActivity(new Intent(context, Home.class));
                    if (context instanceof FragmentActivity)
                        ((FragmentActivity) context).finish();
                }).addOnFailureListener(e -> Log.e("BalanceDebug", "Error updating balance", e));
            }
        });
    }
}