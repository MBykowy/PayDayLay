package com.example.paydaylay.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.example.paydaylay.R;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvExporter {

    private final Context context;
    private final SimpleDateFormat dateFormat;
    private final String currencySymbol;

    public interface OnCsvExportListener {
        void onSuccess(File csvFile);
        void onError(Exception e);
    }

    public CsvExporter(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencySymbol = context.getString(R.string.currency_symbol);
    }

    /**
     * Export transactions to a CSV file
     */
    public void exportTransactions(List<Transaction> transactions, Map<String, Category> categoryMap,
                                   Date startDate, Date endDate, OnCsvExportListener listener) {
        try {
            // Create directory for CSV files if it doesn't exist
            File csvDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "PayDayLay");
            if (!csvDir.exists() && !csvDir.mkdirs()) {
                throw new Exception(context.getString(R.string.error_creating_directory));
            }

            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "transactions_" + timestamp + ".csv";
            File csvFile = new File(csvDir, fileName);

            // Create CSV writer
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile));

            // Write header
            String[] header = new String[] {
                    context.getString(R.string.date),
                    context.getString(R.string.description),
                    context.getString(R.string.category),
                    context.getString(R.string.amount),
                    context.getString(R.string.type)
            };
            writer.writeNext(header);

            // Write transactions
            for (Transaction transaction : transactions) {
                // Get category name
                String categoryName = context.getString(R.string.unknown_category);
                if (categoryMap.containsKey(transaction.getCategoryId())) {
                    categoryName = categoryMap.get(transaction.getCategoryId()).getName();
                }

                // Format amount
                String amount = String.format(Locale.getDefault(), "%.2f %s",
                        transaction.getAmount(), currencySymbol);

                // Determine transaction type
                String type = transaction.isExpense() ?
                        context.getString(R.string.expense) :
                        context.getString(R.string.income);

                // Create row
                String[] row = new String[] {
                        dateFormat.format(transaction.getDate()),
                        transaction.getDescription() != null ? transaction.getDescription() : "",
                        categoryName,
                        amount,
                        type
                };
                writer.writeNext(row);
            }

            // Close writer
            writer.close();

            // Notify success
            listener.onSuccess(csvFile);

        } catch (Exception e) {
            listener.onError(e);
        }
    }

    /**
     * Share the CSV file with other apps
     */
    public void shareFile(File csvFile) {
        Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                csvFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_csv_file));

        context.startActivity(chooser);
    }

    /**
     * Get summary information as an array of string arrays for CSV export
     */
    /**
     * Get summary information as an array of string arrays for CSV export
     */
    public String[][] getSummaryData(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : transactions) {
            if (transaction.isExpense()) {
                totalExpense += transaction.getAmount();
            } else {
                totalIncome += transaction.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;

        return new String[][] {
                {context.getString(R.string.summary_title), ""},
                {context.getString(R.string.total_income),
                        String.format(Locale.getDefault(), "%.2f %s", totalIncome, currencySymbol)},
                {context.getString(R.string.total_expenses),
                        String.format(Locale.getDefault(), "%.2f %s", totalExpense, currencySymbol)},
                {context.getString(R.string.balance),
                        String.format(Locale.getDefault(), "%.2f %s", balance, currencySymbol)}
        };
    }
}