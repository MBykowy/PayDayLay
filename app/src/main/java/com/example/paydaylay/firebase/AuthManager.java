package com.example.paydaylay.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {
    private final FirebaseAuth auth;
    private static final String PREFS_NAME = "PayDayLayPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";

    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public AuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserSession(task.getResult().getUser(), null);
                        callback.onSuccess();
                    } else {
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserSession(task.getResult().getUser(), null);
                        callback.onSuccess();
                    } else {
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Login failed");
                    }
                });
    }


    // Add this method to your AuthManager.java
    public boolean isUserLoggedInForWidget(Context context) {
        try {
            String userId = getCurrentUserId();
            return userId != null;
        } catch (Exception e) {
            Log.e("AuthManager", "Error checking login state in widget", e);
            return false;
        }
    }


    public boolean isSavedSessionExists(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        return userId != null && !userId.isEmpty();
    }

    public void resetPassword(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Password reset failed");
                    }
                });
    }

    public void logoutUser(Context context) {
        auth.signOut();
        clearUserSession(context);
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void saveUserSession(FirebaseUser user, Context context) {
        if (user == null) return;

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_USER_ID, user.getUid())
                    .putString(KEY_USER_EMAIL, user.getEmail())
                    .apply();
        }
    }

    public void clearUserSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .apply();
    }
}