package com.example.paydaylay.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.google.firebase.FirebaseApp;

public class LoginActivity extends BaseActivity {

    // Pola do przechowywania referencji do widoków
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewForgotPassword, textViewRegister;
    private AuthManager authManager;

    /**
     * Metoda wywoływana podczas tworzenia aktywności.
     * Inicjalizuje widoki, sprawdza, czy użytkownik jest zalogowany, 
     * oraz ustawia akcje dla przycisków i pól tekstowych.
     *
     * @param savedInstanceState Zapisany stan aktywności (jeśli istnieje).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tymczasowa inicjalizacja Firebase, jeśli nie jest jeszcze zainicjalizowany
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        setContentView(R.layout.activity_login);

        authManager = new AuthManager();

        // Sprawdzenie, czy istnieje zapisany stan sesji użytkownika
        if (authManager.isSavedSessionExists(this)) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Inicjalizacja widoków
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewRegister = findViewById(R.id.textViewRegister);

        // Ustawienie akcji dla przycisku logowania
        buttonLogin.setOnClickListener(v -> loginUser());

        // Ustawienie akcji dla linku "Zapomniałeś hasła?"
        textViewForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
        });

        // Ustawienie akcji dla linku "Zarejestruj się"
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    /**
     * Loguje użytkownika na podstawie wprowadzonych danych (email i hasło).
     * Sprawdza poprawność danych wejściowych i wywołuje metodę logowania z `AuthManager`.
     */
    private void loginUser() {
        // Pobranie danych z pól tekstowych
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Walidacja pola email
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email jest wymagany");
            return;
        }

        // Walidacja pola hasło
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Hasło jest wymagane");
            return;
        }

        // Próba zalogowania użytkownika
        authManager.loginUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // Zapisanie sesji użytkownika i przejście do głównej aktywności
                authManager.saveUserSession(authManager.getCurrentUser(), LoginActivity.this);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                // Wyświetlenie komunikatu o błędzie
                Toast.makeText(LoginActivity.this, "Błąd: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}