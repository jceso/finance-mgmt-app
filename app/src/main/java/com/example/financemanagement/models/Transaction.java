package com.example.financemanagement.models;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Transaction {
    private float amount;
    private String note, category, method;
    private String type;   // 0 = false = INCOME, 1 = true = EXPENSE
    private long date;

    public Transaction() { }

    public Transaction(float amount, String note, String category, String method, String type) {
        this.amount = amount;
        this.note = note;
        this.category = category;
        this.method = method;
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

    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
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