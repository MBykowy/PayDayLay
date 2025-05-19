package com.example.paydaylay.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.adapters.BudgetSelectionAdapter;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.utils.AlarmPermissionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Aktywność konfiguracji widżetu budżetu.
 * Umożliwia użytkownikowi wybór budżetu, który ma być wyświetlany w widżecie.
 */
public class BudgetWidgetConfigActivity extends AppCompatActivity implements BudgetSelectionAdapter.OnBudgetSelectedListener {

    private static final String TAG = "BudgetWidgetConfig";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private DatabaseManager databaseManager;
    private AuthManager authManager;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textViewNoBudgets;
    private BudgetSelectionAdapter adapter;
    private List<Budget> budgets = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_config);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Inicjalizacja widoków
        recyclerView = findViewById(R.id.recyclerViewBudgetSelection);
        progressBar = findViewById(R.id.progressBarWidgetConfig);
        textViewNoBudgets = findViewById(R.id.textViewNoBudgetsWidget);

        // Konfiguracja RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BudgetSelectionAdapter(this, budgets, categories);
        adapter.setOnBudgetSelectedListener(this);
        recyclerView.setAdapter(adapter);

        // Sprawdź uprawnienia do alarmów
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!AlarmPermissionHelper.checkAlarmPermission(this)) {
                AlarmPermissionHelper.requestAlarmPermission(this);
            }
        }

        // Pobieranie ID widgetu z intencji
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Sprawdzenie, czy użytkownik jest zalogowany
        if (!authManager.isUserLoggedInForWidget(this)) {
            Toast.makeText(this, R.string.widget_login_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pobieranie budżetów
        loadBudgetsForWidget();
    }

    private void loadBudgetsForWidget() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            showError(getString(R.string.not_logged_in));
            return;
        }

        showLoading(true);

        // Najpierw pobierz kategorie
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);

                // Następnie pobierz budżety
                databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Budget> loadedBudgets) {
                        showLoading(false);
                        budgets.clear();
                        budgets.addAll(loadedBudgets);
                        updateUI();
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        showError("Błąd podczas ładowania budżetów: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                showError("Błąd podczas ładowania kategorii: " + e.getMessage());
            }
        });
    }

    @Override
    public void onBudgetSelected(Budget budget) {
        // Zapisz ID budżetu dla tego widgetu
        BudgetWidgetProvider.saveBudgetIdPref(this, appWidgetId, budget.getId());

        // Zapisz dane budżetu w SharedPreferences
        BudgetWidgetDataHelper dataHelper = new BudgetWidgetDataHelper(this);
        dataHelper.saveBudgetForWidget(appWidgetId, budget);

        // Aktualizuj widget z wybranym budżetem
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        BudgetWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);

        // Zakończ aktywność z powodzeniem
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void updateUI() {
        if (budgets.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textViewNoBudgets.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewNoBudgets.setVisibility(View.GONE);
            adapter = new BudgetSelectionAdapter(this, budgets, categories);
            adapter.setOnBudgetSelectedListener(this);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textViewNoBudgets.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
        textViewNoBudgets.setText(R.string.error_loading_budgets);
        textViewNoBudgets.setVisibility(View.VISIBLE);
    }
}