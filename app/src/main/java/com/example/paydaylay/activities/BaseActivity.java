package com.example.paydaylay.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.paydaylay.utils.LocaleHelper;

/**
 * BaseActivity to abstrakcyjna klasa bazowa dla wszystkich aktywności w aplikacji.
 * Zapewnia wsparcie dla zmiany języka aplikacji na podstawie preferencji użytkownika.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Dołącza kontekst bazowy z odpowiednimi ustawieniami lokalizacji.
     *
     * @param base bazowy kontekst aplikacji
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    /**
     * Wywoływane podczas uruchamiania aktywności. Przywraca zapisane ustawienie języka.
     *
     * @param savedInstanceState Jeżeli aktywność jest ponownie inicjowana po wcześniejszym zamknięciu,
     *                           ten Bundle zawiera dane, które zostały ostatnio zapisane w onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Zastosowanie zapisanego języka
        LocaleHelper.onAttach(this);
    }
}