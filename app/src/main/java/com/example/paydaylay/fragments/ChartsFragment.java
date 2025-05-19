package com.example.paydaylay.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.utils.PdfExporter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
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

/**
 * Fragment odpowiedzialny za wyświetlanie wykresów transakcji użytkownika.
 * Obsługuje wykresy kołowe dla wydatków i przychodów oraz wykres słupkowy dla podsumowania miesięcznego.
 * Umożliwia eksport wykresów do pliku PDF.
 */
public class ChartsFragment extends Fragment {

    private static final String TAG = "ChartsFragment";
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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Inicjalizacja widoków
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        spinnerChartType = view.findViewById(R.id.spinnerChartType);
        spinnerTimeFrame = view.findViewById(R.id.spinnerTimeFrame);
        textViewNoData = view.findViewById(R.id.textViewNoData);

        // Konfiguracja spinnera typu wykresu
        ArrayAdapter<CharSequence> chartTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.chart_types, android.R.layout.simple_spinner_item);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);

        // Konfiguracja spinnera zakresu czasowego
        ArrayAdapter<CharSequence> timeFrameAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.time_frames, android.R.layout.simple_spinner_item);
        timeFrameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeFrame.setAdapter(timeFrameAdapter);

        // Obsługa wyboru w spinnerach
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

    /**
     * Wywoływane po wznowieniu fragmentu.
     * Ładuje dane do wykresów.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * Aktualizuje widoczność wykresów w zależności od wybranego typu.
     *
     * @param chartTypePosition Pozycja wybranego typu wykresu.
     */
    private void updateChartVisibility(int chartTypePosition) {
        switch (chartTypePosition) {
            case CHART_TYPE_EXPENSES:  // Wykres kołowy wydatków
            case CHART_TYPE_INCOME:    // Wykres kołowy przychodów
                pieChart.setVisibility(View.VISIBLE);
                barChart.setVisibility(View.GONE);
                break;
            case CHART_TYPE_MONTHLY:   // Wykres słupkowy
                pieChart.setVisibility(View.GONE);
                barChart.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Ładuje dane do wykresów z bazy danych.
     */
    private void loadData() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // Najpierw ładuje kategorie
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;
                categoryMap.clear();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                // Następnie ładuje transakcje
                databaseManager.getTransactions(userId, new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                        if (!isAdded()) {
                            return; // Przerwij jeśli fragment nie jest już dołączony
                        }
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

    /**
     * Filtruje transakcje według wybranego zakresu czasowego.
     *
     * @param allTransactions Lista wszystkich transakcji.
     * @return Lista przefiltrowanych transakcji.
     */
    private List<Transaction> filterTransactionsByTimeFrame(List<Transaction> allTransactions) {
        if (allTransactions == null || allTransactions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Transaction> filtered = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();

        int timeFramePosition = spinnerTimeFrame.getSelectedItemPosition();

        // Resetuje do początku dnia
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (timeFramePosition) {
            case 0: // Ten tydzień
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                break;
            case 1: // Ten miesiąc
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2: // Ostatnie 3 miesiące
                calendar.add(Calendar.MONTH, -3);
                break;
            case 3: // Ostatnie 6 miesięcy
                calendar.add(Calendar.MONTH, -6);
                break;
            case 4: // Ten rok
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case 5: // Cały czas
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

    /**
     * Wyświetla komunikat o błędzie.
     *
     * @param message Treść komunikatu błędu.
     */
    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}