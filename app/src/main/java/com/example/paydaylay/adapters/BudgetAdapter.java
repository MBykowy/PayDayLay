package com.example.paydaylay.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.widgets.BudgetWidgetProvider;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgets;
    private List<Category> categories;
    private Map<String, Category> categoryMap = new HashMap<>();
    private DatabaseManager databaseManager;

    public BudgetAdapter(List<Budget> budgets, List<Category> categories) {
        this.budgets = budgets;
        this.categories = categories;
        this.databaseManager = new DatabaseManager();

        // Create category map for quick lookups
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        // Get category name
        String categoryName;
        if (budget.getCategoryId() == null) {
            categoryName = holder.itemView.getContext().getString(R.string.overall_budget);
        } else {
            Category category = categoryMap.get(budget.getCategoryId());
            categoryName = category != null ? category.getName() : "Unknown Category";
        }

        // Format amount
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
        String formattedLimit = currencyFormatter.format(budget.getLimit());

        // Set period type
        String periodType;
        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                periodType = holder.itemView.getContext().getString(R.string.daily);
                break;
            case Budget.PERIOD_WEEKLY:
                periodType = holder.itemView.getContext().getString(R.string.weekly);
                break;
            case Budget.PERIOD_YEARLY:
                periodType = holder.itemView.getContext().getString(R.string.yearly);
                break;
            default:
                periodType = holder.itemView.getContext().getString(R.string.monthly);
                break;
        }

        // Setup view
        holder.textViewCategory.setText(categoryName);
        holder.textViewLimit.setText(formattedLimit);
        holder.textViewPeriodType.setText(periodType);

        // Calculate date range
        Date startDate = new Date(budget.getPeriodStartDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(budget.getPeriodStartDate());

        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                endCal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Budget.PERIOD_WEEKLY:
                endCal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case Budget.PERIOD_MONTHLY:
                endCal.add(Calendar.MONTH, 1);
                break;
            case Budget.PERIOD_YEARLY:
                endCal.add(Calendar.YEAR, 1);
                break;
        }

        String dateRange = dateFormat.format(startDate) + " - " + dateFormat.format(endCal.getTime());
        holder.textViewDateRange.setText(dateRange);

        // Load transaction data to show current progress
        loadTransactionsForBudget(holder, budget);

        // Delete button click
        holder.buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Budget")
                    .setMessage("Are you sure you want to delete this budget?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        databaseManager.deleteBudget(budget, new DatabaseManager.OnCompletionListener() {
                            @Override
                            public void onSuccess() {
                                int pos = budgets.indexOf(budget);
                                if (pos != -1) {
                                    budgets.remove(pos);
                                    notifyItemRemoved(pos);

                                    if (budgets.isEmpty()) {
                                        // Notify fragment to show empty state
                                        notifyDataSetChanged();
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                // Show error message
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadTransactionsForBudget(BudgetViewHolder holder, Budget budget) {
        // Show loading state
        holder.progressBar.setProgress(0);
        holder.textViewSpent.setText("Loading...");

        // Get date range
        Date startDate = new Date(budget.getPeriodStartDate());
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(budget.getPeriodStartDate());

        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                endCal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Budget.PERIOD_WEEKLY:
                endCal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case Budget.PERIOD_MONTHLY:
                endCal.add(Calendar.MONTH, 1);
                break;
            case Budget.PERIOD_YEARLY:
                endCal.add(Calendar.YEAR, 1);
                break;
        }

        // Query transactions
        databaseManager.getTransactionsForBudget(
                budget.getUserId(),
                budget.getCategoryId(),
                startDate,
                endCal.getTime(),
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        // Calculate total spending
                        double totalSpent = 0;
                        for (Transaction transaction : transactions) {
                            if (transaction.isExpense()) {
                                totalSpent += transaction.getAmount();
                            }
                        }

                        // Update UI
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
                        String formattedSpent = currencyFormatter.format(totalSpent);
                        String formattedRemaining = currencyFormatter.format(Math.max(0, budget.getLimit() - totalSpent));

                        // Calculate percentage
                        int percentage = budget.getLimit() > 0 ?
                                (int) Math.min(100, (totalSpent / budget.getLimit()) * 100) : 0;

                        holder.progressBar.setProgress(percentage);
                        holder.textViewSpent.setText("Spent: " + formattedSpent);
                        holder.textViewRemaining.setText("Remaining: " + formattedRemaining);

                        // Set progress color based on percentage
                        if (percentage >= 90) {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Red
                        } else if (percentage >= 75) {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFFFB74D)); // Orange
                        } else {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        holder.textViewSpent.setText("Error loading data");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCategory;
        TextView textViewLimit;
        TextView textViewPeriodType;
        TextView textViewDateRange;
        TextView textViewSpent;
        TextView textViewRemaining;
        ProgressBar progressBar;
        ImageButton buttonDelete;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategory = itemView.findViewById(R.id.textViewCategory);
            textViewLimit = itemView.findViewById(R.id.textViewLimit);
            textViewPeriodType = itemView.findViewById(R.id.textViewPeriodType);
            textViewDateRange = itemView.findViewById(R.id.textViewDateRange);
            textViewSpent = itemView.findViewById(R.id.textViewSpent);
            textViewRemaining = itemView.findViewById(R.id.textViewRemaining);
            progressBar = itemView.findViewById(R.id.progressBarBudget);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}