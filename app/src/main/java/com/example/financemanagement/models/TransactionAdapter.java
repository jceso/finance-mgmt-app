package com.example.financemanagement.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.financemanagement.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView category, date, amount;

        public TransactionViewHolder(View view) {
            super(view);
            category = view.findViewById(R.id.transaction_category);
            date = view.findViewById(R.id.transaction_date);
            amount = view.findViewById(R.id.transaction_amount);
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_list, parent, false);
        return new TransactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction t = transactions.get(position);
        holder.category.setText(t.getCategory());
        holder.amount.setText(String.valueOf(t.getAmount()));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
}
