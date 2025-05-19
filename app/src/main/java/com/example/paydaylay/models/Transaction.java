package com.example.paydaylay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model reprezentujący transakcję użytkownika.
 * Przechowuje informacje o kwocie, dacie, kategorii, opisie, użytkowniku oraz typie transakcji (wydatek/przychód).
 */
public class Transaction implements Serializable {

    @DocumentId
    private String id; // Unikalny identyfikator transakcji
    private double amount; // Kwota transakcji
    private Date date; // Data transakcji
    private String categoryId; // ID kategorii przypisanej do transakcji
    private String description; // Opis transakcji
    private String userId; // ID użytkownika, do którego należy transakcja

    // Pole odpowiadające strukturze w Firestore
    @PropertyName("isExpense")
    private Boolean isExpense; // Typ transakcji: true - wydatek, false - przychód

    /**
     * Konstruktor domyślny wymagany przez Firestore.
     */
    public Transaction() {
    }

    /**
     * Konstruktor tworzący transakcję z podanymi parametrami.
     *
     * @param amount      Kwota transakcji.
     * @param date        Data transakcji.
     * @param categoryId  ID kategorii przypisanej do transakcji.
     * @param description Opis transakcji.
     * @param userId      ID użytkownika, do którego należy transakcja.
     * @param isExpense   Typ transakcji: true - wydatek, false - przychód.
     */
    public Transaction(double amount, Date date, String categoryId, String description, String userId, boolean isExpense) {
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.description = description;
        this.userId = userId;
        this.isExpense = isExpense;
    }

    /**
     * Konwertuje obiekt transakcji na mapę klucz-wartość.
     *
     * @return Mapa reprezentująca transakcję.
     */
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

    // Gettery i settery

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    /**
     * Zwraca typ transakcji (wydatek/przychód) jako obiekt Boolean.
     *
     * @return True, jeśli transakcja jest wydatkiem, false w przeciwnym razie.
     */
    @PropertyName("isExpense")
    public Boolean getIsExpense() {
        return isExpense;
    }

    /**
     * Ustawia typ transakcji (wydatek/przychód).
     *
     * @param isExpense True, jeśli transakcja jest wydatkiem, false w przeciwnym razie.
     */
    @PropertyName("isExpense")
    public void setIsExpense(Boolean isExpense) {
        this.isExpense = isExpense;
    }

    /**
     * Zwraca typ transakcji jako wartość prymitywną boolean.
     * Używane dla kompatybilności z istniejącym kodem.
     *
     * @return True, jeśli transakcja jest wydatkiem, false w przeciwnym razie.
     */
    @Exclude
    public boolean isExpense() {
        return isExpense != null ? isExpense : false;
    }

    /**
     * Ustawia typ transakcji jako wartość prymitywną boolean.
     * Używane dla kompatybilności z istniejącym kodem.
     *
     * @param expense True, jeśli transakcja jest wydatkiem, false w przeciwnym razie.
     */
    @Exclude
    public void setExpense(boolean expense) {
        this.isExpense = expense;
    }
}