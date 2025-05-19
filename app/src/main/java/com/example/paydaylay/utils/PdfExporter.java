package com.example.paydaylay.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfExporter {
    private static final String TAG = "PdfExporter";
    private final Context context;

    public PdfExporter(Context context) {
        this.context = context;
    }

    public interface OnPdfExportListener {
        void onSuccess(String filePath);
        void onError(Exception e);
    }

    public void exportChart(Chart chart, String chartTitle, OnPdfExportListener listener) {
        try {
            // Use the app's private directory or Downloads directory
            File directory;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PaydayLay");
            } else {
                directory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "PaydayLay");
            }

            Log.d(TAG, "Storage directory path: " + directory.getAbsolutePath());

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                Log.d(TAG, "Directory created: " + created);
                if (!created) {
                    throw new IOException("Failed to create directory");
                }
            }

            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "chart_" + timestamp + ".pdf";
            File file = new File(directory, fileName);

            Log.d(TAG, "Creating file: " + file.getAbsolutePath());

            // Create PDF document
            PdfDocument document = new PdfDocument();

            // Get chart bitmap
            Bitmap chartBitmap = getChartBitmap(chart);
            if (chartBitmap == null) {
                throw new IOException("Failed to render chart bitmap");
            }

            // Create PDF page with some padding
            int padding = 50;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    chartBitmap.getWidth() + padding * 2,
                    chartBitmap.getHeight() + padding * 2 + 80, // Extra space for title
                    1).create();

            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw chart on page with padding
            Canvas canvas = page.getCanvas();

            // Fill background
            canvas.drawColor(Color.WHITE);

            // Draw title
            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(30);
            canvas.drawText(chartTitle, padding, 60, titlePaint);

            // Draw chart
            canvas.drawBitmap(chartBitmap, padding, padding + 80, null);

            // Finish page
            document.finishPage(page);

            // Write to file
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d(TAG, "PDF file saved: " + file.getAbsolutePath());

            // Success callback
            listener.onSuccess(file.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error exporting PDF", e);
            listener.onError(e);
        }
    }

    private Bitmap getChartBitmap(Chart chart) {
        // Ensure the chart is fully rendered
        if (chart instanceof PieChart) {
            ((PieChart) chart).invalidate();
        } else if (chart instanceof BarChart) {
            ((BarChart) chart).invalidate();
        }

        // Get chart dimensions
        int width = chart.getWidth();
        int height = chart.getHeight();

        if (width <= 0 || height <= 0) {
            // Use sensible defaults for unrendered charts
            width = 1200;
            height = 800;

            // Force measure and layout
            chart.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            chart.layout(0, 0, width, height);
        }

        // Create bitmap with appropriate configuration
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw chart background
        canvas.drawColor(Color.WHITE);

        // Draw the chart
        chart.draw(canvas);

        return bitmap;
    }
}