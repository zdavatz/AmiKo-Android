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
import android.util.Size;

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
    private static float marginX = 50;

    static final int MILS_PER_INCH = 1000;
    static final int POINTS_IN_INCH = 72;

    public static File generatePDF(Context context, Prescription prescription, String filename, Bitmap ePrescriptionQRCode) {
        float margin = mm2pix(18);
//        float fontSize = 11.0;

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
        float medY = mm2pix(110);
        float medSpacing = mm2pix(8);

        PdfDocument.Page page = document.startPage(1);
        Canvas canvas = page.getCanvas();
        if (prescription.medications.size() == 0) {
            drawHeader(originY, canvas, prescription, ePrescriptionQRCode);
        }

        for (int i = 0; i < prescription.medications.size(); i++) {
            Product p = prescription.medications.get(i);
            if (needToDrawHeader) {
                drawPageNumber(context, canvas, pageNumber);
                drawHeader(originY, canvas, prescription, ePrescriptionQRCode);
                needToDrawHeader = false;
                originY = medY;
            }
            originY = drawProduct(p, canvas, originY, a4Height - margin - originY);
            if (originY < 0) {
                originY = 0;
                needToDrawHeader = true;
                pageNumber++;
                document.finishPage(page);
                page = document.startPage(pageNumber);
                canvas = page.getCanvas();
                i--;
            } else {
                originY += medSpacing;
            }
        }
        // do final processing of the last page
        document.finishPage(page);
        try {
            PrescriptionUtility.ensureDirectory(PrescriptionUtility.pdfDirectory(context));
            File f = new File(PrescriptionUtility.pdfDirectory(context), filename);
            FileOutputStream fos = new FileOutputStream(f);
            document.writeTo(fos);
            document.close();
            fos.close();
            return f;
        } catch (IOException e) {
            throw new RuntimeException("Error generating file", e);
        }
    }

    public static float drawHeader(float originY, Canvas canvas, Prescription prescription, Bitmap ePrescriptionQRCode) {
        float fontSize = 11;
//        float filenameY = mm2pix(50);
        float docY = mm2pix(60);
        float patY = mm2pix(60);
        float placeDateY = mm2pix(95);
        String doctorString = prescription.doctor.getStringForPrescriptionPrinting();

        TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(fontSize);

        Size doctorRect = drawMultipleLinesTextWithRightTop(doctorString, canvas, paint, a4Width - marginX, originY + docY);

        String patientString = prescription.patient.getStringForPrescriptionPrinting();
        drawMultipleLinesText(patientString, canvas, paint, marginX, originY + patY);

        Bitmap signature = prescription.doctor.getSignatureThumbnailForPrinting(ePrescriptionQRCode);
        if (signature != null) {
            int signatureX = (int) (a4Width - marginX - doctorRect.getWidth());
            int signatureY = (int) (originY + docY + doctorRect.getHeight());
            canvas.drawBitmap(
                    signature,
                    new Rect(0, 0, signature.getWidth(), signature.getHeight()),
                    new Rect(signatureX, signatureY, signatureX + signature.getWidth(), signatureY + signature.getHeight()),
                    null
            );
        }

        Size placeDateRect = drawMultipleLinesText(prescription.placeDate, canvas, paint, marginX, originY + placeDateY);
        return originY + placeDateY + placeDateRect.getHeight();
    }

    private static float drawProduct(Product product, Canvas canvas, float originY, float availableHeight) {
        // If not enough space -> return -1;
        float fontSize = 11;
        TextPaint paint = new TextPaint();
        paint.setTextSize(fontSize);

        Size packageInfoSize = sizeOfMultipleLinesText(product.packageInfo, paint);
        Size eanSize = sizeOfMultipleLinesText(product.eanCode, paint);
        Size commentSize = new Size(0, 0);
        if (product.comment != null && !product.comment.equals("")) {
            commentSize = sizeOfMultipleLinesText(product.comment, paint);
        }

        if (packageInfoSize.getHeight() + eanSize.getHeight() + commentSize.getHeight() > availableHeight) {
            return -1;
        }

        paint.setColor(Color.rgb(11, 36, 251));
        drawMultipleLinesText(product.packageInfo, canvas, paint, marginX, originY);
        originY += packageInfoSize.getHeight();

        paint.setColor(Color.rgb(128, 128, 128));
        drawMultipleLinesText(product.eanCode, canvas, paint, marginX, originY);
        originY += eanSize.getHeight();

        if (commentSize.getHeight() > 0) {
            paint.setColor(Color.rgb(128, 128, 128));
            drawMultipleLinesText(product.comment, canvas, paint, marginX, originY);
            originY += commentSize.getHeight();
        }

        return originY;
    }

    private static void drawPageNumber(Context context, Canvas canvas, int pageNumber) {
        TextPaint paint = new TextPaint();
        float fontSize = 11;
        paint.setTextSize(fontSize);
        float pageNumberY = mm2pix(50);
        String pageString = context.getString(R.string.page) + " " + Integer.toString(pageNumber);
        drawMultipleLinesTextWithRightTop(pageString, canvas, paint, a4Width - marginX, pageNumberY);
    }

    private static Size drawMultipleLinesTextWithRightTop(String str, Canvas canvas, TextPaint paint, float xOfRight, float top) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, (int)(a4Width - marginX * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float stringWidth = 0;
        for (int i = 0; i < textLayout.getLineCount(); i++) {
            stringWidth  = max(stringWidth , textLayout.getLineMax(i));
        }

        canvas.save();
        canvas.translate(xOfRight - stringWidth, top);
        textLayout.draw(canvas);
        canvas.restore();

        return new Size(
                (int)stringWidth,
                textLayout.getHeight()
        );
    }

    private static Size drawMultipleLinesText(String str, Canvas canvas, TextPaint paint, float left, float top) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, (int)(a4Width - marginX * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float stringWidth = 0;
        for (int i = 0; i < textLayout.getLineCount(); i++) {
            stringWidth  = max(stringWidth , textLayout.getLineMax(i));
        }

        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();

        return new Size(
                (int)stringWidth,
                textLayout.getHeight()
        );
    }

    private static Size sizeOfMultipleLinesText(String str, TextPaint paint) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, (int)(a4Width - marginX * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float stringWidth = 0;
        for (int i = 0; i < textLayout.getLineCount(); i++) {
            stringWidth  = max(stringWidth , textLayout.getLineMax(i));
        }
        return new Size(
                (int)stringWidth,
                textLayout.getHeight()
        );
    }
}
