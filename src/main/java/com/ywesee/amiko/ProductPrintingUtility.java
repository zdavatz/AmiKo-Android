package com.ywesee.amiko;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.nsd.NsdServiceInfo;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.cups4j.PrintRequestResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


public class ProductPrintingUtility {

    public static PrintRequestResult printToService(Context context, CupsPrinter cupsPrinter, Operator doctor, Patient patient, Product product) throws Exception {
        PrintedPdfDocument pdfDocument = generatePDF(context, doctor, patient, product);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            pdfDocument.writeTo(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] pdfBytes = stream.toByteArray();
        pdfDocument.close();

        PrintJob printJob = new PrintJob.Builder(new ByteArrayInputStream(pdfBytes))
                .pageFormat("w102h252") // 72 point per inch, w102 h252 refers to the Dymo 30321 Large Address Labels
                .portrait(false)
                .build();
        PrintRequestResult printRequestResult = cupsPrinter.print(printJob);
        return printRequestResult;
    }

    /**
     * The caller is responsible for document.close();
     */
    private static PrintedPdfDocument generatePDF(Context context, Operator doctor, Patient patient, Product product) {
        PrintAttributes printAttrs = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(new PrintAttributes.MediaSize("dymo", "Custom", (int)(89 / 25.4f * 1000f), (int)(36 / 25.4f * 1000f)))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        PrintedPdfDocument pdfDocument = new PrintedPdfDocument(context, printAttrs);
        // The official is w102h252, need rotate, so we set .portrait(false) at print job
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(252, 102, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Start drawing
        Canvas canvas = page.getCanvas();
        // units are in points (1/72 of an inch)

        TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(9);

        drawMultipleLinesText(doctor.getStringForLabelPrinting(), canvas, paint, 15, 10, 231);
        canvas.drawRect(15, 22, 244, 24, paint);

        if (patient != null) {
            drawMultipleLinesText(patient.getStringForLabelPrinting(context), canvas, paint, 15, 29, 231);
        }

        TextPaint boldPaint = new TextPaint();
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(9);

        String[] packageParts = product.packageInfo.split(", ");
        drawMultipleLinesText(packageParts[0], canvas, boldPaint , 15, 47, 231);

        drawMultipleLinesText(product.comment, canvas, paint, 15, 59, 231);

        String[] swissmedArray = product.packageInfo.split(" \\[");
        if (swissmedArray.length >= 2) {
            drawMultipleLinesText("[" + swissmedArray[1], canvas, paint, 15, 87, 111);
        }

        if (packageParts.length > 2) {
            String[] priceParts = packageParts[2].split(" ");
            if (priceParts[0].equals("PP")) {
                drawMultipleLinesText("CHF\t" + priceParts[1], canvas, paint, 175, 87, 79);
            }
        }
        // End drawing
        pdfDocument.finishPage(page);
        return pdfDocument;
    }

    private static void drawMultipleLinesText(String str, Canvas canvas, TextPaint paint, float left, float top, int maxWidth) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();
    }
}
