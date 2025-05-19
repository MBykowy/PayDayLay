package com.example.paydaylay.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model reprezentujący budżet użytkownika.
 * Przechowuje informacje o limicie, kategorii, użytkowniku, okresie oraz wydatkach.
 */
public class Budget implements Serializable {

    @DocumentId
    private String id; // Unikalny identyfikator budżetu
    private double limit; // Limit budżetu
    private String categoryId; // ID kategorii (null dla budżetu ogólnego)
    private String userId; // ID użytkownika
    private long periodStartDate; // Data rozpoczęcia okresu (timestamp)
    private int periodType; // Typ okresu (0 - dzienny, 1 - tygodniowy, 2 - miesięczny, 3 - roczny)
    private long createdAt; // Data utworzenia budżetu (timestamp)
    private double spent = 0; // Kwota wydana w ramach budżetu

    // Stałe reprezentujące typy okresów
    public static final int PERIOD_DAILY = 0;
    public static final int PERIOD_WEEKLY = 1;
    public static final int PERIOD_MONTHLY = 2;
    public static final int PERIOD_YEARLY = 3;

    /**
     * Konstruktor domyślny wymagany przez Firestore.
     */
    public Budget() {
    }

    /**
     * Konstruktor tworzący budżet z podanymi parametrami.
     *
     * @param limit          Limit budżetu.
     * @param categoryId     ID kategorii (null dla budżetu ogólnego).
     * @param userId         ID użytkownika.
     * @param periodStartDate Data rozpoczęcia okresu (timestamp).
     * @param periodType     Typ okresu (0 - dzienny, 1 - tygodniowy, 2 - miesięczny, 3 - roczny).
     */
    public Budget(double limit, String categoryId, String userId, long periodStartDate, int periodType) {
        this.limit = limit;
        this.categoryId = categoryId;
        this.userId = userId;
        this.periodStartDate = periodStartDate;
        this.periodType = periodType;
    }

    /**
     * Konwertuje obiekt budżetu na mapę klucz-wartość.
     *
     * @return Mapa reprezentująca budżet.
     */
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

    // Gettery i settery

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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Zwraca aktualną kwotę wydaną w ramach budżetu.
     *
     * @return Kwota wydana.
     */
    public double getSpent() {
        return spent;
    }

    /**
     * Ustawia kwotę wydaną w ramach budżetu.
     *
     * @param spent Kwota wydana.
     */
    public void setSpent(double spent) {
        this.spent = spent;
    }
}