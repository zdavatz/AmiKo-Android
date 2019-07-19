package com.ywesee.amiko.barcodereader;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.ywesee.amiko.R;

import java.io.IOException;
import java.util.ArrayList;

public class BarcodeScannerActivity extends AppCompatActivity {
    static final String TAG = "Barcode-reader";
    private static final int REQUEST_CAMERA = 2;

    SurfaceView cameraView;
    CameraSource cameraSource;

    public BarcodeScannerActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.scan_barcode);
        setContentView(R.layout.activity_barcode);

        cameraView = findViewById(R.id.camera_view);
        BarcodeDetector barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.EAN_13 | Barcode.DATA_MATRIX)
                        .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                final ArrayList<String> ean13 = new ArrayList<>();
                final ArrayList<GS1Extractor.Result> dataMatrices = new ArrayList<>();
                GS1Extractor extractor = new GS1Extractor();
                for(int i = 0; i < barcodes.size(); i++) {
                    int key = barcodes.keyAt(i);
                    Barcode barcode = barcodes.get(key);
                    String value = barcode.rawValue;
                    if (barcode.format == Barcode.EAN_13) {
                        ean13.add(value);
                    } else if (barcode.format == Barcode.DATA_MATRIX) {
                        GS1Extractor.Result result = extractor.extract(value);
                        if (result != null) {
                            dataMatrices.add(result);
                        }
                    }
                }
                if (ean13.size() + dataMatrices.size() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraSource.stop();
                            Intent data = new Intent();
                            data.putExtra("ean13", ean13);
                            data.putExtra("dataMatrix", dataMatrices);
                            setResult(0, data);
                            finish();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.stop();
        cameraSource.release();
    }

    void startCamera() {
        int hasPermission = checkSelfPermission(android.Manifest.permission.CAMERA);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            try {
                cameraSource.start(cameraView.getHolder());
                relayout();
            } catch (IOException ie) {
                Log.e("CAMERA SOURCE", ie.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void relayout() {
        Size size = cameraSource.getPreviewSize();
        int width = size.getHeight();
        int height = size.getWidth();
        FrameLayout layout = findViewById(R.id.root);
        int containerWidth = layout.getWidth();
        int containerHeight = layout.getHeight();
        float ratio = Math.max(containerWidth / (float)width, containerHeight / (float)height);
        int targetWidth = (int)(width * ratio);
        int targetHeight = (int)(height * ratio);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(targetWidth, targetHeight);
        p.leftMargin = (containerWidth - targetWidth) / 2;
        p.topMargin = (containerHeight - targetHeight) / 2;
        cameraView.setLayoutParams(p);
    }
}
