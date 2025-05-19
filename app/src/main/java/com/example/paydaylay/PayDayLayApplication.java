package com.example.paydaylay;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.paydaylay.database.AppDatabase;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.utils.LocaleHelper;
import com.example.paydaylay.utils.NotificationUtils;
import com.example.paydaylay.utils.ThemeUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Główna klasa aplikacji, odpowiedzialna za inicjalizację kluczowych komponentów i konfiguracji.
 * Rozszerza klasę Application, co pozwala na wykonywanie operacji globalnych dla całej aplikacji.
 */
public class PayDayLayApplication extends Application {
    private static final String TAG = "PayDayLayApplication";

    /**
     * Metoda wywoływana przed `onCreate`, pozwala na dostosowanie kontekstu aplikacji.
     *
     * @param base Bazowy kontekst aplikacji.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    /**
     * Metoda wywoływana podczas tworzenia aplikacji.
     * Inicjalizuje Firebase, bazę danych Room, motywy, powiadomienia i inne kluczowe komponenty.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicjalizacja Firebase
        FirebaseApp.initializeApp(this);

        // Konfiguracja emulatorów Firebase w trybie debugowania
        if (BuildConfig.DEBUG) {
            try {
                // Konfiguracja emulatorów Firebase (jeśli potrzebne)
                // FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
                // FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);

                // Wyłączenie weryfikacji SSL dla połączeń z emulatorami
                // Log.d(TAG, "Firebase emulators configured successfully");
            } catch (Exception e) {
                Log.e(TAG, "Firebase emulator setup failed", e);
            }
        }

        // Konfiguracja Firebase z włączoną persystencją offline
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // Inicjalizacja bazy danych Room
        AppDatabase.getInstance(this);

        // Pominięcie inicjalizacji App Check (jeśli powoduje problemy)
        // DatabaseManager.initAppCheck(this);

        // Sprawdzenie dostępności Google Play Services
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services not available. Status code: " + status);
        } else {
            Log.d(TAG, "Google Play Services available");
        }

        // Zastosowanie zapisanego motywu
        ThemeUtils.applyTheme(this);

        // Tworzenie kanałów powiadomień
        NotificationUtils.createNotificationChannels(this);
    }
}