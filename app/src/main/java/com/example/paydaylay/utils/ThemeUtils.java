package com.example.paydaylay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Klasa pomocnicza do zarządzania motywami aplikacji.
 * Umożliwia zapisanie preferencji motywu, zastosowanie wybranego motywu oraz sprawdzenie, czy aktywny jest tryb ciemny.
 */
public class ThemeUtils {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Tryby motywu
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // Motyw systemowy
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;            // Motyw jasny
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;            // Motyw ciemny

    /**
     * Zapisuje preferencję motywu w SharedPreferences.
     *
     * @param context   Kontekst aplikacji.
     * @param themeMode Wybrany tryb motywu (np. MODE_SYSTEM, MODE_LIGHT, MODE_DARK).
     */
    public static void saveThemeMode(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }

    /**
     * Pobiera zapisany tryb motywu z SharedPreferences.
     *
     * @param context Kontekst aplikacji.
     * @return Zapisany tryb motywu (domyślnie MODE_SYSTEM).
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Zastosowuje motyw na podstawie zapisanej preferencji.
     *
     * @param context Kontekst aplikacji.
     */
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * Sprawdza, czy aktywny jest tryb ciemny.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli aktywny jest tryb ciemny, false w przeciwnym razie.
     */
    public static boolean isDarkModeActive(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Sprawdza, czy aktywny jest tryb nocny.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli aktywny jest tryb nocny, false w przeciwnym razie.
     */
    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}