package com.ywesee.amiko;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Size;

import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.Float.max;

public class ProductPrintDocumentAdapter extends PrintDocumentAdapter {
    private Product product;
    private Operator doctor;
    private Patient patient;
    private Context context;

    PrintedPdfDocument pdfDocument;

    ProductPrintDocumentAdapter(Context c, Product p, Operator doctor, Patient patient) {
        super();
        this.product = p;
        this.doctor = doctor;
        this.patient = patient;
        context = c;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        pdfDocument = new PrintedPdfDocument(context, newAttributes);

        // Respond to cancellation request
        if (cancellationSignal.isCanceled() ) {
            callback.onLayoutCancelled();
            return;
        }

        // Return print information to print framework
        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("print_output.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();
        // Content layout reflow is complete
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(252, 102, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // check for cancellation
        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            pdfDocument.close();
            pdfDocument = null;
            return;
        }

        drawPage(page);
        pdfDocument.finishPage(page);

        // Write PDF document to file
        try {
            pdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            pdfDocument.close();
            pdfDocument = null;
        }
        PageRange[] writtenPages = new PageRange[]{ PageRange.ALL_PAGES };
        callback.onWriteFinished(writtenPages);
    }

    private void drawPage(PdfDocument.Page page) {
        Canvas canvas = page.getCanvas();
        // units are in points (1/72 of an inch)

        TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(9);

        drawMultipleLinesText(this.doctor.getStringForLabelPrinting(), canvas, paint, 15, 10, 231);
        canvas.drawRect(15, 22, 244, 24, paint);

        if (this.patient != null) {
            drawMultipleLinesText(this.patient.getStringForLabelPrinting(this.context), canvas, paint, 15, 29, 231);
        }

        TextPaint boldPaint = new TextPaint();
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(9);

        String[] packageParts = this.product.packageInfo.split(", ");
        drawMultipleLinesText(packageParts[0], canvas, boldPaint , 15, 47, 231);

        drawMultipleLinesText(this.product.comment, canvas, paint, 15, 59, 231);

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
    }

    private void drawMultipleLinesText(String str, Canvas canvas, TextPaint paint, float left, float top, int maxWidth) {
        StaticLayout textLayout = new StaticLayout(
                str, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();
    }
}
