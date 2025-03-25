package com.example.paydaylay.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.utils.PdfExporter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartsFragment extends Fragment {

    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 100;
    private static final int CHART_TYPE_EXPENSES = 0;
    private static final int CHART_TYPE_INCOME = 1;
    private static final int CHART_TYPE_MONTHLY = 2;

    private PieChart pieChart;
    private BarChart barChart;
    private Spinner spinnerChartType;
    private Spinner spinnerTimeFrame;
    private TextView textViewNoData;

    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private List<Category> categories = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Initialize views
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        spinnerChartType = view.findViewById(R.id.spinnerChartType);
        spinnerTimeFrame = view.findViewById(R.id.spinnerTimeFrame);
        textViewNoData = view.findViewById(R.id.textViewNoData);

        // Setup chart type spinner
        ArrayAdapter<CharSequence> chartTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.chart_types, android.R.layout.simple_spinner_item);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);

        // Setup time frame spinner
        ArrayAdapter<CharSequence> timeFrameAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.time_frames, android.R.layout.simple_spinner_item);
        timeFrameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeFrame.setAdapter(timeFrameAdapter);

        // Setup spinners listeners
        spinnerChartType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChartVisibility(position);
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinnerTimeFrame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void updateChartVisibility(int chartTypePosition) {
        switch (chartTypePosition) {
            case CHART_TYPE_EXPENSES:  // Expenses Pie Chart
            case CHART_TYPE_INCOME:    // Income Pie Chart
                pieChart.setVisibility(View.VISIBLE);
                barChart.setVisibility(View.GONE);
                break;
            case CHART_TYPE_MONTHLY:   // Bar Chart
                pieChart.setVisibility(View.GONE);
                barChart.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateCharts() {
        if (transactions.isEmpty()) {
            showNoDataMessage();
            return;
        }

        hideNoDataMessage();

        int chartType = spinnerChartType.getSelectedItemPosition();
        switch (chartType) {
            case CHART_TYPE_EXPENSES:
                updatePieChart(true); // Show expenses
                break;
            case CHART_TYPE_INCOME:
                updatePieChart(false); // Show income
                break;
            case CHART_TYPE_MONTHLY:
                updateBarChart();
                break;
        }
    }

    private void updatePieChart(boolean showExpenses) {
        // Aggregate data by category
        Map<String, Float> categoryTotals = new HashMap<>();
        Map<String, Integer> categoryColors = new HashMap<>();

        for (Transaction transaction : transactions) {
            // Filter transactions based on type (expense or income)
            if (transaction.isExpense() == showExpenses) {
                String categoryId = transaction.getCategoryId();
                Category category = categoryMap.get(categoryId);
                String categoryName = category != null ? category.getName() : getString(R.string.unknown_category);

                // Update totals
                float currentTotal = categoryTotals.containsKey(categoryName) ?
                        categoryTotals.get(categoryName) : 0f;
                categoryTotals.put(categoryName, currentTotal + (float)transaction.getAmount());

                // Store category color if available
                if (category != null) {
                    categoryColors.put(categoryName, category.getColor());
                }
            }
        }

        // Create pie chart entries
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));

            // Use category color or default
            if (categoryColors.containsKey(entry.getKey())) {
                colors.add(categoryColors.get(entry.getKey()));
            } else {
                colors.add(ColorTemplate.COLORFUL_COLORS[colors.size() % ColorTemplate.COLORFUL_COLORS.length]);
            }
        }

        // Show message if no data for this chart type
        if (pieEntries.isEmpty()) {
            textViewNoData.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            return;
        }

        // Configure pie chart
        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);

        Description description = new Description();
        description.setText(showExpenses ?
                getString(R.string.expenses_by_category) :
                getString(R.string.income_by_category));
        description.setTextSize(14f);
        pieChart.setDescription(description);

        pieChart.invalidate(); // Refresh
    }

    private void updateBarChart() {
        // Aggregate monthly data for expenses and income
        Map<Integer, Float> monthlyExpenses = new HashMap<>();
        Map<Integer, Float> monthlyIncome = new HashMap<>();

        Calendar calendar = Calendar.getInstance();

        for (Transaction transaction : transactions) {
            calendar.setTime(transaction.getDate());
            int month = calendar.get(Calendar.MONTH);

            if (transaction.isExpense()) {
                float current = monthlyExpenses.containsKey(month) ? monthlyExpenses.get(month) : 0f;
                monthlyExpenses.put(month, current + (float)transaction.getAmount());
            } else {
                float current = monthlyIncome.containsKey(month) ? monthlyIncome.get(month) : 0f;
                monthlyIncome.put(month, current + (float)transaction.getAmount());
            }
        }

        // Create bar entries
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<BarEntry> incomeEntries = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            float expense = monthlyExpenses.containsKey(i) ? monthlyExpenses.get(i) : 0f;
            float income = monthlyIncome.containsKey(i) ? monthlyIncome.get(i) : 0f;

            expenseEntries.add(new BarEntry(i, expense));
            incomeEntries.add(new BarEntry(i, income));
        }

        // Create datasets
        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, getString(R.string.expenses));
        expenseDataSet.setColor(getResources().getColor(R.color.expense_color));

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, getString(R.string.income));
        incomeDataSet.setColor(getResources().getColor(R.color.income_color));

        BarData barData = new BarData(expenseDataSet, incomeDataSet);

        // Configure bar chart
        barChart.setData(barData);
        barChart.getDescription().setText(getString(R.string.monthly_summary));

        // Set X axis labels to month names
        String[] months = getResources().getStringArray(R.array.months);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);

        barChart.setFitBars(true);
        barChart.invalidate(); // Refresh
    }

    private void loadData() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // First load categories
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;
                categoryMap.clear();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                // Then load transactions
                databaseManager.getTransactions(userId, new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                        transactions = filterTransactionsByTimeFrame(loadedTransactions);
                        updateCharts();
                    }

                    @Override
                    public void onError(Exception e) {
                        showError("Error loading transactions: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                showError("Error loading categories: " + e.getMessage());
            }
        });
    }

    private List<Transaction> filterTransactionsByTimeFrame(List<Transaction> allTransactions) {
        if (allTransactions == null || allTransactions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Transaction> filtered = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();

        int timeFramePosition = spinnerTimeFrame.getSelectedItemPosition();

        // Reset to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (timeFramePosition) {
            case 0: // This week
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                break;
            case 1: // This month
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2: // Last 3 months
                calendar.add(Calendar.MONTH, -3);
                break;
            case 3: // Last 6 months
                calendar.add(Calendar.MONTH, -6);
                break;
            case 4: // This year
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case 5: // All time
                return new ArrayList<>(allTransactions);
        }

        long startTime = calendar.getTimeInMillis();

        for (Transaction transaction : allTransactions) {
            long transactionTime = transaction.getDate().getTime();
            if (transactionTime >= startTime) {
                filtered.add(transaction);
            }
        }

        return filtered;
    }

    private void showNoDataMessage() {
        textViewNoData.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
    }

    private void hideNoDataMessage() {
        textViewNoData.setVisibility(View.GONE);

        int chartType = spinnerChartType.getSelectedItemPosition();
        if (chartType == CHART_TYPE_EXPENSES || chartType == CHART_TYPE_INCOME) { // Pie charts
            pieChart.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.GONE);
        } else { // Bar chart
            pieChart.setVisibility(View.GONE);
            barChart.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.charts_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export_pdf) {
            if (transactions.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_data_to_export, Toast.LENGTH_SHORT).show();
                return true;
            }

            // Check for storage permission
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_STORAGE);
            } else {
                exportToPdf();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToPdf();
            } else {
                Toast.makeText(getActivity(), R.string.storage_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportToPdf() {
        int chartType = spinnerChartType.getSelectedItemPosition();
        int timeFramePosition = spinnerTimeFrame.getSelectedItemPosition();

        String timeFrameText = spinnerTimeFrame.getSelectedItem().toString();
        String chartTypeText = spinnerChartType.getSelectedItem().toString();

        // Show a progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.exporting_pdf));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Ensure charts are properly rendered before export
        if (chartType == CHART_TYPE_MONTHLY) {
            barChart.invalidate();
        } else {
            pieChart.invalidate();
        }

        PdfExporter pdfExporter = new PdfExporter(requireContext());
        pdfExporter.exportChart(
                transactions,
                categories,
                chartType == CHART_TYPE_MONTHLY ? barChart : pieChart,
                chartTypeText,
                timeFrameText,
                new PdfExporter.OnPdfExportListener() {
                    @Override
                    public void onSuccess(String filePath) {
                        progressDialog.dismiss();

                        // Show success dialog with option to view the file
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle(R.string.export_successful);
                        builder.setMessage(getString(R.string.pdf_exported_successfully, filePath));
                        builder.setPositiveButton(R.string.view_file, (dialog, which) -> {
                            // Open the PDF file
                            try {
                                File file = new File(filePath);
                                Uri uri = FileProvider.getUriForFile(
                                        requireContext(),
                                        requireContext().getApplicationContext().getPackageName() + ".provider",
                                        file);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri, "application/pdf");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(getActivity(), R.string.cannot_open_pdf, Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder.setNegativeButton(R.string.close, null);
                        builder.show();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressDialog.dismiss();
                        Log.e("ChartsFragment", "PDF export error", e);
                        Toast.makeText(getActivity(),
                                getString(R.string.pdf_export_failed, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}