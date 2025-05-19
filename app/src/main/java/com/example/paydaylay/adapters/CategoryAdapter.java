package com.example.paydaylay.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.models.Category;

import java.util.List;

/**
 * Adapter CategoryAdapter obsługuje wyświetlanie listy kategorii w RecyclerView.
 * Umożliwia użytkownikowi przeglądanie i wybieranie kategorii.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    // Lista kategorii
    private final List<Category> categories;

    // Kontekst aplikacji
    private final Context context;

    // Listener obsługujący kliknięcia na kategorie
    private final OnCategoryClickListener listener;

    /**
     * Interfejs definiujący akcję po kliknięciu na kategorię.
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    /**
     * Konstruktor adaptera.
     *
     * @param context Kontekst aplikacji.
     * @param categories Lista kategorii do wyświetlenia.
     * @param listener Listener obsługujący kliknięcia na kategorie.
     */
    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    /**
     * Tworzy nowy widok dla elementu RecyclerView.
     *
     * @param parent Rodzic widoku.
     * @param viewType Typ widoku.
     * @return Obiekt CategoryViewHolder.
     */
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    /**
     * Wiąże dane kategorii z widokiem.
     *
     * @param holder Obiekt CategoryViewHolder.
     * @param position Pozycja elementu w liście.
     */
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position);
    }

    /**
     * Zwraca liczbę elementów w liście.
     *
     * @return Liczba elementów.
     */
    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * Aktualizuje listę kategorii i odświeża widok.
     *
     * @param newCategories Nowa lista kategorii.
     */
    public void updateCategories(List<Category> newCategories) {
        this.categories.clear();
        this.categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    /**
     * Klasa CategoryViewHolder przechowuje widoki dla elementu kategorii.
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewCategoryName;
        private final ImageView imageViewCategoryIcon;
        private final View colorIndicator;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
            imageViewCategoryIcon = itemView.findViewById(R.id.imageViewCategoryIcon);
            colorIndicator = itemView.findViewById(R.id.viewCategoryColor);
        }

        /**
         * Wiąże dane kategorii z widokiem.
         *
         * @param category Obiekt kategorii.
         * @param position Pozycja elementu w liście.
         */
        void bind(final Category category, final int position) {
            textViewCategoryName.setText(category.getName());

            // Ustawienie koloru kategorii
            GradientDrawable backgroundShape = (GradientDrawable) colorIndicator.getBackground();
            backgroundShape.setColor(category.getColor());

            // Ustawienie ikony kategorii na podstawie nazwy ikony
            int iconResId = getIconResourceByName(category.getIconName());
            if (iconResId != 0) {
                imageViewCategoryIcon.setImageResource(iconResId);
            } else {
                // Domyślna ikona
                imageViewCategoryIcon.setImageResource(R.drawable.ic_category_default);
            }

            // Ustawienie nasłuchiwacza kliknięcia
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category, position);
                }
            });
        }

        /**
         * Pobiera identyfikator zasobu ikony na podstawie jej nazwy.
         *
         * @param iconName Nazwa ikony.
         * @return Identyfikator zasobu ikony.
         */
        private int getIconResourceByName(String iconName) {
            if (iconName == null || iconName.isEmpty()) {
                return 0;
            }
            return context.getResources().getIdentifier(
                    iconName, "drawable", context.getPackageName());
        }
    }
}
