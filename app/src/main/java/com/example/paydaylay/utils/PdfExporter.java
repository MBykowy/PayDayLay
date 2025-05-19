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

/**
 * Klasa odpowiedzialna za eksportowanie wykresów do plików PDF.
 * Umożliwia zapisanie wykresów w formacie PDF w katalogu aplikacji lub w katalogu Pobrane.
 */
public class PdfExporter {
    private static final String TAG = "PdfExporter";
    private final Context context;

    /**
     * Konstruktor klasy PdfExporter.
     *
     * @param context Kontekst aplikacji.
     */
    public PdfExporter(Context context) {
        this.context = context;
    }

    /**
     * Interfejs do obsługi zdarzeń eksportu PDF.
     */
    public interface OnPdfExportListener {
        /**
         * Wywoływane po pomyślnym eksporcie pliku PDF.
         *
         * @param filePath Ścieżka do zapisanego pliku PDF.
         */
        void onSuccess(String filePath);

        /**
         * Wywoływane w przypadku błędu podczas eksportu.
         *
         * @param e Wyjątek opisujący błąd.
         */
        void onError(Exception e);
    }

    /**
     * Eksportuje wykres do pliku PDF.
     *
     * @param chart      Obiekt wykresu do eksportu.
     * @param chartTitle Tytuł wykresu, który zostanie umieszczony w pliku PDF.
     * @param listener   Słuchacz zdarzeń eksportu.
     */
    public void exportChart(Chart chart, String chartTitle, OnPdfExportListener listener) {
        try {
            // Ustawia katalog zapisu pliku PDF
            File directory;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PaydayLay");
            } else {
                directory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "PaydayLay");
            }

            Log.d(TAG, "Ścieżka katalogu: " + directory.getAbsolutePath());

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                Log.d(TAG, "Katalog utworzony: " + created);
                if (!created) {
                    throw new IOException("Nie udało się utworzyć katalogu");
                }
            }

            // Generuje nazwę pliku z sygnaturą czasową
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "chart_" + timestamp + ".pdf";
            File file = new File(directory, fileName);

            Log.d(TAG, "Tworzenie pliku: " + file.getAbsolutePath());

            // Tworzy dokument PDF
            PdfDocument document = new PdfDocument();

            // Pobiera bitmapę wykresu
            Bitmap chartBitmap = getChartBitmap(chart);
            if (chartBitmap == null) {
                throw new IOException("Nie udało się wygenerować bitmapy wykresu");
            }

            // Tworzy stronę PDF z marginesami
            int padding = 50;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    chartBitmap.getWidth() + padding * 2,
                    chartBitmap.getHeight() + padding * 2 + 80, // Dodatkowe miejsce na tytuł
                    1).create();

            PdfDocument.Page page = document.startPage(pageInfo);

            // Rysuje zawartość strony
            Canvas canvas = page.getCanvas();

            // Wypełnia tło
            canvas.drawColor(Color.WHITE);

            // Rysuje tytuł
            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(30);
            canvas.drawText(chartTitle, padding, 60, titlePaint);

            // Rysuje wykres
            canvas.drawBitmap(chartBitmap, padding, padding + 80, null);

            // Kończy stronę
            document.finishPage(page);

            // Zapisuje dokument do pliku
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d(TAG, "Plik PDF zapisany: " + file.getAbsolutePath());

            // Wywołuje callback sukcesu
            listener.onSuccess(file.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas eksportu PDF", e);
            listener.onError(e);
        }
    }

    /**
     * Generuje bitmapę z wykresu.
     *
     * @param chart Obiekt wykresu.
     * @return Bitmapa wykresu.
     */
    private Bitmap getChartBitmap(Chart chart) {
        // Upewnia się, że wykres jest w pełni wyrenderowany
        if (chart instanceof PieChart) {
            ((PieChart) chart).invalidate();
        } else if (chart instanceof BarChart) {
            ((BarChart) chart).invalidate();
        }

        // Pobiera wymiary wykresu
        int width = chart.getWidth();
        int height = chart.getHeight();

        if (width <= 0 || height <= 0) {
            // Ustawia domyślne wymiary dla niewyrenderowanych wykresów
            width = 1200;
            height = 800;

            // Wymusza pomiar i układ
            chart.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            chart.layout(0, 0, width, height);
        }

        // Tworzy bitmapę z odpowiednią konfiguracją
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Rysuje tło wykresu
        canvas.drawColor(Color.WHITE);

        // Rysuje wykres
        chart.draw(canvas);

        return bitmap;
    }
}