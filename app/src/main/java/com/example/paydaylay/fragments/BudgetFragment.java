package com.example.paydaylay.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.paydaylay.R;
import com.example.paydaylay.adapters.BudgetAdapter;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.utils.BudgetAlarmScheduler;
import com.example.paydaylay.utils.NotificationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import com.example.paydaylay.widgets.BudgetWidgetConfigActivity;

/**
 * Fragment odpowiedzialny za zarządzanie budżetami użytkownika.
 * Wyświetla listę budżetów, umożliwia ich dodawanie, edytowanie oraz usuwanie.
 */
public class BudgetFragment extends Fragment {

    private static final String TAG = "BudgetFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewNoBudgets;
    private ProgressBar progressBarLoading;
    private SwitchMaterial switchNotifications;
    private FloatingActionButton fabAddBudget;

    private BudgetAdapter adapter;
    private List<Budget> budgets = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private BudgetAlarmScheduler alarmScheduler;

    /**
     * Tworzy widok fragmentu.
     *
     * @param inflater  Obiekt LayoutInflater do tworzenia widoków.
     * @param container Kontener, w którym znajduje się fragment.
     * @param savedInstanceState Zapisany stan fragmentu.
     * @return Widok fragmentu.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budgets, container, false);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();
        alarmScheduler = new BudgetAlarmScheduler(requireContext());

        // Inicjalizacja widoków
        recyclerView = view.findViewById(R.id.recyclerViewBudgets);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        textViewNoBudgets = view.findViewById(R.id.textViewNoBudgets);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        fabAddBudget = view.findViewById(R.id.fabAddBudget);

        // Konfiguracja RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(budgets, categories);
        recyclerView.setAdapter(adapter);

        // Konfiguracja SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadBudgets);

        // Konfiguracja FAB
        fabAddBudget.setOnClickListener(v -> showAddBudgetDialog());

        // Konfiguracja przełącznika powiadomień
        switchNotifications.setChecked(NotificationUtils.areNotificationsEnabled(requireContext()));
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationUtils.setNotificationsEnabled(requireContext(), isChecked);
            if (isChecked) {
                alarmScheduler.scheduleAlarms();
            } else {
                alarmScheduler.cancelAlarms();
            }
        });

        return view;
    }

    /**
     * Wywoływane po wznowieniu fragmentu.
     * Ładuje listę budżetów.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadBudgets();
    }

    /**
     * Ładuje listę budżetów i kategorii użytkownika.
     */
    private void loadBudgets() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        showLoading();

        // Najpierw ładuje kategorie
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);

                // Następnie ładuje budżety
                databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Budget> loadedBudgets) {
                        budgets.clear();
                        budgets.addAll(loadedBudgets);

                        updateUI();

                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        handleError("Error loading budgets: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                handleError("Error loading categories: " + e.getMessage());
            }
        });
    }

    /**
     * Wyświetla dialog do dodawania nowego budżetu.
     */
    private void showAddBudgetDialog() {
        // Tworzy dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_budget, null);
        builder.setView(dialogView);

        // Pobiera widoki dialogu
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText editTextBudgetLimit = dialogView.findViewById(R.id.editTextBudgetLimit);
        RadioGroup radioGroupPeriod = dialogView.findViewById(R.id.radioGroupPeriod);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        // Konfiguracja spinnera kategorii
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Overall Budget");

        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // Tworzy dialog
        AlertDialog dialog = builder.create();

        // Konfiguracja przycisków
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            // Walidacja danych wejściowych
            String limitText = editTextBudgetLimit.getText().toString();
            if (limitText.isEmpty()) {
                editTextBudgetLimit.setError("Please enter a limit amount");
                return;
            }

            double limit;
            try {
                limit = Double.parseDouble(limitText);
            } catch (NumberFormatException e) {
                editTextBudgetLimit.setError("Invalid amount");
                return;
            }

            if (limit <= 0) {
                editTextBudgetLimit.setError("Amount must be greater than zero");
                return;
            }

            // Pobiera wybrany okres
            int selectedPeriodId = radioGroupPeriod.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = dialogView.findViewById(selectedPeriodId);
            if (selectedRadioButton == null) {
                Toast.makeText(requireContext(), "Please select a period", Toast.LENGTH_SHORT).show();
                return;
            }

            int periodType;
            if (selectedRadioButton.getText().toString().equals("Daily")) {
                periodType = Budget.PERIOD_DAILY;
            } else if (selectedRadioButton.getText().toString().equals("Weekly")) {
                periodType = Budget.PERIOD_WEEKLY;
            } else if (selectedRadioButton.getText().toString().equals("Yearly")) {
                periodType = Budget.PERIOD_YEARLY;
            } else {
                periodType = Budget.PERIOD_MONTHLY;
            }

            // Pobiera ID kategorii
            String categoryId = null;
            int selectedCategory = spinnerCategory.getSelectedItemPosition();
            if (selectedCategory > 0) { // Pomija "Overall Budget"
                categoryId = categories.get(selectedCategory - 1).getId();
            }

            // Tworzy obiekt budżetu
            Budget budget = new Budget();
            budget.setUserId(authManager.getCurrentUserId());
            budget.setCategoryId(categoryId);
            budget.setLimit(limit);
            budget.setPeriodType(periodType);

            // Ustawia datę początkową okresu na dzisiejszy dzień o 00:00:00
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (periodType == Budget.PERIOD_WEEKLY) {
                // Ustawia na pierwszy dzień tygodnia
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            } else if (periodType == Budget.PERIOD_MONTHLY) {
                // Ustawia na pierwszy dzień miesiąca
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else if (periodType == Budget.PERIOD_YEARLY) {
                // Ustawia na pierwszy dzień roku
                calendar.set(Calendar.DAY_OF_YEAR, 1);
            }

            budget.setPeriodStartDate(calendar.getTimeInMillis());
            budget.setCreatedAt(new Date().getTime());

            // Zapisuje budżet
            saveBudget(budget);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Zapisuje budżet w bazie danych.
     *
     * @param budget Obiekt budżetu do zapisania.
     */
    private void saveBudget(Budget budget) {
        boolean isNewBudget = (budget.getId() == null || budget.getId().isEmpty());

        databaseManager.saveBudget(budget, new DatabaseManager.OnBudgetSavedListener() {
            @Override
            public void onBudgetSaved(Budget budget) {
                loadBudgets(); // Odświeża listę budżetów

                // Aktualizuje istniejące widżety
                if (getContext() != null) {
                    databaseManager.updateBudgetWidgets(requireContext());
                }

                // Jeśli to nowy budżet, sugeruje dodanie widżetu
                if (isNewBudget) {
                    showAddWidgetDialog(budget);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Failed to save budget: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Wyświetla dialog sugerujący dodanie widżetu dla nowego budżetu.
     *
     * @param budget Obiekt budżetu.
     */
    private void showAddWidgetDialog(Budget budget) {
        if (budget == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.add_widget_title)
                .setMessage(R.string.add_widget_message)
                .setPositiveButton(R.string.add_widget, (dialog, which) -> {
                    // Uruchom konfigurację widgetu
                    Intent intent = new Intent(requireContext(), BudgetWidgetConfigActivity.class);
                    // Przekazanie ID budżetu
                    intent.putExtra("budget_id", budget.getId());
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * Aktualizuje interfejs użytkownika po załadowaniu danych.
     */
    private void updateUI() {
        // Ukrywa wskaźnik ładowania
        progressBarLoading.setVisibility(View.GONE);

        if (budgets.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textViewNoBudgets.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewNoBudgets.setVisibility(View.GONE);

            // Aktualizuje adapter
            adapter = new BudgetAdapter(budgets, categories);
            recyclerView.setAdapter(adapter);
        }
    }

    /**
     * Wyświetla wskaźnik ładowania.
     */
    private void showLoading() {
        progressBarLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        textViewNoBudgets.setVisibility(View.GONE);
    }

    /**
     * Obsługuje błędy podczas ładowania danych.
     *
     * @param message Komunikat błędu.
     */
    private void handleError(String message) {
        if (getActivity() == null) return;

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        progressBarLoading.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Wyświetla stan pusty, jeśli wcześniej nie załadowano danych
        if (budgets.isEmpty()) {
            textViewNoBudgets.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}