package com.example.paydaylay.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;

import java.util.ArrayList;
import java.util.List;

public class BudgetWidgetConfigActivity extends AppCompatActivity {

    private static final String PREFS_WIDGET_CATEGORY = "widget_category_";
    private static final String PREFS_WIDGET_BUDGET_TYPE = "widget_budget_type_";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Spinner spinnerCategory;
    private RadioGroup radioGroupBudgetType;
    private Button buttonSave;

    private AuthManager authManager;
    private DatabaseManager databaseManager;
    private List<Category> categories;
    private List<String> categoryNames;
    private List<String> categoryIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        // Set result CANCELED in case user backs out
        setResult(RESULT_CANCELED);

        // Find the widget id from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an invalid widget ID, exit
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Initialize views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        radioGroupBudgetType = findViewById(R.id.radioGroupBudgetType);
        buttonSave = findViewById(R.id.buttonSave);

        authManager = new AuthManager();
        databaseManager = new DatabaseManager();
        categories = new ArrayList<>();
        categoryNames = new ArrayList<>();
        categoryIds = new ArrayList<>();

        // Add "Overall Budget" option
        categoryNames.add(getString(R.string.overall_budget));
        categoryIds.add(null);

        // Check if user is logged in
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load categories
        loadCategories(userId);

        // Save button click
        buttonSave.setOnClickListener(v -> saveWidgetConfiguration());
    }

    private void loadCategories(String userId) {
        // Show loading indication
        spinnerCategory.setEnabled(false);
        buttonSave.setEnabled(false);

        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.addAll(loadedCategories);

                for (Category category : loadedCategories) {
                    categoryNames.add(category.getName());
                    categoryIds.add(category.getId());
                }

                // Set up spinner adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        BudgetWidgetConfigActivity.this,
                        android.R.layout.simple_spinner_item,
                        categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);

                // Enable controls
                spinnerCategory.setEnabled(true);
                buttonSave.setEnabled(true);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(BudgetWidgetConfigActivity.this,
                        getString(R.string.error_loading_categories),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void saveWidgetConfiguration() {
        // Get selected category
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        String selectedCategoryId = categoryIds.get(selectedPosition);

        // Get selected budget type
        int budgetType;
        int checkedId = radioGroupBudgetType.getCheckedRadioButtonId();
        if (checkedId == R.id.radioButtonDaily) {
            budgetType = Budget.PERIOD_DAILY;
        } else if (checkedId == R.id.radioButtonWeekly) {
            budgetType = Budget.PERIOD_WEEKLY;
        } else if (checkedId == R.id.radioButtonYearly) {
            budgetType = Budget.PERIOD_YEARLY;
        } else {
            budgetType = Budget.PERIOD_MONTHLY;
        }

        // Save preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFS_WIDGET_CATEGORY + appWidgetId, selectedCategoryId);
        editor.putInt(PREFS_WIDGET_BUDGET_TYPE + appWidgetId, budgetType);
        editor.apply();


        // Update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        Intent updateIntent = new Intent(this, BudgetWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        sendBroadcast(updateIntent);

        // Set the result as success
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}