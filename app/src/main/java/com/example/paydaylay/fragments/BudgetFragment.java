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
// Added correct import for SwipeRefreshLayout
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budgets, container, false);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();
        alarmScheduler = new BudgetAlarmScheduler(requireContext());

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewBudgets);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        textViewNoBudgets = view.findViewById(R.id.textViewNoBudgets);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        fabAddBudget = view.findViewById(R.id.fabAddBudget);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(budgets, categories);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBudgets();
        });

        // Setup FAB
        fabAddBudget.setOnClickListener(v -> showAddBudgetDialog());

        // Setup notification switch
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

    @Override
    public void onResume() {
        super.onResume();
        loadBudgets();
    }

    private void loadBudgets() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        showLoading();

        // First load categories
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);

                // Then load budgets
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

    private void showAddBudgetDialog() {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_budget, null);
        builder.setView(dialogView);

        // Get dialog views
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText editTextBudgetLimit = dialogView.findViewById(R.id.editTextBudgetLimit);
        RadioGroup radioGroupPeriod = dialogView.findViewById(R.id.radioGroupPeriod);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        // Setup category spinner
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

        // Create dialog
        AlertDialog dialog = builder.create();

        // Setup buttons
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            // Validate input
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

            // Get selected period
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

            // Get category ID
            String categoryId = null;
            int selectedCategory = spinnerCategory.getSelectedItemPosition();
            if (selectedCategory > 0) { // Skip "Overall Budget"
                categoryId = categories.get(selectedCategory - 1).getId();
            }

            // Create budget object
            Budget budget = new Budget();
            budget.setUserId(authManager.getCurrentUserId());
            budget.setCategoryId(categoryId);
            budget.setLimit(limit);
            budget.setPeriodType(periodType);

            // Set period start date to today at 00:00:00
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (periodType == Budget.PERIOD_WEEKLY) {
                // Set to first day of week
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            } else if (periodType == Budget.PERIOD_MONTHLY) {
                // Set to first day of month
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else if (periodType == Budget.PERIOD_YEARLY) {
                // Set to first day of year
                calendar.set(Calendar.DAY_OF_YEAR, 1);
            }

            budget.setPeriodStartDate(calendar.getTimeInMillis());
            budget.setCreatedAt(new Date().getTime());

            // Save budget
            saveBudget(budget);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveBudget(Budget budget) {
        boolean isNewBudget = (budget.getId() == null || budget.getId().isEmpty());

        databaseManager.saveBudget(budget, new DatabaseManager.OnBudgetSavedListener() {
            @Override
            public void onBudgetSaved(Budget budget) {
                loadBudgets(); // Refresh the budget list

                // Update any existing widgets
                if (getContext() != null) {
                    databaseManager.updateBudgetWidgets(requireContext());
                }

                // If this is a new budget, suggest adding a widget
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

    private void updateUI() {
        // Hide loading indicator
        progressBarLoading.setVisibility(View.GONE);

        if (budgets.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textViewNoBudgets.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewNoBudgets.setVisibility(View.GONE);

            // Update adapter
            adapter = new BudgetAdapter(budgets, categories);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showLoading() {
        progressBarLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        textViewNoBudgets.setVisibility(View.GONE);
    }

    private void handleError(String message) {
        if (getActivity() == null) return;

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        progressBarLoading.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Show empty state if no data loaded previously
        if (budgets.isEmpty()) {
            textViewNoBudgets.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}