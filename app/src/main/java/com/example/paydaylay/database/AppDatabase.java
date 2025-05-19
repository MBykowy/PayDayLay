package com.example.paydaylay.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.paydaylay.models.TransactionEntity;
import com.example.paydaylay.models.CategoryEntity;

/**
 * Klasa AppDatabase definiuje bazę danych Room dla aplikacji PaydayLay.
 * Zawiera tabele dla transakcji i kategorii.
 */
@Database(entities = {TransactionEntity.class, CategoryEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // DAO dla transakcji
    public abstract TransactionDao transactionDao();

    // DAO dla kategorii
    public abstract CategoryDao categoryDao();

    /**
     * Pobiera instancję bazy danych.
     *
     * @param context Kontekst aplikacji.
     * @return Instancja AppDatabase.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "paydaylay_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}