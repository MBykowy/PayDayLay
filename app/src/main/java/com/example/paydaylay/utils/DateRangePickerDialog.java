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

public class DateRangePickerDialog {

    public interface OnDateRangeSelectedListener {
        void onDateRangeSelected(Date startDate, Date endDate);
    }

    private final Context context;
    private final Calendar startDateCalendar;
    private final Calendar endDateCalendar;
    private final SimpleDateFormat dateFormat;
    private TextView startDateText;
    private TextView endDateText;

    public DateRangePickerDialog(Context context) {
        this.context = context;
        this.startDateCalendar = Calendar.getInstance();
        this.endDateCalendar = Calendar.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Default: start = first day of month, end = today
        startDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
    }

    public void show(OnDateRangeSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogView = LayoutInflater.from(context).inflate(
                R.layout.dialog_date_range_picker, null);

        startDateText = dialogView.findViewById(R.id.textViewStartDate);
        endDateText = dialogView.findViewById(R.id.textViewEndDate);
        Button buttonStartDate = dialogView.findViewById(R.id.buttonSelectStartDate);
        Button buttonEndDate = dialogView.findViewById(R.id.buttonSelectEndDate);

        updateDateTexts();

        // Set up click listeners for date buttons
        buttonStartDate.setOnClickListener(v -> showStartDatePicker());
        buttonEndDate.setOnClickListener(v -> showEndDatePicker());

        // Preset buttons
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

    private void updateDateTexts() {
        startDateText.setText(dateFormat.format(startDateCalendar.getTime()));
        endDateText.setText(dateFormat.format(endDateCalendar.getTime()));
    }

    private void setThisMonth() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }

    private void setLastMonth() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 1, 1);
        endDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 0);
        updateDateTexts();
    }

    private void setLast3Months() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 2, 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }

    private void setThisYear() {
        Calendar now = Calendar.getInstance();
        startDateCalendar.set(now.get(Calendar.YEAR), Calendar.JANUARY, 1);
        endDateCalendar.setTimeInMillis(now.getTimeInMillis());
        updateDateTexts();
    }
}