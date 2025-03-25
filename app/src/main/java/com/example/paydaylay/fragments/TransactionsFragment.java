package com.example.paydaylay.fragments;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.TransactionActivity;
import com.example.paydaylay.adapters.TransactionAdapter;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opencsv.CSVWriter;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionsFragment extends Fragment {

    private RecyclerView recyclerViewTransactions;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddTransaction;
    private TransactionAdapter adapter;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private List<Transaction> transactions;
    private List<Category> categories;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transactions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        // Initialize views
        recyclerViewTransactions = view.findViewById(R.id.recyclerViewTransactions);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        progressBar = view.findViewById(R.id.progressBar); // Initialize progress bar


        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Initialize lists
        transactions = new ArrayList<>();
        categories = new ArrayList<>();

        // Setup RecyclerView
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext(), transactions, categories);
        recyclerViewTransactions.setAdapter(adapter);

        // Setup FAB
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TransactionActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (getActivity() == null) return;

        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // First load categories
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;

                // Then load transactions
                databaseManager.getTransactions(userId, new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                        if (getActivity() == null) return;

                        transactions = loadedTransactions;
                        adapter.updateData(transactions, categories);

                        // Show empty view if no transactions
                        if (transactions.isEmpty()) {
                            recyclerViewTransactions.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewTransactions.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        Toast.makeText(getActivity(),
                                "Error loading transactions: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                Toast.makeText(getActivity(),
                        "Error loading categories: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    // In TransactionsFragment.java

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_csv) {
            showExportCsvDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Add helper methods to show UI feedback
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }


    private void showExportCsvDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Transactions")
                .setMessage("How would you like to export your transactions?")
                .setPositiveButton("Share", (dialog, which) -> {
                    exportTransactionsToCsv(true);
                })
                .setNegativeButton("Save to Downloads", (dialog, which) -> {
                    exportTransactionsToCsv(false);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
    private void exportTransactionsToCsv(boolean share) {
        if (transactions.isEmpty()) {
            showMessage("No transactions to export");
            return;
        }

        showLoading(true);

        // Get file directory - use Download folder when saving
        File directory;
        if (share) {
            directory = new File(requireContext().getExternalFilesDir(null), "PayDayLay");
        } else {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PayDayLay");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "transactions_" + System.currentTimeMillis() + ".csv";
        File file = new File(directory, fileName);

        new Thread(() -> {
            try {
                // Create CSV file with header and data
                FileWriter fileWriter = new FileWriter(file);
                CSVWriter csvWriter = new CSVWriter(fileWriter);

                // Write header
                String[] header = {"Date", "Amount", "Category", "Description", "Type"};
                csvWriter.writeNext(header);

                // Write transaction data
                Map<String, String> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category.getName());
                }

                for (Transaction transaction : transactions) {
                    String categoryName = categoryMap.getOrDefault(transaction.getCategoryId(), "Unknown");
                    String[] data = {
                            new SimpleDateFormat("yyyy-MM-dd").format(transaction.getDate()),
                            String.valueOf(transaction.getAmount()),
                            categoryName,
                            transaction.getDescription(),
                            transaction.isExpense() ? "Expense" : "Income"
                    };
                    csvWriter.writeNext(data);
                }

                csvWriter.close();

                // Share or notify about saved file
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    if (share) {
                        shareFile(file);
                    } else {
                        // Make the file visible in Downloads
                        MediaScannerConnection.scanFile(requireContext(),
                                new String[]{file.getAbsolutePath()}, null, null);
                        showMessage(getString(R.string.csv_exported_successfully, file.getAbsolutePath()));
                    }
                });
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Export failed: " + e.getMessage());
                });
            }
        }).start();
    }
    private void shareFile(File file) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                requireContext(),
                "com.example.paydaylay.provider",
                file));
        shareIntent.setType("text/csv");
        startActivity(Intent.createChooser(shareIntent, "Share CSV file"));
    }


    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}