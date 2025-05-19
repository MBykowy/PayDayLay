package com.example.paydaylay.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model reprezentujący kategorię użytkownika.
 * Przechowuje informacje o nazwie, kolorze, ikonie oraz przypisanym użytkowniku.
 */
public class Category implements Serializable {

    @DocumentId
    private String id; // Unikalny identyfikator kategorii
    private String name; // Nazwa kategorii
    private int color; // Kolor kategorii (reprezentowany jako int)
    private String userId; // ID użytkownika, do którego należy kategoria
    private String iconName; // Nazwa ikony przypisanej do kategorii

    /**
     * Konstruktor domyślny wymagany przez Firestore.
     */
    public Category() {
    }

    /**
     * Konstruktor tworzący kategorię z podanymi parametrami.
     *
     * @param name     Nazwa kategorii.
     * @param color    Kolor kategorii.
     * @param userId   ID użytkownika.
     * @param iconName Nazwa ikony kategorii.
     */
    public Category(String name, int color, String userId, String iconName) {
        this.name = name;
        this.color = color;
        this.userId = userId;
        this.iconName = iconName;
    }

    /**
     * Konwertuje obiekt kategorii na mapę klucz-wartość.
     *
     * @return Mapa reprezentująca kategorię.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("color", color);
        map.put("userId", userId);
        map.put("iconName", iconName);
        return map;
    }

    // Gettery i settery

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