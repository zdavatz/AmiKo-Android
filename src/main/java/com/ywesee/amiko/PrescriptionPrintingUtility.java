package com.ywesee.amiko;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.Context.PRINT_SERVICE;
import static java.lang.Float.max;

public class PrescriptionPrintingUtility {

    private static float mm2inch = 1/25.4f;
    private static float inch2pix(float x) {
        return x * POINTS_IN_INCH;
    }
    private static float mm2pix(float x) {
        return inch2pix(x * mm2inch);
    }
    private static float a4Width = mm2pix(210);
    private static float a4Height = mm2pix(297);

    static final int MILS_PER_INCH = 1000;
    static final int POINTS_IN_INCH = 72;

    public static void generatePDF(Context context, Prescription prescription) {
        float margin = mm2pix(18);
//        float fontSize = 11.0;

        float medY = mm2pix(110);
        float medSpacing = mm2pix(8);

        PrintAttributes.MediaSize a4 = PrintAttributes.MediaSize.ISO_A4;
        PrintAttributes printAttrs = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(a4)
                .setResolution(new PrintAttributes.Resolution("amiko", PRINT_SERVICE, POINTS_IN_INCH, POINTS_IN_INCH))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        PrintedPdfDocument document = new PrintedPdfDocument(context, printAttrs);
        // crate a page description
        int pageNumber = 1;
        float originY = 0;
        boolean needToDrawHeader = true;
        PdfDocument.Page page = document.startPage(1);
        Canvas canvas = page.getCanvas();
        if (prescription.medications.size() == 0) {
            drawHeader(originY, canvas, prescription);
        }
        for (Product p : prescription.medications) {
            if (needToDrawHeader) {
                drawHeader(originY, canvas, prescription);
                needToDrawHeader = false;
            }
            if (originY > a4Height - margin) {
                // next page
            }
        }
        // do final processing of the page
        document.finishPage(page);
        // Here you could add more pages in a longer doc app, but you'd have
        // to handle page-breaking yourself in e.g., write your own word processor...
        // Now write the PDF document to a file; it actually needs to be a file
        // since the Share mechanism can't accept a byte[]. though it can
        // accept a String/CharSequence. Meh.
        try {
            File f = new File(context.getFilesDir() + "/test.pdf");
            FileOutputStream fos = new FileOutputStream(f);
            document.writeTo(fos);
            document.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException("Error generating file", e);
        }
    }

    public static void drawHeader(float originY, Canvas canvas, Prescription prescription) {
        float marginX = 50;
        float fontSize = 11;
//        float filenameY = mm2pix(50);
        float docY = mm2pix(60);
        float patY = mm2pix(60);
        float placeDateY = mm2pix(95);
        String doctorString = prescription.doctor.getStringForPrescriptionPrinting();

        TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(fontSize);

        Rect doctorRect = drawMultipleLinesTextWithRightTop(doctorString, canvas, paint, a4Width - marginX, originY + docY);

        String patientString = prescription.patient.getStringForPrescriptionPrinting();
        drawMultipleLinesText(patientString, canvas, paint, marginX, originY + patY);

        Bitmap signature = prescription.doctor.getSignatureThumbnailForPrinting();
        if (signature != null) {
            int signatureX = (int) (a4Width - doctorRect.width() - marginX);
            int signatureY = (int) (originY + docY + doctorRect.height());
            canvas.drawBitmap(
                    signature,
                    new Rect(0, 0, signature.getWidth(), signature.getHeight()),
                    new Rect(signatureX, signatureY, signatureX + signature.getWidth(), signatureY + signature.getHeight()),
                    null
            );
        }

        drawMultipleLinesText(prescription.placeDate, canvas, paint, marginX, originY + placeDateY);
    }

    private static Rect drawMultipleLinesTextWithRightTop(String str, Canvas canvas, TextPaint paint, float xOfRight, float top) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, (int)a4Width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float stringWidth = 0;
        for (int i = 0; i < textLayout.getLineCount(); i++) {
            stringWidth  = max(stringWidth , textLayout.getLineMax(i));
        }

        canvas.save();
        canvas.translate(xOfRight - stringWidth, top);
        textLayout.draw(canvas);
        canvas.restore();

        return new Rect(
                0,
                0,
                (int)stringWidth,
                textLayout.getHeight()
        );
    }

    private static void drawMultipleLinesText(String str, Canvas canvas, TextPaint paint, float left, float top) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, (int)a4Width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();
    }
}
