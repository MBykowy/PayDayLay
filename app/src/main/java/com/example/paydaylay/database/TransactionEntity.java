// app/src/main/java/com/example/paydaylay/database/TransactionEntity.java
package com.example.paydaylay.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private double amount;
    private long dateTimestamp;
    private String categoryId;
    private String description;
    private String userId;
    private boolean isExpense;

    // Default constructor
    public TransactionEntity() {
        // Required by Room
    }

    // Parameterized constructor
    public TransactionEntity(@NonNull String id, double amount, long dateTimestamp,
                             String categoryId, String description, String userId,
                             boolean isExpense) {
        this.id = id;
        this.amount = amount;
        this.dateTimestamp = dateTimestamp;
        this.categoryId = categoryId;
        this.description = description;
        this.userId = userId;
        this.isExpense = isExpense;
    }

    // Getters and setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDateTimestamp() {
        return dateTimestamp;
    }

    public void setDateTimestamp(long dateTimestamp) {
        this.dateTimestamp = dateTimestamp;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isExpense() {
        return isExpense;
    }

    public void setExpense(boolean expense) {
        isExpense = expense;
    }

    // Conversion methods
    public static TransactionEntity fromTransaction(com.example.paydaylay.models.Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = transaction.getId();
        entity.amount = transaction.getAmount();
        entity.dateTimestamp = transaction.getDate().getTime();
        entity.categoryId = transaction.getCategoryId();
        entity.description = transaction.getDescription();
        entity.userId = transaction.getUserId();
        entity.isExpense = transaction.isExpense();
        return entity;
    }

    public com.example.paydaylay.models.Transaction toTransaction() {
        com.example.paydaylay.models.Transaction transaction = new com.example.paydaylay.models.Transaction();
        transaction.setId(id);
        transaction.setAmount(amount);
        transaction.setDate(new Date(dateTimestamp));
        transaction.setCategoryId(categoryId);
        transaction.setDescription(description);
        transaction.setUserId(userId);
        transaction.setExpense(isExpense);
        return transaction;
    }
}