package com.example.paydaylay.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;

/**
 * ResetPasswordActivity to aktywność odpowiedzialna za resetowanie hasła użytkownika.
 * Umożliwia wprowadzenie adresu e-mail, na który zostanie wysłany link do zresetowania hasła.
 */
public class ResetPasswordActivity extends BaseActivity {

    // Pole do wprowadzania adresu e-mail użytkownika
    private EditText editTextEmail;

    // Przycisk do wysłania żądania resetowania hasła
    private Button buttonReset;

    // Obiekt zarządzający uwierzytelnianiem użytkowników
    private AuthManager authManager;

    /**
     * Wywoływane podczas tworzenia aktywności.
     * Inicjalizuje widoki, ustawia nasłuchiwacze zdarzeń oraz instancję AuthManager.
     *
     * @param savedInstanceState Zapisany stan aktywności, jeśli była wcześniej zamknięta.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        authManager = new AuthManager();

        // Inicjalizacja widoków
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonReset = findViewById(R.id.buttonReset);

        // Ustawienie nasłuchiwacza dla przycisku resetowania hasła
        buttonReset.setOnClickListener(v -> resetPassword());
    }

    /**
     * Obsługuje proces resetowania hasła użytkownika.
     * Sprawdza poprawność wprowadzonego adresu e-mail i wysyła żądanie resetowania hasła.
     * Wyświetla odpowiednie komunikaty o błędach lub sukcesie.
     */
    private void resetPassword() {
        String email = editTextEmail.getText().toString().trim();

        // Walidacja adresu e-mail
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        // Wysłanie żądania resetowania hasła za pomocą AuthManager
        authManager.resetPassword(email, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // Wyświetlenie komunikatu o sukcesie
                Toast.makeText(ResetPasswordActivity.this, "Reset email sent", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                // Wyświetlenie komunikatu o błędzie
                Toast.makeText(ResetPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}