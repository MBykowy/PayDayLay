package com.example.paydaylay.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.CategoryActivity;
import com.example.paydaylay.adapters.CategoryAdapter;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment odpowiedzialny za zarządzanie kategoriami użytkownika.
 * Wyświetla listę kategorii, umożliwia ich edytowanie, dodawanie oraz sortowanie.
 */
public class CategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView recyclerViewCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private FloatingActionButton fabAddCategory;

    /**
     * Konstruktor domyślny fragmentu CategoriesFragment.
     */
    public CategoriesFragment() {
        // Wymagany pusty konstruktor publiczny
    }

    /**
     * Wywoływane podczas tworzenia fragmentu.
     * Ustawia, że fragment ma własne menu opcji.
     *
     * @param savedInstanceState Zapisany stan fragmentu.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Tworzy widok fragmentu.
     *
     * @param inflater  Obiekt LayoutInflater do tworzenia widoków.
     * @param container Kontener, w którym znajduje się fragment.
     * @param savedInstanceState Zapisany stan fragmentu.
     * @return Widok fragmentu.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Inicjalizacja RecyclerView
        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicjalizacja adaptera
        categoryAdapter = new CategoryAdapter(getContext(), categories, this);
        recyclerViewCategories.setAdapter(categoryAdapter);

        // Inicjalizacja FAB
        fabAddCategory = view.findViewById(R.id.fabAddCategory);
        fabAddCategory.setOnClickListener(v -> openCategoryEditor(null));

        return view;
    }

    /**
     * Wywoływane po wznowieniu fragmentu.
     * Ładuje listę kategorii.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
    }

    /**
     * Ładuje listę kategorii użytkownika z bazy danych.
     */
    private void loadCategories() {
        if (getContext() == null || authManager.getCurrentUserId() == null) return;

        databaseManager.getCategories(authManager.getCurrentUserId(), new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);
                categoryAdapter.notifyDataSetChanged();

                // Wyświetla widok pusty, jeśli brak kategorii
                if (getView() != null) {
                    getView().findViewById(R.id.emptyView).setVisibility(
                            categories.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error loading categories: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Obsługuje kliknięcie na kategorię.
     *
     * @param category Wybrana kategoria.
     * @param position Pozycja kategorii na liście.
     */
    @Override
    public void onCategoryClick(Category category, int position) {
        openCategoryEditor(category);
    }

    /**
     * Otwiera edytor kategorii.
     *
     * @param category Kategoria do edycji lub null, jeśli tworzona jest nowa.
     */
    private void openCategoryEditor(Category category) {
        Intent intent = new Intent(getContext(), CategoryActivity.class);
        if (category != null) {
            intent.putExtra("category", category);
        }
        startActivity(intent);
    }

    /**
     * Tworzy menu opcji dla fragmentu.
     *
     * @param menu     Obiekt menu.
     * @param inflater Obiekt inflatera menu.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.categories_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Obsługuje wybór elementu z menu opcji.
     *
     * @param item Wybrany element menu.
     * @return True, jeśli element został obsłużony, w przeciwnym razie false.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort_categories) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Wyświetla dialog sortowania kategorii.
     */
    private void showSortDialog() {
        String[] sortOptions = {
                getString(R.string.sort_by_name_asc),
                getString(R.string.sort_by_name_desc)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sort_categories)
                .setItems(sortOptions, (dialog, which) -> {
                    // Sortuje kategorie na podstawie wyboru
                    switch (which) {
                        case 0: // Nazwa rosnąco
                            categories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
                            break;
                        case 1: // Nazwa malejąco
                            categories.sort((c1, c2) -> c2.getName().compareToIgnoreCase(c1.getName()));
                            break;
                    }
                    categoryAdapter.notifyDataSetChanged();
                });
        builder.create().show();
    }
}