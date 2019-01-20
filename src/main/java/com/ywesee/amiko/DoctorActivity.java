package com.ywesee.amiko;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class DoctorActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_LIBRARY = 2;
    static final int REQUEST_CODE_ASK_PERMISSIONS = 3;

    static final int MIN_SIGNATURE_HEIGHT = 45;
    static final int MIN_SIGNATURE_WIDTH = 90;

    private EditText editTitle;
    private EditText editName;
    private EditText editSurname;
    private EditText editStreet;
    private EditText editCity;
    private EditText editZip;
    private EditText editPhone;
    private EditText editEmail;
    private ImageView imageView;

    DoctorStore store;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        store = new DoctorStore(this.getFilesDir().toString());

        editTitle = findViewById(R.id.doctor_title);
        editName = findViewById(R.id.doctor_name);
        editSurname = findViewById(R.id.doctor_surname);
        editStreet = findViewById(R.id.doctor_street);
        editCity = findViewById(R.id.doctor_city);
        editZip = findViewById(R.id.doctor_zip);
        editPhone = findViewById(R.id.doctor_phone);
        editEmail = findViewById(R.id.doctor_email);
        imageView = findViewById(R.id.imageView);

        Button selfieButton = findViewById(R.id.button_selfie);
        selfieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        Button photoLibraryButton = findViewById(R.id.button_photo);
        photoLibraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDoctorSignatureFromLibrary();
            }
        });
        this.loadFromStore();
    }

    private void pickDoctorSignatureFromLibrary() {
        int hasPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_LIBRARY);
            }
        }
    }

    private void loadFromStore() {
        store.load();
        editTitle.setText(store.title);
        editName.setText(store.name);
        editSurname.setText(store.surname);
        editStreet.setText(store.street);
        editCity.setText(store.city);
        editZip.setText(store.zip);
        editPhone.setText(store.phone);
        editEmail.setText(store.email);
        imageView.setImageBitmap(store.getSignature());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.doctor_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save: {
                boolean errored = false;
                store.title = editTitle.getText().toString();
                for (EditText v : Arrays.asList(editName, editSurname, editStreet, editCity, editZip, editPhone, editEmail)) {
                    if (v.length() == 0) {
                        v.setError(getString(R.string.required));
                        errored = true;
                    }
                }

                store.name = editName.getText().toString();
                store.surname = editSurname.getText().toString();
                store.street = editStreet.getText().toString();
                store.city = editCity.getText().toString();
                store.zip = editZip.getText().toString();
                store.phone = editPhone.getText().toString();
                store.email = editEmail.getText().toString();
                if (!errored) {
                    store.save();
                    finish();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            saveSignatureImage(imageBitmap);
        } else if (requestCode == REQUEST_IMAGE_LIBRARY && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            Cursor cursor = getContentResolver().query(selectedImage,
                    new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

            cursor.moveToFirst();
            int orientation = cursor.getInt(0);
            try {
                InputStream is = getContentResolver().openInputStream(selectedImage);
                Bitmap imageBitmap = BitmapFactory.decodeStream(is);
                if (orientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);

                    imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(),
                            imageBitmap.getHeight(), matrix, true);
                }
                saveSignatureImage(imageBitmap);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickDoctorSignatureFromLibrary();
            }
        }
    }

    private void saveSignatureImage(Bitmap imageBitmap) {
        double targetWidth = imageBitmap.getWidth() * 0.3;
        double targetHeight = imageBitmap.getHeight() * 0.3;
        if (targetWidth < MIN_SIGNATURE_WIDTH) {
            targetHeight *= MIN_SIGNATURE_WIDTH / targetWidth;
            targetWidth = MIN_SIGNATURE_WIDTH;
        }
        if (targetHeight < MIN_SIGNATURE_HEIGHT) {
            targetWidth *= MIN_SIGNATURE_HEIGHT / targetHeight;
            targetHeight = MIN_SIGNATURE_HEIGHT;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, (int)targetWidth, (int)targetHeight, false);
        imageView.setImageBitmap(scaled);
        store.saveSignature(scaled);
    }
}
