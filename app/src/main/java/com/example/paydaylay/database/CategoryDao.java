package com.example.paydaylay.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.paydaylay.models.CategoryEntity;

import java.util.List;

/**
 * Interfejs DAO dla operacji na tabeli kategorii w bazie danych.
 */
@Dao
public interface CategoryDao {

    /**
     * Pobiera listę kategorii dla danego użytkownika.
     *
     * @param userId Identyfikator użytkownika.
     * @return Lista obiektów CategoryEntity.
     */
    @Query("SELECT * FROM categories WHERE userId = :userId")
    List<CategoryEntity> getCategoriesByUser(String userId);

    /**
     * Wstawia listę kategorii do bazy danych.
     * Jeśli kategoria już istnieje, zostanie zastąpiona.
     *
     * @param categories Lista kategorii do wstawienia.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    /**
     * Usuwa wszystkie kategorie powiązane z danym użytkownikiem.
     *
     * @param userId Identyfikator użytkownika.
     */
    @Query("DELETE FROM categories WHERE userId = :userId")
    void deleteAllByUser(String userId);
}