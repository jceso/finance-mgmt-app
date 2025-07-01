package com.example.financemanagement.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Transaction {
    public static final String FREQ_ONCE = "once";
    public static final String FREQ_DAILY = "daily";
    public static final String FREQ_MONTHLY = "monthly";
    public static final String FREQ_YEARLY = "yearly";

    private float amount;
    private String note, category, method, frequency;
    private String type;   // 0 = false = INCOME, 1 = true = EXPENSE
    private long date, lastExecuted;

    public Transaction() { }

    public Transaction(float amount, String note, String category, String method, String type, String frequency) {
        this.amount = amount;
        this.note = note;
        this.category = category;
        this.method = method;
        this.type = type;
        this.frequency = frequency;
    }

    public String getFormatted(long dateToFormat) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateToFormat), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return dateTime.format(formatter);
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

    public String getFrequency() {
        return frequency;
    }
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public long getDate() {
        return date;
    }
    public void setDate(int d, int mth, int y, int h, int min) {
        LocalDateTime localDateTime = LocalDateTime.of(y, mth, d, h, min);
        this.date = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }




    public boolean isRecurring() {
        return !FREQ_ONCE.equals(frequency);
    }

    public long getLastExecuted() {
        return lastExecuted;
    }
    public void setLastExecuted(int d, int mth, int y, int h, int min) {
        LocalDateTime localDateTime = LocalDateTime.of(y, mth, d, h, min);
        this.lastExecuted = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}