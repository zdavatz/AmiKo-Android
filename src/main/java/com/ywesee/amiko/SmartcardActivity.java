/*
Copyright (c) 2019 Brian Chan

This file is part of AmiKo for Android.

AmiKo for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.ywesee.amiko;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.FrameLayout;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.ywesee.amiko.Patient.KEY_AMK_PAT_GENDER_F;
import static com.ywesee.amiko.Patient.KEY_AMK_PAT_GENDER_M;

public class SmartcardActivity extends AppCompatActivity {
    static final String TAG = "Smartcard";
    private static final int REQUEST_CAMERA = 2;
    private static final int NUNBER_OF_FIELD_IN_CARD = 3;

    SurfaceView cameraView;
    CameraSource cameraSource;
    ImageView imageView;

    RectF cardRect;
    TextRecognizer textRecognizer;
    SparseArray<TextBlock> currentDetectedTexts;
    Frame.Metadata currentDetectedMetadata;

    Text detectedNameText;
    Text detectedBirthdaySexText;
    ArrayList<Text> okTexts;
    ArrayList<Text> notOkTexts;

    public SmartcardActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.scan_smartcard);
        setContentView(R.layout.activity_smartcard);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cameraView = findViewById(R.id.camera_view);
        imageView = findViewById(R.id.image_view);

        textRecognizer = new TextRecognizer.Builder(this).build();
        cameraSource = new CameraSource
                .Builder(this, textRecognizer)
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

        cameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    // processCurrentFrame();
                    if (detectedNameText != null && detectedBirthdaySexText != null) {
                        submitCurrentDetectedText();
                    }
                }
                return true;
            }
        });
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                currentDetectedMetadata = detections.getFrameMetadata();
                currentDetectedTexts = detections.getDetectedItems();
                final SparseArray<TextBlock> textBlocks = detections.getDetectedItems();
                processCurrentFrame();
            }
        });
    }

    private void processCurrentFrame() {
        SparseArray<TextBlock> textBlocks = currentDetectedTexts;
        Frame.Metadata metadata = currentDetectedMetadata;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cameraView.getLayoutParams();
        float leftPercentage = (cardRect.left - (float) params.leftMargin) / (float) params.width;
        float topPercentage = (cardRect.top - (float) params.topMargin) / (float) params.height;
        float rightPercentage = (cardRect.right - (float) params.leftMargin) / (float) params.width;
        float bottomPercentage = (cardRect.bottom - (float) params.topMargin) / (float) params.height;
        float cropX = metadata.getWidth() * leftPercentage;
        float cropY = metadata.getHeight() * topPercentage;
        float cropRight= metadata.getWidth() * rightPercentage;
        float cropBottom = metadata.getHeight() * bottomPercentage;
        Rect rect = new Rect(
                (int) cropX,
                (int) cropY,
                (int) cropRight,
                (int) cropBottom);

        ArrayList<Text> allTexts = new ArrayList<>();
        for (int i = 0; i < textBlocks.size(); i++) {
            int key = textBlocks.keyAt(i);
            TextBlock t = textBlocks.get(key);
            List<? extends Text> texts = t.getComponents();
            allTexts.addAll(texts);
        }

        ArrayList<Text> textsInCard = new ArrayList<>();
        for (Text t : allTexts) {
            if (Rect.intersects(rect, t.getBoundingBox()) || rect.contains(t.getBoundingBox())) {
                textsInCard.add(t);
            }
        }

        ArrayList<Text> lowerLeftTexts = new ArrayList<>();
        for (Text t : textsInCard) {
            float yPercentage = (t.getBoundingBox().top - rect.top) / ((float) rect.height());
            float xPercentage = (t.getBoundingBox().left - rect.left) / ((float) rect.width());

            // Discards text in the top area of the card
            // Discard text in the right area of the card
            if (yPercentage >= 0.5 && xPercentage <= 0.2) {
                lowerLeftTexts.add(t);
            }
        }

        ArrayList<Text> filteredText = new ArrayList<>();
        for (Text t : lowerLeftTexts) {
            if (!t.getValue().toLowerCase().startsWith("name,") &&
                !t.getValue().toLowerCase().contains("vorname") &&
                !t.getValue().toLowerCase().startsWith("karten") &&
                !t.getValue().toLowerCase().startsWith("geburtsdatum")
            ) {
                filteredText.add(t);
            }
        }
        ArrayList<Text> goodBoxes = analyzeVisionBoxes(filteredText);
        // We expect to have
        //  goodBoxes[0] FamilyName, GivenName
        //  goodBoxes[1] CardNumber (unused)
        //  goodBoxes[2] Birthday Sex

        okTexts = new ArrayList<>();
        notOkTexts = new ArrayList<>();
        for (Text t : allTexts) {
            if (goodBoxes.contains(t)) {
                okTexts.add(t);
            } else {
                notOkTexts.add(t);
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                relayout();
            }
        });

        if (goodBoxes.size() != NUNBER_OF_FIELD_IN_CARD) {
            Log.w(TAG, "Wrong number of field");
            return;
        }

        Text firstBox = goodBoxes.get(0);

        String name = firstBox.getValue().replace('.', ',');
        String[] nameArray = name.split(",");
        if (nameArray.length < 2) {
            Log.w(TAG, "Cannot parse name");
            return;
        }

        String[] dateArray = tryBirthdayAndSex(goodBoxes.get(1));

        if (dateArray != null) {
            detectedBirthdaySexText = goodBoxes.get(1);
        } else {
            dateArray = tryBirthdayAndSex(goodBoxes.get(2));
            if (dateArray != null) {
                detectedBirthdaySexText = goodBoxes.get(2);
            } else {
                return;
            }
        }

        detectedNameText = firstBox;

        // TODO: remove after some time?
    }

    String[] tryBirthdayAndSex(Text input) {
        String[] dateArray = input.getValue().split(" ");
        if (dateArray.length < 2) {
            return null;
        }
        if (dateArray[1].equals("F") || dateArray[1].equals("M")) {
            return dateArray;
        }
        return null;
    }

    void submitCurrentDetectedText() {
        String name = detectedNameText.getValue().replace('.', ',');
        String[] nameArray = name.split(",");
        String[] dateArray = detectedBirthdaySexText.getValue().split(" ");
        if (nameArray.length < 2 || dateArray.length < 2) {
            Log.w(TAG, "Cannot parse name / date");
            return;
        }

        String givenName = nameArray[1].trim();
        if (givenName.endsWith("-")) {
            givenName = givenName.substring(0, givenName.length() - 1);
        }
        final SmartcardScanResult r = new SmartcardScanResult();
        r.familyName = nameArray[0];
        r.givenName = givenName;
        r.birthDate = dateArray[0];
        if (dateArray[1].equals("M")) {
            r.gender = KEY_AMK_PAT_GENDER_M;
        } else if (dateArray[1].equals("F")) {
            r.gender = KEY_AMK_PAT_GENDER_F;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent data = new Intent();
                data.putExtra("result", r);
                setResult(0, data);
                finish();
            }
        });
    }

    ArrayList<Text> analyzeVisionBoxes(ArrayList<Text> boxes) {
        if (boxes.size() <= NUNBER_OF_FIELD_IN_CARD) {
            return boxes;
        }
        // Sort by Y
        boxes.sort(new Comparator<Text>() {
            @Override
            public int compare(Text o1, Text o2) {
                return o1.getBoundingBox().top - o2.getBoundingBox().top;
            }
        });
        if (boxes.size() > 5) {
            return new ArrayList<>(boxes.subList(0, 5));
        }
        // Sort by height
        boxes.sort(new Comparator<Text>() {
            @Override
            public int compare(Text o1, Text o2) {
                return o1.getBoundingBox().height() - o2.getBoundingBox().height();
            }
        });
        // Keep only the first NUNBER_OF_FIELD_IN_CARD
        boxes = new ArrayList<>(boxes.subList(0, NUNBER_OF_FIELD_IN_CARD));
        // Sort them back by Y
        boxes.sort(new Comparator<Text>() {
            @Override
            public int compare(Text o1, Text o2) {
                return o1.getBoundingBox().top - o2.getBoundingBox().top;
            }
        });
        return boxes;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.stop();
        cameraSource.release();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.getItemId();
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

        float cardAspectRatio = 85.6f / 53.98f; // w/h 1.585
        float cardX = containerWidth * 0.04f;
        float cardW = containerWidth - (2.0f * cardX);
        float cardH = cardW / cardAspectRatio;
        float cardY = containerHeight/2.0f - cardH/2.0f; // Center vertically for drawing
        if (cardY < 0) {
            cardY = containerHeight * 0.1f; // top margin: 10% of the height
            cardH = containerHeight - 2.0f * cardY;
            cardW = cardH * cardAspectRatio;
            cardX = (containerWidth-cardW) / 2.0f; // Center horizontally for drawing
        }

        Bitmap bitmap = Bitmap.createBitmap(containerWidth, containerHeight, Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(20, 240, 30));
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        this.cardRect = new RectF(cardX,
            cardY,
            cardX + cardW,
            cardY + cardH);
        canvas.drawRoundRect(
            this.cardRect,
            5, // rx
            5, // ry
            paint);

        float scaleFactor = 0.05f;    // 5%
        float dx = cardW * scaleFactor;
        float dy = cardH * scaleFactor;
        RectF inner = new RectF(cardRect);
        inner.inset(dx, dy);
        canvas.drawRoundRect(
            inner,
            10, // rx
            10, // ry
            paint);

        if (detectedNameText != null) {
            RectF rect = cameraRectToViewRect(detectedNameText.getBoundingBox());

            Paint paint1 = new Paint();
            paint1.setColor(Color.rgb(20, 30, 240));
            paint1.setStrokeWidth(2);
            paint1.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(
                rect,
                2, // rx
                2, // ry
                paint1);
        }
        if (detectedBirthdaySexText != null) {
            RectF rect = cameraRectToViewRect(detectedBirthdaySexText.getBoundingBox());

            Paint paint1 = new Paint();
            paint1.setColor(Color.rgb(20, 30, 240));
            paint1.setStrokeWidth(2);
            paint1.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(
                rect,
                2, // rx
                2, // ry
                paint1);
        }

        imageView.setImageBitmap(bitmap);
    }

    RectF cameraRectToViewRect(Rect cameraRect) {
        Frame.Metadata metadata = currentDetectedMetadata;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cameraView.getLayoutParams();
        if (currentDetectedMetadata == null) return null;
        float left = cameraRect.left * ((float)params.width / (float)metadata.getWidth()) + (float) params.leftMargin;
        float right = cameraRect.right * ((float)params.width / (float)metadata.getWidth()) + (float) params.leftMargin;
        float top = cameraRect.top * ((float)params.height / (float)metadata.getHeight()) + (float) params.topMargin;
        float bottom = cameraRect.bottom * ((float)params.height / (float)metadata.getHeight()) + (float) params.topMargin;
        return new RectF(left, top, right, bottom);
    }
}
