package com.example.paydaylay.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Klasa TransactionActivity obsługuje dodawanie, edytowanie i usuwanie transakcji.
 * Umożliwia użytkownikowi wprowadzenie szczegółów transakcji, takich jak kwota, kategoria,
 * data, opis oraz typ (wydatek/przychód).
 */
public class TransactionActivity extends BaseActivity {

    // Deklaracje pól widoków
    private EditText editTextAmount, editTextDescription;
    private TextView textViewDate;
    private Spinner spinnerCategory;
    private Switch switchExpenseIncome;
    private Button buttonSave, buttonDelete;

    // Deklaracje pól pomocniczych
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private List<Category> categories;
    private Transaction currentTransaction;
    private boolean isEditMode = false;

    /**
     * Metoda wywoływana podczas tworzenia aktywności.
     * Inicjalizuje widoki, menedżery oraz ustawia dane w przypadku edycji transakcji.
     *
     * @param savedInstanceState Zapisany stan aktywności (jeśli istnieje).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        // Inicjalizacja menedżerów i kalendarza
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Ustawienie paska narzędzi
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Inicjalizacja widoków
        editTextAmount = findViewById(R.id.editTextAmount);
        textViewDate = findViewById(R.id.textViewDate);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchExpenseIncome = findViewById(R.id.switchExpenseIncome);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Ustawienie bieżącej daty
        updateDateDisplay();

        // Sprawdzenie trybu edycji
        if (getIntent().hasExtra("transaction")) {
            currentTransaction = (Transaction) getIntent().getSerializableExtra("transaction");
            isEditMode = true;
            setTitle(R.string.edit_transaction);
            fillFormWithTransactionData();
            buttonDelete.setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.add_transaction);
            buttonDelete.setVisibility(View.GONE);
        }

        // Załadowanie kategorii
        loadCategories();

        // Ustawienie nasłuchiwaczy dla widoków
        textViewDate.setOnClickListener(v -> showDatePickerDialog());
        buttonSave.setOnClickListener(v -> saveTransaction());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Wypełnia formularz danymi transakcji w trybie edycji.
     */
    private void fillFormWithTransactionData() {
        if (currentTransaction != null) {
            editTextAmount.setText(String.valueOf(currentTransaction.getAmount()));
            calendar.setTime(currentTransaction.getDate());
            updateDateDisplay();
            editTextDescription.setText(currentTransaction.getDescription());
            switchExpenseIncome.setChecked(!currentTransaction.isExpense());
        }
    }

    /**
     * Ładuje kategorie z bazy danych i ustawia je w spinnerze.
     */
    private void loadCategories() {
        databaseManager.getCategories(authManager.getCurrentUserId(), new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;
                setupCategorySpinner();

                // Ustawienie wybranej kategorii w trybie edycji
                if (isEditMode && currentTransaction != null) {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getId().equals(currentTransaction.getCategoryId())) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(TransactionActivity.this,
                        "Błąd podczas ładowania kategorii: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Ustawia adapter dla spinnera kategorii.
     */
    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Wyświetla okno dialogowe wyboru daty.
     */
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Aktualizuje wyświetlaną datę w widoku.
     */
    private void updateDateDisplay() {
        textViewDate.setText(dateFormat.format(calendar.getTime()));
    }

    /**
     * Zapisuje transakcję do bazy danych.
     */
    private void saveTransaction() {
        if (!validateForm()) {
            return;
        }

        double amount = Double.parseDouble(editTextAmount.getText().toString());
        Date date = calendar.getTime();
        String description = editTextDescription.getText().toString();
        boolean isIncome = switchExpenseIncome.isChecked();

        Category selectedCategory = categories.get(spinnerCategory.getSelectedItemPosition());
        String categoryId = selectedCategory.getId();
        String userId = authManager.getCurrentUserId();

        Transaction transaction;
        if (isEditMode && currentTransaction != null) {
            transaction = currentTransaction;
            transaction.setAmount(amount);
            transaction.setDate(date);
            transaction.setCategoryId(categoryId);
            transaction.setDescription(description);
            transaction.setExpense(!isIncome);
        } else {
            transaction = new Transaction(amount, date, categoryId, description, userId, !isIncome);
        }

        if (isEditMode) {
            databaseManager.updateTransaction(transaction, new DatabaseManager.OnTransactionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this,
                            "Transakcja zaktualizowana pomyślnie", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Błąd podczas aktualizacji transakcji: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }, TransactionActivity.this);
        } else {
            databaseManager.addTransaction(transaction, new DatabaseManager.OnTransactionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this,
                            "Transakcja dodana pomyślnie", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Błąd podczas dodawania transakcji: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }, TransactionActivity.this);
        }
    }

    /**
     * Waliduje formularz transakcji.
     *
     * @return True, jeśli formularz jest poprawny, w przeciwnym razie false.
     */
    private boolean validateForm() {
        boolean valid = true;

        String amount = editTextAmount.getText().toString();
        if (TextUtils.isEmpty(amount)) {
            editTextAmount.setError("Wymagane");
            valid = false;
        } else {
            try {
                double value = Double.parseDouble(amount);
                if (value <= 0) {
                    editTextAmount.setError("Musi być większe od 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                editTextAmount.setError("Musi być liczbą");
                valid = false;
            }
        }

        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Najpierw utwórz kategorię", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    /**
     * Wyświetla okno dialogowe potwierdzenia usunięcia transakcji.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Usuń transakcję")
                .setMessage("Czy na pewno chcesz usunąć tę transakcję?")
                .setPositiveButton("Usuń", (dialog, which) -> deleteTransaction())
                .setNegativeButton("Anuluj", null)
                .show();
    }

    /**
     * Usuwa transakcję z bazy danych.
     */
    private void deleteTransaction() {
        if (currentTransaction != null) {
            databaseManager.deleteTransaction(currentTransaction.getId(), new DatabaseManager.OnTransactionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this,
                            "Transakcja usunięta pomyślnie", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Błąd podczas usuwania transakcji: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Obsługuje wybór elementów menu.
     *
     * @param item Wybrany element menu.
     * @return True, jeśli zdarzenie zostało obsłużone, w przeciwnym razie false.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}