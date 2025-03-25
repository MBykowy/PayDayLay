package com.example.paydaylay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Transaction implements Serializable {
    @DocumentId
    private String id;
    private double amount;
    private Date date;
    private String categoryId;
    private String description;
    private String userId;

    // Field to match exactly what's in Firestore
    @PropertyName("isExpense")
    private Boolean isExpense; // Use Boolean object instead of primitive

    // Required empty constructor for Firestore
    public Transaction() {
    }

    public Transaction(double amount, Date date, String categoryId, String description, String userId, boolean isExpense) {
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.description = description;
        this.userId = userId;
        this.isExpense = isExpense;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("date", new Timestamp(date));
        map.put("categoryId", categoryId);
        map.put("description", description);
        map.put("userId", userId);
        map.put("isExpense", isExpense);
        return map;
    }

    // Standard getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Explicit getters/setters for isExpense with PropertyName annotation
    @PropertyName("isExpense")
    public Boolean getIsExpense() {
        return isExpense;
    }

    @PropertyName("isExpense")
    public void setIsExpense(Boolean isExpense) {
        this.isExpense = isExpense;
    }

    // For compatibility with existing code
    @Exclude
    public boolean isExpense() {
        return isExpense != null ? isExpense : false;
    }

    @Exclude
    public void setExpense(boolean expense) {
        this.isExpense = expense;
    }
}