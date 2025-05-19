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

/**
 * Aktywność służąca do tworzenia nowej kategorii lub edytowania istniejącej.
 * Umożliwia użytkownikowi wprowadzenie nazwy kategorii, wybór koloru oraz ikony.
 * Dziedziczy po {@link BaseActivity}.
 */
public class CategoryActivity extends BaseActivity {

    /**
     * Pole tekstowe do wprowadzania nazwy kategorii.
     */
    private EditText editTextCategoryName;
    /**
     * Siatka (GridLayout) do wyświetlania i wyboru dostępnych kolorów kategorii.
     */
    private GridLayout gridLayoutColors;
    /**
     * Siatka (GridLayout) do wyświetlania i wyboru dostępnych ikon kategorii.
     */
    private GridLayout gridLayoutIcons;
    /**
     * Przycisk do zapisywania zmian w kategorii (dodawanie nowej lub aktualizacja istniejącej).
     */
    private MaterialButton buttonSave;
    /**
     * Przycisk do usuwania kategorii (widoczny tylko w trybie edycji).
     */
    private MaterialButton buttonDelete;

    /**
     * Aktualnie wybrany kolor dla kategorii (wartość liczbowa koloru).
     */
    private int selectedColor;
    /**
     * Nazwa aktualnie wybranej ikony dla kategorii (np. "ic_category_default").
     */
    private String selectedIcon = "ic_category_default";
    /**
     * Menedżer bazy danych do wykonywania operacji CRUD na kategoriach.
     */
    private DatabaseManager databaseManager;
    /**
     * Menedżer uwierzytelniania, używany m.in. do pobrania ID bieżącego użytkownika.
     */
    private AuthManager authManager;
    /**
     * Obiekt kategorii, który jest edytowany. Null, jeśli tworzona jest nowa kategoria.
     */
    private Category currentCategory;
    /**
     * Flaga wskazująca, czy aktywność jest w trybie edycji (true) czy tworzenia nowej kategorii (false).
     */
    private boolean isEditMode = false;

    /**
     * Tablica predefiniowanych identyfikatorów zasobów kolorów dostępnych dla kategorii.
     */
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

    /**
     * Tablica predefiniowanych nazw zasobów ikon (drawable) dostępnych dla kategorii.
     */
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

    /**
     * Metoda cyklu życia aktywności, wywoływana przy jej tworzeniu.
     * Inicjalizuje widoki, menedżerów, pasek narzędzi oraz ustawia tryb (dodawanie/edycja)
     * na podstawie przekazanych danych (Intent).
     *
     * @param savedInstanceState Jeśli aktywność jest ponownie tworzona po poprzednim zniszczeniu,
     *                           ten pakiet zawiera dane, które ostatnio dostarczyła w
     *                           {@link #onSaveInstanceState(Bundle)}. W przeciwnym razie ma wartość null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Konfiguracja paska narzędzi
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Inicjalizacja widoków
        editTextCategoryName = findViewById(R.id.editTextCategoryName);
        gridLayoutColors = findViewById(R.id.gridLayoutColors);
        gridLayoutIcons = findViewById(R.id.gridLayoutIcons);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Sprawdzenie, czy jesteśmy w trybie edycji
        if (getIntent().hasExtra("category")) {
            currentCategory = (Category) getIntent().getSerializableExtra("category");
            isEditMode = true;
            setTitle(R.string.edit_category); // Ustawia tytuł "Edytuj kategorię"
            fillFormWithCategoryData();
            buttonDelete.setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.add_category); // Ustawia tytuł "Dodaj kategorię"
            selectedColor = ContextCompat.getColor(this, categoryColors[0]); // Domyślny kolor
            buttonDelete.setVisibility(View.GONE);
        }

        setupColorSelection();
        setupIconSelection();

        // Ustawienie nasłuchiwacza dla przycisku zapisu
        buttonSave.setOnClickListener(v -> saveCategory());

        // Ustawienie nasłuchiwacza dla przycisku usuwania
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Wypełnia pola formularza (nazwa, kolor, ikona) danymi z {@code currentCategory},
     * jeśli aktywność jest w trybie edycji i {@code currentCategory} nie jest nullem.
     */
    private void fillFormWithCategoryData() {
        if (currentCategory != null) {
            editTextCategoryName.setText(currentCategory.getName());
            selectedColor = currentCategory.getColor();
            selectedIcon = currentCategory.getIconName();
        }
    }

    /**
     * Konfiguruje siatkę wyboru kolorów. Dla każdego koloru z {@code categoryColors}
     * tworzy widok wyboru koloru, ustawia jego tło i obsługę kliknięcia.
     * Zaznacza wybrany kolor (lub pierwszy domyślny, jeśli to nowa kategoria).
     */
    private void setupColorSelection() {
        gridLayoutColors.removeAllViews(); // Czyści poprzednie widoki

        for (int colorRes : categoryColors) {
            int color = ContextCompat.getColor(this, colorRes);

            View colorView = getLayoutInflater().inflate(R.layout.item_color_select, gridLayoutColors, false);
            MaterialCardView cardView = colorView.findViewById(R.id.cardViewColor);
            View colorCircle = colorView.findViewById(R.id.viewColorCircle);

            colorCircle.setBackgroundColor(color);

            // Zaznaczenie koloru w trybie edycji lub domyślnego dla nowej kategorii
            if (isEditMode && color == selectedColor) {
                cardView.setStrokeWidth(5); // Grubość obramowania
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent)); // Kolor obramowania
            } else if (!isEditMode && color == ContextCompat.getColor(this, categoryColors[0])) {
                // Jeśli to nowa kategoria i jest to pierwszy kolor na liście, zaznacz go
                selectedColor = color; // Ustaw jako wybrany
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            }

            colorView.setOnClickListener(v -> {
                selectedColor = color; // Aktualizacja wybranego koloru
                updateColorSelection(); // Aktualizacja wizualna siatki
            });

            gridLayoutColors.addView(colorView);
        }
    }

    /**
     * Aktualizuje wizualnie wybór koloru w siatce {@code gridLayoutColors}.
     * Usuwa obramowanie ze wszystkich elementów i dodaje je tylko do aktualnie wybranego.
     */
    private void updateColorSelection() {
        for (int i = 0; i < gridLayoutColors.getChildCount(); i++) {
            View colorView = gridLayoutColors.getChildAt(i);
            MaterialCardView cardView = colorView.findViewById(R.id.cardViewColor);
            View colorCircle = colorView.findViewById(R.id.viewColorCircle);

            // Pobranie koloru z tła kółka
            int color = ((android.graphics.drawable.ColorDrawable) colorCircle.getBackground()).getColor();

            if (color == selectedColor) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else {
                cardView.setStrokeWidth(0); // Brak obramowania
            }
        }
    }

    /**
     * Konfiguruje siatkę wyboru ikon. Dla każdej nazwy ikony z {@code categoryIcons}
     * tworzy widok wyboru ikony, ustawia jej obrazek i obsługę kliknięcia.
     * Zaznacza wybraną ikonę (lub pierwszą domyślną, jeśli to nowa kategoria).
     */
    private void setupIconSelection() {
        gridLayoutIcons.removeAllViews(); // Czyści poprzednie widoki

        for (String iconName : categoryIcons) {
            View iconView = getLayoutInflater().inflate(R.layout.item_icon_select, gridLayoutIcons, false);
            MaterialCardView cardView = iconView.findViewById(R.id.cardViewIcon);
            ImageView imageView = iconView.findViewById(R.id.imageViewIcon);

            // Pobranie identyfikatora zasobu drawable na podstawie nazwy
            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            if (iconResId != 0) {
                imageView.setImageResource(iconResId);
            }

            // Zaznaczenie ikony w trybie edycji lub domyślnej dla nowej kategorii
            if (isEditMode && iconName.equals(selectedIcon)) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else if (!isEditMode && iconName.equals(categoryIcons[0])) {
                // Jeśli to nowa kategoria i jest to pierwsza ikona na liście, zaznacz ją
                selectedIcon = iconName; // Ustaw jako wybraną
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            }

            iconView.setOnClickListener(v -> {
                selectedIcon = iconName; // Aktualizacja wybranej ikony
                updateIconSelection(); // Aktualizacja wizualna siatki
            });

            gridLayoutIcons.addView(iconView);
        }
    }

    /**
     * Aktualizuje wizualnie wybór ikony w siatce {@code gridLayoutIcons}.
     * Usuwa obramowanie ze wszystkich elementów i dodaje je tylko do aktualnie wybranego.
     */
    private void updateIconSelection() {
        for (int i = 0; i < gridLayoutIcons.getChildCount(); i++) {
            View iconView = gridLayoutIcons.getChildAt(i);
            MaterialCardView cardView = iconView.findViewById(R.id.cardViewIcon);
            // ImageView imageView = iconView.findViewById(R.id.imageViewIcon); // Niepotrzebne do aktualizacji wyglądu

            // Sprawdzenie, czy bieżąca ikona w pętli jest tą wybraną
            if (categoryIcons[i].equals(selectedIcon)) {
                cardView.setStrokeWidth(5);
                cardView.setStrokeColor(ContextCompat.getColor(this, R.color.accent));
            } else {
                cardView.setStrokeWidth(0);
            }
        }
    }

    /**
     * Zapisuje kategorię do bazy danych.
     * Jeśli {@code isEditMode} jest true, aktualizuje istniejącą kategorię {@code currentCategory}.
     * W przeciwnym razie tworzy nową kategorię z wprowadzonych danych.
     * Wyświetla odpowiedni komunikat Toast o sukcesie lub błędzie operacji.
     */
    private void saveCategory() {
        String name = editTextCategoryName.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextCategoryName.setError(getString(R.string.category_name_required));
            return;
        }

        if (isEditMode && currentCategory != null) {
            // Tryb edycji: aktualizacja istniejącej kategorii
            currentCategory.setName(name);
            currentCategory.setColor(selectedColor);
            currentCategory.setIconName(selectedIcon);

            databaseManager.updateCategory(currentCategory, new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_updated), Toast.LENGTH_SHORT).show();
                    finish(); // Zamyka aktywność po pomyślnej aktualizacji
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.error_updating_category) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Tryb dodawania: tworzenie nowej kategorii
            Category newCategory = new Category(name, selectedColor, authManager.getCurrentUserId(), selectedIcon);

            databaseManager.addCategory(newCategory, new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_added), Toast.LENGTH_SHORT).show();
                    finish(); // Zamyka aktywność po pomyślnym dodaniu
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

    /**
     * Wyświetla dialog potwierdzający usunięcie kategorii.
     * Wywoływane po kliknięciu przycisku usuwania. Upewnia się, że {@code currentCategory} nie jest nullem.
     */
    private void showDeleteConfirmationDialog() {
        if (currentCategory == null) return; // Nie powinno się zdarzyć, jeśli przycisk jest widoczny

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_category) // Tytuł "Usuń kategorię"
                .setMessage(R.string.delete_category_confirmation) // Komunikat "Czy na pewno chcesz usunąć tę kategorię?"
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCategory()) // Przycisk "Usuń"
                .setNegativeButton(R.string.cancel, null) // Przycisk "Anuluj"
                .show();
    }

    /**
     * Usuwa bieżącą kategorię ({@code currentCategory}) z bazy danych.
     * Wywoływane po potwierdzeniu w dialogu usuwania.
     * Wyświetla odpowiedni komunikat Toast o sukcesie lub błędzie operacji.
     */
    private void deleteCategory() {
        if (currentCategory != null) {
            databaseManager.deleteCategory(currentCategory.getId(), new DatabaseManager.OnCategoryOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CategoryActivity.this,
                            getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                    finish(); // Zamyka aktywność po pomyślnym usunięciu
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

    /**
     * Obsługuje wybór elementów z menu opcji (np. przycisk wstecz na pasku narzędzi).
     *
     * @param item Wybrany element menu.
     * @return Zwraca true, jeśli zdarzenie zostało obsłużone (np. naciśnięcie przycisku "wstecz"),
     * w przeciwnym razie przekazuje obsługę do klasy nadrzędnej.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Zachowanie przycisku "wstecz"
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}