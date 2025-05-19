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

/**
 * Klasa AuthManager zarządza uwierzytelnianiem użytkowników za pomocą Firebase Authentication.
 * Oferuje funkcje rejestracji, logowania, resetowania hasła oraz zarządzania sesją użytkownika.
 */
public class AuthManager {
    private final FirebaseAuth auth;
    private static final String PREFS_NAME = "PayDayLayPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";

    /**
     * Interfejs zwrotny do obsługi wyników operacji uwierzytelniania.
     */
    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Konstruktor klasy AuthManager.
     * Inicjalizuje instancję FirebaseAuth.
     */
    public AuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Rejestruje nowego użytkownika za pomocą podanego e-maila i hasła.
     *
     * @param email    Adres e-mail użytkownika.
     * @param password Hasło użytkownika.
     * @param callback Interfejs zwrotny do obsługi wyniku operacji.
     */
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

    /**
     * Loguje użytkownika za pomocą podanego e-maila i hasła.
     *
     * @param email    Adres e-mail użytkownika.
     * @param password Hasło użytkownika.
     * @param callback Interfejs zwrotny do obsługi wyniku operacji.
     */
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

    /**
     * Sprawdza, czy użytkownik jest zalogowany w kontekście widżetu.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli użytkownik jest zalogowany, w przeciwnym razie false.
     */
    public boolean isUserLoggedInForWidget(Context context) {
        try {
            String userId = getCurrentUserId();
            return userId != null;
        } catch (Exception e) {
            Log.e("AuthManager", "Error checking login state in widget", e);
            return false;
        }
    }

    /**
     * Sprawdza, czy istnieje zapisana sesja użytkownika.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli sesja istnieje, w przeciwnym razie false.
     */
    public boolean isSavedSessionExists(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        return userId != null && !userId.isEmpty();
    }

    /**
     * Resetuje hasło użytkownika, wysyłając e-mail resetujący.
     *
     * @param email    Adres e-mail użytkownika.
     * @param callback Interfejs zwrotny do obsługi wyniku operacji.
     */
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

    /**
     * Wylogowuje użytkownika i czyści zapisaną sesję.
     *
     * @param context Kontekst aplikacji.
     */
    public void logoutUser(Context context) {
        auth.signOut();
        clearUserSession(context);
    }

    /**
     * Pobiera identyfikator aktualnie zalogowanego użytkownika.
     *
     * @return Identyfikator użytkownika lub null, jeśli użytkownik nie jest zalogowany.
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Pobiera obiekt aktualnie zalogowanego użytkownika.
     *
     * @return Obiekt FirebaseUser lub null, jeśli użytkownik nie jest zalogowany.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Sprawdza, czy użytkownik jest zalogowany.
     *
     * @return True, jeśli użytkownik jest zalogowany, w przeciwnym razie false.
     */
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Zapisuje sesję użytkownika w preferencjach aplikacji.
     *
     * @param user    Obiekt FirebaseUser reprezentujący użytkownika.
     * @param context Kontekst aplikacji.
     */
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

    /**
     * Czyści zapisaną sesję użytkownika w preferencjach aplikacji.
     *
     * @param context Kontekst aplikacji.
     */
    public void clearUserSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .apply();
    }
}