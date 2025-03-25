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

public class CategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView recyclerViewCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private FloatingActionButton fabAddCategory;

    public CategoriesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Initialize RecyclerView
        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter
        categoryAdapter = new CategoryAdapter(getContext(), categories, this);
        recyclerViewCategories.setAdapter(categoryAdapter);

        // Initialize FAB
        fabAddCategory = view.findViewById(R.id.fabAddCategory);
        fabAddCategory.setOnClickListener(v -> openCategoryEditor(null));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        if (getContext() == null || authManager.getCurrentUserId() == null) return;

        databaseManager.getCategories(authManager.getCurrentUserId(), new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);
                categoryAdapter.notifyDataSetChanged();

                // Show empty view if no categories
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

    @Override
    public void onCategoryClick(Category category, int position) {
        openCategoryEditor(category);
    }

    private void openCategoryEditor(Category category) {
        Intent intent = new Intent(getContext(), CategoryActivity.class);
        if (category != null) {
            intent.putExtra("category", category);
        }
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.categories_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort_categories) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        String[] sortOptions = {
                getString(R.string.sort_by_name_asc),
                getString(R.string.sort_by_name_desc)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sort_categories)
                .setItems(sortOptions, (dialog, which) -> {
                    // Sort categories based on selection
                    switch (which) {
                        case 0: // Name ASC
                            categories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
                            break;
                        case 1: // Name DESC
                            categories.sort((c1, c2) -> c2.getName().compareToIgnoreCase(c1.getName()));
                            break;
                    }
                    categoryAdapter.notifyDataSetChanged();
                });
        builder.create().show();
    }
}