package com.example.paydaylay.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Category implements Serializable {
    @DocumentId
    private String id;
    private String name;
    private int color;
    private String userId;
    private String iconName;

    // Required empty constructor for Firestore
    public Category() {
    }

    public Category(String name, int color, String userId, String iconName) {
        this.name = name;
        this.color = color;
        this.userId = userId;
        this.iconName = iconName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("color", color);
        map.put("userId", userId);
        map.put("iconName", iconName);
        return map;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}