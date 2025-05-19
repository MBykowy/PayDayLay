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

public class PayDayLayApplication extends Application {
    private static final String TAG = "PayDayLayApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase early
        FirebaseApp.initializeApp(this);

        // Configure Firebase emulators in debug mode - BEFORE any other Firebase operations
        if (BuildConfig.DEBUG) {
            try {
                // Set emulator hosts for ALL Firebase services
                //FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
                //FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);

                // Disable SSL verification for emulator connections
                //Log.d(TAG, "Firebase emulators configured successfully");
            } catch (Exception e) {
                Log.e(TAG, "Firebase emulator setup failed", e);
            }
        }

        // Configure Firebase offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // Initialize Room database
        AppDatabase.getInstance(this);

        // Skip App Check initialization as it's causing issues
        // DatabaseManager.initAppCheck(this);

        // Check Google Play Services availability
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services not available. Status code: " + status);
        } else {
            Log.d(TAG, "Google Play Services available");
        }

        // Apply saved theme
        ThemeUtils.applyTheme(this);

        // Create notification channels
        NotificationUtils.createNotificationChannels(this);
    }
}