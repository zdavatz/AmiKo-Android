package com.ywesee.amiko;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.Arrays;

public class PatientActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editSurname;
    private EditText editStreet;
    private EditText editCity;
    private EditText editZip;
    private EditText editCountry;
    private EditText editBirthday;
    private RadioGroup editSex;
    private EditText editWeight;
    private EditText editHeight;
    private EditText editPhone;
    private EditText editEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        editName = findViewById(R.id.patient_name);
        editSurname = findViewById(R.id.patient_surname);
        editStreet = findViewById(R.id.patient_street);
        editCity = findViewById(R.id.patient_city);
        editZip = findViewById(R.id.patient_zip);
        editCountry = findViewById(R.id.patient_country);
        editBirthday = findViewById(R.id.patient_birthday);
        editSex = findViewById(R.id.patient_sex);
        editWeight = findViewById(R.id.patient_weight);
        editHeight = findViewById(R.id.patient_height);
        editPhone = findViewById(R.id.patient_phone);
        editEmail = findViewById(R.id.patient_email);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.patient_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save: {
                Patient patient = new Patient();
                patient.familyname = editSurname.getText().toString();
                patient.givenname = editName.getText().toString();
                patient.birthdate = editBirthday.getText().toString();

                String sexString = null;
                switch (editSex.getCheckedRadioButtonId()) {
                    case R.id.patient_sex_male:
                        sexString = "man";
                        break;
                    case R.id.patient_sex_female:
                        sexString = "women";
                        break;
                    default:
                        showEmptySexAlert();
                        return false;
                }
                patient.gender = sexString;
                try {
                    patient.weight_kg = Integer.parseInt(editWeight.getText().toString());
                } catch(Exception e) {
                    patient.weight_kg = 0;
                }
                try {
                    patient.height_cm = Integer.parseInt(editHeight.getText().toString());
                } catch(Exception e) {
                    patient.height_cm = 0;
                }
                patient.zipcode = editZip.getText().toString();
                patient.city = editCity.getText().toString();
                patient.country = editCountry.getText().toString();
                patient.address = editStreet.getText().toString();
                patient.phone = editPhone.getText().toString();
                patient.email = editEmail.getText().toString();

                boolean errored = false;

                for (EditText v : Arrays.asList(editName, editSurname, editStreet, editCity, editZip, editBirthday)) {
                    if (v.length() == 0) {
                        v.setError(getString(R.string.required));
                        errored = true;
                    }
                }

                if (!errored) {
                    PatientDBAdapter db = new PatientDBAdapter(this);
                    db.insertRecord(patient);
                    db.close();
                    finish();
                }
                return true;
            }
        }
        return false;
    }

    void showEmptySexAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sex_not_selected))
                .setMessage(getString(R.string.please_select_sex))
                .setPositiveButton("OK", null)
                .show();
    }
}
