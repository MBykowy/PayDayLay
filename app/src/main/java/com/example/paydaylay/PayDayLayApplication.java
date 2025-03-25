package com.example.paydaylay;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.paydaylay.utils.LocaleHelper;
import com.example.paydaylay.utils.NotificationUtils;
import com.example.paydaylay.utils.ThemeUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class PayDayLayApplication extends Application {
    private static final String TAG = "PayDayLayApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Check Google Play Services availability
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services not available. Status code: " + status);
            // Can't show dialog here as we need activity context
            // Services requiring Play Services may not function correctly
        } else {
            Log.d(TAG, "Google Play Services available");
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Configure Firebase emulators in debug mode
        if (BuildConfig.DEBUG) {
            try {
                FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
                Log.d("FirebaseInit", "Firebase emulators configured");
            } catch (Exception e) {
                Log.e("FirebaseInit", "Firebase emulator setup failed", e);
            }
        }
        Log.d("FirebaseInit", "Firebase initialized successfully");

        // Apply saved theme
        ThemeUtils.applyTheme(this);

        // Create notification channels
        NotificationUtils.createNotificationChannels(this);
    }
}