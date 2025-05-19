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

/**
 * Adapter BudgetAdapter obsługuje wyświetlanie listy budżetów w RecyclerView.
 * Umożliwia użytkownikowi przeglądanie szczegółów budżetów, takich jak kategoria,
 * limit, okres, wydatki oraz pozostałe środki.
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    // Lista budżetów i kategorii
    private List<Budget> budgets;
    private List<Category> categories;

    // Mapa kategorii dla szybkiego dostępu
    private Map<String, Category> categoryMap = new HashMap<>();
    private DatabaseManager databaseManager;

    /**
     * Konstruktor adaptera.
     *
     * @param budgets Lista budżetów do wyświetlenia.
     * @param categories Lista kategorii powiązanych z budżetami.
     */
    public BudgetAdapter(List<Budget> budgets, List<Category> categories) {
        this.budgets = budgets;
        this.categories = categories;
        this.databaseManager = new DatabaseManager();

        // Tworzenie mapy kategorii
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }
    }

    /**
     * Tworzy nowy widok dla elementu RecyclerView.
     *
     * @param parent Rodzic widoku.
     * @param viewType Typ widoku.
     * @return Obiekt BudgetViewHolder.
     */
    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    /**
     * Wiąże dane budżetu z widokiem.
     *
     * @param holder Obiekt BudgetViewHolder.
     * @param position Pozycja elementu w liście.
     */
    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        // Pobieranie nazwy kategorii
        String categoryName;
        if (budget.getCategoryId() == null) {
            categoryName = holder.itemView.getContext().getString(R.string.overall_budget);
        } else {
            Category category = categoryMap.get(budget.getCategoryId());
            categoryName = category != null ? category.getName() : "Unknown Category";
        }

        // Formatowanie limitu
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
        String formattedLimit = currencyFormatter.format(budget.getLimit());

        // Ustawianie typu okresu
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

        // Ustawianie widoków
        holder.textViewCategory.setText(categoryName);
        holder.textViewLimit.setText(formattedLimit);
        holder.textViewPeriodType.setText(periodType);

        // Obliczanie zakresu dat
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

        // Ładowanie danych transakcji
        loadTransactionsForBudget(holder, budget);

        // Obsługa przycisku usuwania
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
                                        // Powiadomienie o pustym stanie
                                        notifyDataSetChanged();
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                // Obsługa błędu
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Ładuje transakcje dla danego budżetu i aktualizuje widok.
     *
     * @param holder Obiekt BudgetViewHolder.
     * @param budget Obiekt budżetu.
     */
    private void loadTransactionsForBudget(BudgetViewHolder holder, Budget budget) {
        // Wyświetlanie stanu ładowania
        holder.progressBar.setProgress(0);
        holder.textViewSpent.setText("Loading...");

        // Pobieranie zakresu dat
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

        // Pobieranie transakcji
        databaseManager.getTransactionsForBudget(
                budget.getUserId(),
                budget.getCategoryId(),
                startDate,
                endCal.getTime(),
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        // Obliczanie całkowitych wydatków
                        double totalSpent = 0;
                        for (Transaction transaction : transactions) {
                            if (transaction.isExpense()) {
                                totalSpent += transaction.getAmount();
                            }
                        }

                        // Aktualizacja widoku
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
                        String formattedSpent = currencyFormatter.format(totalSpent);
                        String formattedRemaining = currencyFormatter.format(Math.max(0, budget.getLimit() - totalSpent));

                        // Obliczanie procentu
                        int percentage = budget.getLimit() > 0 ?
                                (int) Math.min(100, (totalSpent / budget.getLimit()) * 100) : 0;

                        holder.progressBar.setProgress(percentage);
                        holder.textViewSpent.setText("Spent: " + formattedSpent);
                        holder.textViewRemaining.setText("Remaining: " + formattedRemaining);

                        // Ustawianie koloru paska postępu
                        if (percentage >= 90) {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Czerwony
                        } else if (percentage >= 75) {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFFFB74D)); // Pomarańczowy
                        } else {
                            holder.progressBar.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Zielony
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        holder.textViewSpent.setText("Error loading data");
                    }
                });
    }

    /**
     * Zwraca liczbę elementów w liście.
     *
     * @return Liczba elementów.
     */
    @Override
    public int getItemCount() {
        return budgets.size();
    }

    /**
     * Klasa BudgetViewHolder przechowuje widoki dla elementu budżetu.
     */
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