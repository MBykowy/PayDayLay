package com.example.paydaylay.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;

/**
 * RegisterActivity to aktywność odpowiedzialna za rejestrację nowych użytkowników.
 * Umożliwia wprowadzenie adresu e-mail, hasła oraz potwierdzenie hasła.
 * Obsługuje walidację danych wejściowych oraz rejestrację użytkownika za pomocą AuthManager.
 */
public class RegisterActivity extends BaseActivity {

    // Pola do wprowadzania danych użytkownika
    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;

    // Przycisk do rejestracji użytkownika
    private Button buttonRegister;

    // Tekst umożliwiający przejście do ekranu logowania
    private TextView textViewLogin;

    // Obiekt zarządzający uwierzytelnianiem użytkowników
    private AuthManager authManager;

    /**
     * Wywoływane podczas tworzenia aktywności.
     * Inicjalizuje widoki, ustawia nasłuchiwacze zdarzeń oraz instancję AuthManager.
     *
     * @param savedInstanceState Zapisany stan aktywności, jeśli był wcześniej zamknięty.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new AuthManager();

        // Inicjalizacja widoków
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        // Ustawienie nasłuchiwacza dla przycisku rejestracji
        buttonRegister.setOnClickListener(v -> registerUser());

        // Ustawienie nasłuchiwacza dla tekstu powrotu do logowania
        textViewLogin.setOnClickListener(v -> {
            finish(); // Powrót do aktywności logowania
        });
    }

    /**
     * Rejestruje nowego użytkownika na podstawie wprowadzonych danych.
     * Sprawdza poprawność danych wejściowych, takich jak e-mail, hasło i potwierdzenie hasła.
     * Wyświetla odpowiednie komunikaty o błędach lub sukcesie rejestracji.
     */
    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Walidacja adresu e-mail
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email jest wymagany");
            return;
        }

        // Walidacja hasła
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Hasło jest wymagane");
            return;
        }

        // Sprawdzenie minimalnej długości hasła
        if (password.length() < 6) {
            editTextPassword.setError("Hasło musi mieć co najmniej 6 znaków");
            return;
        }

        // Sprawdzenie zgodności hasła z potwierdzeniem
        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Hasła nie są zgodne");
            return;
        }

        // Rejestracja użytkownika za pomocą AuthManager
        authManager.registerUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // Wyświetlenie komunikatu o sukcesie
                Toast.makeText(RegisterActivity.this, "Użytkownik zarejestrowany pomyślnie", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                // Wyświetlenie komunikatu o błędzie
                Toast.makeText(RegisterActivity.this, "Błąd: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}