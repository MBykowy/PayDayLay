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
import androidx.appcompat.app.AppCompatActivity;
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

public class TransactionActivity extends BaseActivity {

    private EditText editTextAmount, editTextDescription;
    private TextView textViewDate;
    private Spinner spinnerCategory;
    private Switch switchExpenseIncome;
    private Button buttonSave, buttonDelete;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private List<Category> categories;
    private Transaction currentTransaction;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        editTextAmount = findViewById(R.id.editTextAmount);
        textViewDate = findViewById(R.id.textViewDate);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchExpenseIncome = findViewById(R.id.switchExpenseIncome);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Set current date initially
        updateDateDisplay();

        // Check if we're in edit mode
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

        // Load categories
        loadCategories();

        // Set up date picker dialog
        textViewDate.setOnClickListener(v -> showDatePickerDialog());

        // Set up save button
        buttonSave.setOnClickListener(v -> saveTransaction());

        // Set up delete button
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void fillFormWithTransactionData() {
        if (currentTransaction != null) {
            editTextAmount.setText(String.valueOf(currentTransaction.getAmount()));
            calendar.setTime(currentTransaction.getDate());
            updateDateDisplay();
            editTextDescription.setText(currentTransaction.getDescription());
            switchExpenseIncome.setChecked(!currentTransaction.isExpense());
        }
    }

    private void loadCategories() {
        databaseManager.getCategories(authManager.getCurrentUserId(), new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;
                setupCategorySpinner();

                // Set the selected category if in edit mode
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
                        "Error loading categories: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void updateDateDisplay() {
        textViewDate.setText(dateFormat.format(calendar.getTime()));
    }

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
                            "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Error updating transaction: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }, TransactionActivity.this);
        } else {
            databaseManager.addTransaction(transaction, new DatabaseManager.OnTransactionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this,
                            "Transaction added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Error adding transaction: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }, TransactionActivity.this);
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        String amount = editTextAmount.getText().toString();
        if (TextUtils.isEmpty(amount)) {
            editTextAmount.setError("Required");
            valid = false;
        } else {
            try {
                double value = Double.parseDouble(amount);
                if (value <= 0) {
                    editTextAmount.setError("Must be greater than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                editTextAmount.setError("Must be a valid number");
                valid = false;
            }
        }

        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransaction() {
        if (currentTransaction != null) {
            databaseManager.deleteTransaction(currentTransaction.getId(), new DatabaseManager.OnTransactionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this,
                            "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TransactionActivity.this,
                            "Error deleting transaction: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}