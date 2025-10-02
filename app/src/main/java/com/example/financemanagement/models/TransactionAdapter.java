package com.example.financemanagement.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.financemanagement.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.Locale;

public class TransactionAdapter extends FirestoreRecyclerAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    public TransactionAdapter(@NonNull FirestoreRecyclerOptions<Transaction> options) {
        super(options);
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView category, date, amount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.transaction_category);
            date = itemView.findViewById(R.id.transaction_date);
            amount = itemView.findViewById(R.id.transaction_amount);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull TransactionViewHolder holder, int position, @NonNull Transaction model) {
        String textAmount;
        String categoryText = model.getCategory();
        categoryText = categoryText.substring(0, 1).toUpperCase() + categoryText.substring(1).toLowerCase();

        holder.category.setText(categoryText);
        holder.date.setText(model.getFormatted(model.getDate()));
        if (model.getType().equals("expense")) {
            textAmount = String.format(Locale.getDefault(), "- %.2f" , model.getAmount());
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.negative));
        } else {
            textAmount = String.format(Locale.getDefault(), "+ %.2f" , model.getAmount());
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.positive));
        }
        holder.amount.setText(textAmount);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_list, parent, false);
        return new TransactionViewHolder(v);
    }
}