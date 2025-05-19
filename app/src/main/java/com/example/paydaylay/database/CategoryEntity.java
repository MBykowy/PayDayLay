// app/src/main/java/com/example/paydaylay/database/CategoryEntity.java
package com.example.paydaylay.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private int color;
    private String userId;

    // Default constructor
    public CategoryEntity() {
        // Required by Room
    }

    // Parameterized constructor
    public CategoryEntity(@NonNull String id, String name, int color, String userId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.userId = userId;
    }

    // Getters and setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Conversion methods
    public static CategoryEntity fromCategory(com.example.paydaylay.models.Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = category.getId();
        entity.name = category.getName();
        entity.color = category.getColor();
        entity.userId = category.getUserId();
        return entity;
    }

    public com.example.paydaylay.models.Category toCategory() {
        com.example.paydaylay.models.Category category = new com.example.paydaylay.models.Category();
        category.setId(id);
        category.setName(name);
        category.setColor(color);
        category.setUserId(userId);
        return category;
    }
}