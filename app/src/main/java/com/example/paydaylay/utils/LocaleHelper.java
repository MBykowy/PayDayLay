package com.example.paydaylay.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.paydaylay.activities.MainActivity;

import java.util.Locale;

/**
 * Klasa pomocnicza do zarządzania ustawieniami języka aplikacji.
 * Umożliwia zmianę języka, zapisanie preferencji użytkownika oraz aktualizację zasobów aplikacji.
 */
public class LocaleHelper {
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";
    private static final String DEFAULT_LANGUAGE = "en"; // Domyślny język to angielski

    /**
     * Ustawia wybrany język i restartuje aplikację, aby zastosować zmiany.
     *
     * @param context      Kontekst aplikacji.
     * @param languageCode Kod języka (np. "en", "pl").
     */
    public static void setLocale(@NonNull Context context, String languageCode) {
        boolean languageChanged = !languageCode.equals(getLanguage(context));
        if (languageChanged) {
            saveLanguage(context, languageCode);
            updateResources(context, languageCode);

            // Restartuje aplikację, aby zastosować zmiany
            if (context instanceof Activity) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    /**
     * Zapisuje preferencję języka w SharedPreferences.
     *
     * @param context      Kontekst aplikacji.
     * @param languageCode Kod języka do zapisania.
     */
    private static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /**
     * Pobiera zapisany język z preferencji.
     *
     * @param context Kontekst aplikacji.
     * @return Kod języka (np. "en", "pl").
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /**
     * Aktualizuje zasoby aplikacji na podstawie wybranego języka.
     *
     * @param context      Kontekst aplikacji.
     * @param languageCode Kod języka do ustawienia.
     * @return Zaktualizowany kontekst.
     */
    public static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    /**
     * Opatula kontekst aplikacji wybranym językiem.
     *
     * @param context Kontekst aplikacji.
     * @return Zaktualizowany kontekst.
     */
    public static Context onAttach(Context context) {
        String language = getLanguage(context);
        return updateResources(context, language);
    }
}