package com.example.paydaylay.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.paydaylay.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Klasa odpowiedzialna za wyświetlanie dialogu wyboru zakresu dat.
 * Umożliwia użytkownikowi wybór daty początkowej i końcowej, a także szybki wybór predefiniowanych zakresów.
 */
public class DateRangePickerDialog {

    /**
     * Interfejs do obsługi zdarzeń wyboru zakresu dat.
     */
    public interface OnDateRangeSelectedListener {
        /**
         * Wywoływane po wybraniu zakresu dat przez użytkownika.
         *
         * @param startDate Data początkowa.
         * @param endDate   Data końcowa.
         */
        void onDateRangeSelected(Date startDate, Date endDate);
    }

    private final Context context;
    private final Calendar startDateCalendar;
    private final Calendar endDateCalendar;
    private final SimpleDateFormat dateFormat;
    private TextView startDateText;
    private TextView endDateText;

    /**
     * Konstruktor klasy DateRangePickerDialog.
     * Inicjalizuje domyślne wartości dat (pierwszy dzień bieżącego miesiąca jako data początkowa, dzisiaj jako data końcowa).
     *
     * @param context Kontekst aplikacji.
     */
    public DateRangePickerDialog(Context context) {
        this.context = context;
        this.startDateCalendar = Calendar.getInstance();
        this.endDateCalendar = Calendar.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Domyślnie: start = pierwszy dzień miesiąca, koniec = dzisiaj
        startDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
    }

    /**
     * Wyświetla dialog wyboru zakresu dat.
     *
     * @param listener Słuchacz zdarzeń wyboru zakresu dat.
     */
    public void show(OnDateRangeSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogView = LayoutInflater.from(context).inflate(
                R.layout.dialog_date_range_picker, null);

        startDateText = dialogView.findViewById(R.id.textViewStartDate);
        endDateText = dialogView.findViewById(R.id.textViewEndDate);
        Button buttonStartDate = dialogView.findViewById(R.id.buttonSelectStartDate);
        Button buttonEndDate = dialogView.findViewById(R.id.buttonSelectEndDate);

        updateDateTexts();

        // Ustawia nasłuchiwacze kliknięć dla przycisków wyboru dat
        buttonStartDate.setOnClickListener(v -> showStartDatePicker());
        buttonEndDate.setOnClickListener(v -> showEndDatePicker());

        // Przyciski predefiniowanych zakresów dat
        Button buttonThisMonth = dialogView.findViewById(R.id.buttonThisMonth);
        Button buttonLastMonth = dialogView.findViewById(R.id.buttonLastMonth);
        Button buttonLast3Months = dialogView.findViewById(R.id.buttonLast3Months);
        Button buttonThisYear = dialogView.findViewById(R.id.buttonThisYear);

        buttonThisMonth.setOnClickListener(v -> setThisMonth());
        buttonLastMonth.setOnClickListener(v -> setLastMonth());
        buttonLast3Months.setOnClickListener(v -> setLast3Months());
        buttonThisYear.setOnClickListener(v -> setThisYear());

        builder.setTitle(R.string.select_date_range)
                .setView(dialogView)
                .setPositiveButton(R.string.export, (dialog, which) -> {
                    listener.onDateRangeSelected(
                            startDateCalendar.getTime(),
                            endDateCalendar.getTime());
                })
                .setNegativeButton(R.string.cancel, null);

        builder.create().show();
    }

    /**
     * Wyświetla dialog wyboru daty początkowej.
     */
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    startDateCalendar.set(year, month, dayOfMonth);
                    if (startDateCalendar.after(endDateCalendar)) {
                        endDateCalendar.setTimeInMillis(startDateCalendar.getTimeInMillis());
                    }
                    updateDateTexts();
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Wyświetla dialog wyboru daty końcowej.
     */
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    endDateCalendar.set(year, month, dayOfMonth);
                    if (endDateCalendar.before(startDateCalendar)) {
                        startDateCalendar.setTimeInMillis(endDateCalendar.getTimeInMillis());
                    }
                    updateDateTexts();
                },
                endDateCalendar.get(Calendar.YEAR),
                endDateCalendar.get(Calendar.MONTH),
                endDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Aktualizuje teksty wyświetlane w polach daty początkowej i końcowej.
     */
    private void updateDateTexts() {
        startDateText.setText(dateFormat.format(startDateCalendar.getTime()));
        endDateText.setText(dateFormat.format(endDateCalendar.getTime()));
    }

    /**
     * Ustawia zakres dat na bieżący miesiąc.
     */
    private void setThisMonth() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }

    /**
     * Ustawia zakres dat na poprzedni miesiąc.
     */
    private void setLastMonth() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 1, 1);
        endDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 0);
        updateDateTexts();
    }

    /**
     * Ustawia zakres dat na ostatnie trzy miesiące.
     */
    private void setLast3Months() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 2, 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }

    /**
     * Ustawia zakres dat na bieżący rok.
     */
    private void setThisYear() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), Calendar.JANUARY, 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }
}