package com.example.paydaylay.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Arrays;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private EditText editTextCategoryName;
    private GridLayout gridLayoutColors;
    private GridLayout gridLayoutIcons;
    private MaterialButton buttonSave, buttonDelete;

    private int selectedColor;
    private String selectedIcon = "ic_category_default";
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private Category currentCategory;
    private boolean isEditMode = false;

    // Available category colors
    private final int[] categoryColors = {
            R.color.category_color_1,
            R.color.category_color_2,
            R.color.category_color_3,
            R.color.category_color_4,
            R.color.category_color_5,
            R.color.category_color_6,
            R.color.category_color_7,
            R.color.category_color_8
    };

    // Available category icons
    private final String[] categoryIcons = {
            "ic_category_default",
            "ic_category_food",
            "ic_category_shopping",
            "ic_category_transport",
            "ic_category_health",
            "ic_category_entertainment",
            "ic_category_home",
            "ic_category_bills",
            "ic_category_education",
            "ic_category_travel",
            "ic_category_gifts",
            "ic_category_salary"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        editTextCategoryName = findViewById(R.id.editTextCategoryName);
        gridLayoutColors = findViewById(R.id.gridLayoutColors);
        gridLayoutIcons = findViewById(R.id.gridLayoutIcons);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Check if we're in edit mode
        if (getIntent().hasExtra("category")) {
            currentCategory = (Category) getIntent().getSerializableExtra("category");
            isEditMode = true;
            setTitle(R.string.edit_category);
            fillFormWithCategoryData();
            buttonDelete.setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.add_category);
            selectedColor = ContextCompat.getColor(this, categoryColors[0]);
            buttonDelete.setVisibility(View.GONE);
        }

        setupColorSelection();
        setupIconSelection();

        // Set up save button
        buttonSave.setOnClickListener(v -> saveCategory());

        // Set up delete button
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void fillFormWithCategoryData() {
        if (currentCategory != null) {
            editTextCategoryName.setText(currentCategory.getName());
            selectedColor = currentCategory.getColor();
            selectedIcon = currentCategory.getIconName();
        }
    }

    private void setupColorSelection() {
        gridLayoutColors.removeAllViews();

        for (int colorRes : categoryColors) {
            int color = ContextCompat.getColor(this, colorRes);

            View colorView = getLayoutInflater().inflate(R.layout.item_color_select, gridLayoutColors, false);
            MaterialCardView cardView = colorView.findViewById(R.id.cardViewColor);
            View colorCircle = colorView.findViewById(R.id.viewColorCircle);

            colorCircle.setBackgroundColor(color);

            if (isEditMode && color == selectedColor) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else if (!isEditMode && color == ContextCompat.getColor(this, categoryColors[0])) {
                selectedColor = color;
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            }

            colorView.setOnClickListener(v -> {
                selectedColor = color;
                updateColorSelection();
            });

            gridLayoutColors.addView(colorView);
        }
    }

    private void updateColorSelection() {
        for (int i = 0; i < gridLayoutColors.getChildCount(); i++) {
            View colorView = gridLayoutColors.getChildAt(i);
            MaterialCardView cardView = colorView.findViewById(R.id.cardViewColor);
            View colorCircle = colorView.findViewById(R.id.viewColorCircle);

            int color = ((android.graphics.drawable.ColorDrawable) colorCircle.getBackground()).getColor();

            if (color == selectedColor) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else {
                cardView.setStrokeWidth(0);
            }
        }
    }

    private void setupIconSelection() {
        gridLayoutIcons.removeAllViews();

        for (String iconName : categoryIcons) {
            View iconView = getLayoutInflater().inflate(R.layout.item_icon_select, gridLayoutIcons, false);
            MaterialCardView cardView = iconView.findViewById(R.id.cardViewIcon);
            ImageView imageView = iconView.findViewById(R.id.imageViewIcon);

            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            if (iconResId != 0) {
                imageView.setImageResource(iconResId);
            }

            if (isEditMode && iconName.equals(selectedIcon)) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else if (!isEditMode && iconName.equals(categoryIcons[0])) {
                selectedIcon = iconName;
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            }

            iconView.setOnClickListener(v -> {
                selectedIcon = iconName;
                updateIconSelection();
            });

            gridLayoutIcons.addView(iconView);
        }
    }

    private void updateIconSelection() {
        for (int i = 0; i < gridLayoutIcons.getChildCount(); i++) {
            View iconView = gridLayoutIcons.getChildAt(i);
            MaterialCardView cardView = iconView.findViewById(R.id.cardViewIcon);
            ImageView imageView = iconView.findViewById(R.id.imageViewIcon);

            int iconResId = getResources().getIdentifier(categoryIcons[i], "drawable", getPackageName());

            if (categoryIcons[i].equals(selectedIcon)) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else {
                cardView.setStrokeWidth(0);
            }
        }
    }

    private void saveCategory() {
        String name = editTextCategoryName.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextCategoryName.setError(getString(R.string.category_name_required));
            return;
        }

        if (isEditMode && currentCategory != null) {
            currentCategory.setName(name);
            currentCategory.setColor(selectedColor);
            currentCategory.setIconName(selectedIcon);

            databaseManager.updateCategory(currentCategory, new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_updated), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.error_updating_category) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Category newCategory = new Category(name, selectedColor, authManager.getCurrentUserId(), selectedIcon);

            databaseManager.addCategory(newCategory, new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_added), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.error_adding_category) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showDeleteConfirmationDialog() {
        if (currentCategory == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_category)
                .setMessage(R.string.delete_category_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCategory())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteCategory() {
        if (currentCategory != null) {
            databaseManager.deleteCategory(currentCategory.getId(), new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.error_deleting_category) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}