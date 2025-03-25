package com.example.paydaylay.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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

    public void exportChart(List<Transaction> transactions,
                            List<Category> categories,
                            Chart chart,
                            String chartType,
                            String timeFrame,
                            OnPdfExportListener listener) {

        try {
            // Create directory if it doesn't exist
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "PaydayLay");

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

            // Create PDF page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    chartBitmap.getWidth(), chartBitmap.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw chart on page
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(chartBitmap, 0, 0, null);

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
        // Make sure chart is properly measured and laid out
        int width = chart.getWidth();
        int height = chart.getHeight();

        Log.d(TAG, "Chart dimensions: " + width + "x" + height);

        if (width <= 0 || height <= 0) {
            // If chart hasn't been measured yet, use default dimensions
            width = 800;
            height = 600;
            chart.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            );
            chart.layout(0, 0, width, height);
        }

        // Create bitmap and draw chart
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        chart.draw(canvas);

        return bitmap;
    }
}