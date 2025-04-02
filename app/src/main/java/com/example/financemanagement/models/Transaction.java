package com.example.financemanagement.models;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Transaction {
    private float amount;
    private String note, category;
    private boolean type;   // 0 = false = INCOME, 1 = true = EXPENSE
    private long date;

    public Transaction() { }

    public Transaction(float amount, String note, String category, boolean type) {
        this.amount = amount;
        this.note = note;
        this.category = category;
        this.type = type;
    }

    public float getAmount() {
        return amount;
    }
    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public boolean getType() {
        return type;
    }
    public void setType(boolean type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }
    public void setDate(int d, int mth, int y, int h, int min) {
        LocalDateTime localDateTime = LocalDateTime.of(y, mth, d, h, min);
        this.date = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}