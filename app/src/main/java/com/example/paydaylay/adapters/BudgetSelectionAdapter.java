package com.example.paydaylay.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;

import java.text.NumberFormat;
import java.util.List;

public class BudgetSelectionAdapter extends RecyclerView.Adapter<BudgetSelectionAdapter.BudgetViewHolder> {

    private final Context context;
    private final List<Budget> budgets;
    private final List<Category> categories;
    private OnBudgetSelectedListener listener;

    public interface OnBudgetSelectedListener {
        void onBudgetSelected(Budget budget);
    }

    public BudgetSelectionAdapter(Context context, List<Budget> budgets, List<Category> categories) {
        this.context = context;
        this.budgets = budgets;
        this.categories = categories;
    }

    public void setOnBudgetSelectedListener(OnBudgetSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_selection, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        // Ustawienie nazwy kategorii lub "Ogólny budżet"
        String categoryName = context.getString(R.string.overall_budget);
        if (budget.getCategoryId() != null) {
            for (Category category : categories) {
                if (budget.getCategoryId().equals(category.getId())) {
                    categoryName = category.getName();
                    break;
                }
            }
        }

        holder.textViewCategory.setText(categoryName);

        // Formatowanie kwoty budżetu
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        holder.textViewAmount.setText(currencyFormat.format(budget.getLimit()));

        // Określenie okresu budżetu
        String periodText = "";
        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                periodText = context.getString(R.string.daily);
                break;
            case Budget.PERIOD_WEEKLY:
                periodText = context.getString(R.string.weekly);
                break;
            case Budget.PERIOD_MONTHLY:
                periodText = context.getString(R.string.monthly);
                break;
            case Budget.PERIOD_YEARLY:
                periodText = context.getString(R.string.yearly);
                break;
        }

        holder.textViewPeriod.setText(periodText);

        // Ustawienie akcji kliknięcia
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBudgetSelected(budget);
            }
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCategory;
        TextView textViewAmount;
        TextView textViewPeriod;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategory = itemView.findViewById(R.id.textViewBudgetSelectionCategory);
            textViewAmount = itemView.findViewById(R.id.textViewBudgetSelectionAmount);
            textViewPeriod = itemView.findViewById(R.id.textViewBudgetSelectionPeriod);
        }
    }
}