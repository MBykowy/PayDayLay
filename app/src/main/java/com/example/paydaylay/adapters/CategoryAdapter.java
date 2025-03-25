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

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categories;
    private final Context context;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories.clear();
        this.categories.addAll(newCategories);
        notifyDataSetChanged();
    }

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

        void bind(final Category category, final int position) {
            textViewCategoryName.setText(category.getName());

            // Set category color
            GradientDrawable backgroundShape = (GradientDrawable) colorIndicator.getBackground();
            backgroundShape.setColor(category.getColor());

            // Set category icon based on iconName
            int iconResId = getIconResourceByName(category.getIconName());
            if (iconResId != 0) {
                imageViewCategoryIcon.setImageResource(iconResId);
            } else {
                // Default icon
                imageViewCategoryIcon.setImageResource(R.drawable.ic_category_default);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category, position);
                }
            });
        }

        private int getIconResourceByName(String iconName) {
            if (iconName == null || iconName.isEmpty()) {
                return 0;
            }
            return context.getResources().getIdentifier(
                    iconName, "drawable", context.getPackageName());
        }
    }
}