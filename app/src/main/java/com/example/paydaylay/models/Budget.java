package com.example.paydaylay.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Budget implements Serializable {
    @DocumentId
    private String id;
    private double limit;
    private String categoryId; // null for overall budget
    private String userId;
    private long periodStartDate; // timestamp for period start
    private int periodType; // 0 - daily, 1 - weekly, 2 - monthly, 3 - yearly
    private long createdAt; // timestamp for creation date

    // Constants for period types
    public static final int PERIOD_DAILY = 0;
    public static final int PERIOD_WEEKLY = 1;
    public static final int PERIOD_MONTHLY = 2;
    public static final int PERIOD_YEARLY = 3;

    // Required empty constructor for Firestore
    public Budget() {
    }



    public Budget(double limit, String categoryId, String userId, long periodStartDate, int periodType) {
        this.limit = limit;
        this.categoryId = categoryId;
        this.userId = userId;
        this.periodStartDate = periodStartDate;
        this.periodType = periodType;
    }

    // Add to your toMap() method
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", limit);
        map.put("categoryId", categoryId);
        map.put("userId", userId);
        map.put("periodStartDate", periodStartDate);
        map.put("periodType", periodType);
        map.put("createdAt", createdAt);
        map.put("spent", spent);
        return map;
    }

    // Replace your existing setCreatedAt method
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Add the getter
    public long getCreatedAt() {
        return createdAt;
    }


    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(long periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public int getPeriodType() {
        return periodType;
    }

    public void setPeriodType(int periodType) {
        this.periodType = periodType;
    }
    private double spent = 0;

    /**
     * Zwraca aktualną kwotę wydaną w ramach budżetu
     * @return kwota wydana
     */
    public double getSpent() {
        return spent;
    }

    /**
     * Ustawia kwotę wydaną w ramach budżetu
     * @param spent kwota wydana
     */
    public void setSpent(double spent) {
        this.spent = spent;
    }
}