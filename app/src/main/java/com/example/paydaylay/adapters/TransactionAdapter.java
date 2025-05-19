package com.example.paydaylay.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.TransactionActivity;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter TransactionAdapter obsługuje wyświetlanie listy transakcji w RecyclerView.
 * Umożliwia użytkownikowi przeglądanie szczegółów transakcji, takich jak opis, kwota,
 * data oraz kategoria.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;
    private final Map<String, Category> categoryMap;
    private final SimpleDateFormat dateFormat;
    private final String currencySymbol;

    /**
     * Konstruktor adaptera.
     *
     * @param context Kontekst aplikacji.
     * @param transactions Lista transakcji do wyświetlenia.
     * @param categories Lista kategorii powiązanych z transakcjami.
     */
    public TransactionAdapter(Context context, List<Transaction> transactions, List<Category> categories) {
        this.context = context;
        this.transactions = transactions;
        this.categoryMap = new HashMap<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencySymbol = context.getString(R.string.currency_symbol);

        // Tworzenie mapy kategorii dla szybkiego dostępu
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }
    }

    /**
     * Tworzy nowy widok dla elementu RecyclerView.
     *
     * @param parent Rodzic widoku.
     * @param viewType Typ widoku.
     * @return Obiekt TransactionViewHolder.
     */
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    /**
     * Wiąże dane transakcji z widokiem.
     *
     * @param holder Obiekt TransactionViewHolder.
     * @param position Pozycja elementu w liście.
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        Category category = categoryMap.get(transaction.getCategoryId());

        // Ustawienie opisu transakcji
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            holder.textViewTransactionDescription.setText(transaction.getDescription());
        } else {
            holder.textViewTransactionDescription.setText(R.string.no_description);
        }

        // Ustawienie kwoty transakcji
        String amountText = (transaction.isExpense() ? "- " : "+ ") +
                String.format(Locale.getDefault(), "%.2f %s",
                        transaction.getAmount(), currencySymbol);
        holder.textViewTransactionAmount.setText(amountText);
        holder.textViewTransactionAmount.setTextColor(
                transaction.isExpense() ?
                        context.getResources().getColor(R.color.expense_color) :
                        context.getResources().getColor(R.color.income_color));

        // Ustawienie daty transakcji
        holder.textViewTransactionDate.setText(dateFormat.format(transaction.getDate()));

        // Ustawienie informacji o kategorii
        if (category != null) {
            holder.textViewCategoryName.setText(category.getName());
            holder.viewCategoryColor.setBackgroundColor(category.getColor());
        } else {
            holder.textViewCategoryName.setText(R.string.unknown_category);
            holder.viewCategoryColor.setBackgroundColor(Color.GRAY);
        }

        // Ustawienie nasłuchiwacza kliknięcia
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TransactionActivity.class);
            intent.putExtra("transaction", transaction);
            context.startActivity(intent);
        });
    }

    /**
     * Zwraca liczbę elementów w liście.
     *
     * @return Liczba elementów.
     */
    @Override
    public int getItemCount() {
        return transactions.size();
    }

    /**
     * Aktualizuje dane transakcji i kategorii oraz odświeża widok.
     *
     * @param newTransactions Nowa lista transakcji.
     * @param newCategories Nowa lista kategorii.
     */
    public void updateData(List<Transaction> newTransactions, List<Category> newCategories) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);

        this.categoryMap.clear();
        for (Category category : newCategories) {
            categoryMap.put(category.getId(), category);
        }

        notifyDataSetChanged();
    }

    /**
     * Klasa TransactionViewHolder przechowuje widoki dla elementu transakcji.
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryColor;
        TextView textViewTransactionDescription;
        TextView textViewTransactionDate;
        TextView textViewCategoryName;
        TextView textViewTransactionAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            textViewTransactionDescription = itemView.findViewById(R.id.textViewTransactionDescription);
            textViewTransactionDate = itemView.findViewById(R.id.textViewTransactionDate);
            textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
            textViewTransactionAmount = itemView.findViewById(R.id.textViewTransactionAmount);
        }
    }
}
