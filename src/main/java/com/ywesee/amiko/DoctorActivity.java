package com.ywesee.amiko;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DoctorActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_LIBRARY = 2;
    static final int REQUEST_CODE_ASK_PERMISSIONS = 3;
    static final int REQUEST_CAMERA = 4;

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
    List<FileObserver> fileObservers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        store = new DoctorStore(this);
        fileObservers = Arrays.asList(
                new File(this.getFilesDir(), "doctor.json").getAbsolutePath(),
                new File(this.getFilesDir(), DoctorStore.DOC_SIGNATURE_FILENAME).getAbsolutePath()
        )
                .stream()
                .map(path -> {
                    FileObserver f = new FileObserver(path, FileObserver.MODIFY) {
                        @Override
                        public void onEvent(int i, @Nullable String s) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadFromStore();
                                }
                            });
                        }
                    };
                    f.startWatching();
                    return f;
                })
                .collect(Collectors.toList());

        editTitle = findViewById(R.id.doctor_title);
        editName = findViewById(R.id.doctor_name);
        editSurname = findViewById(R.id.doctor_surname);
        editStreet = findViewById(R.id.doctor_street);
        editCity = findViewById(R.id.doctor_city);
        editZip = findViewById(R.id.doctor_zip);
        editPhone = findViewById(R.id.doctor_phone);
        editEmail = findViewById(R.id.doctor_email);
        imageView = findViewById(R.id.imageView);

        final DoctorActivity _this = this;
        Button selfieButton = findViewById(R.id.button_selfie);
        selfieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int hasPermission = checkSelfPermission(android.Manifest.permission.CAMERA);
                if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(_this,
                            new String[]{android.Manifest.permission.CAMERA},
                            REQUEST_CAMERA);
                } else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
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

    @Override
    protected void onDestroy() {
        for (FileObserver f : fileObservers) {
            f.stopWatching();
        }
        super.onDestroy();
    }

    private void pickDoctorSignatureFromLibrary() {
        int hasPermission = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
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
            case android.R.id.home:
                finish();
                return true;
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
            try {
                InputStream is = getContentResolver().openInputStream(selectedImage);
                Bitmap imageBitmap = BitmapFactory.decodeStream(is);
                InputStream is2 = getContentResolver().openInputStream(selectedImage);
                ExifInterface exif = new ExifInterface(is2);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
                int orientationInt = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        orientationInt = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        orientationInt = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        orientationInt = 270;
                        break;
                    default:
                        orientationInt = 0;
                }

                if (orientationInt != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientationInt);

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
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    pickDoctorSignatureFromLibrary();
                    return;
                }
            }
        } else if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
