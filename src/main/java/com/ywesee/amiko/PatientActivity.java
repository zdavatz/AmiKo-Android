package com.ywesee.amiko;

import android.content.Intent;
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

    static final int REQUEST_PATIENT = 1;

    private Patient mPatient;

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
        setTitle(R.string.menu_patients);

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

    public void updateUIForPatient() {
        if (mPatient != null) {
            editName.setText(mPatient.givenname);
            editSurname.setText(mPatient.familyname);
            editStreet.setText(mPatient.address);
            editCity.setText(mPatient.city);
            editZip.setText(mPatient.zipcode);
            editCountry.setText(mPatient.country);
            editBirthday.setText(mPatient.birthdate);
            if (mPatient.gender.equals("man")) {
                editSex.check(R.id.patient_sex_male);
            } else if (mPatient.gender.equals("women")) {
                editSex.check(R.id.patient_sex_female);
            } else {
                editSex.clearCheck();
            }
            if (mPatient.weight_kg == 0) {
                editWeight.setText("");
            } else {
                editWeight.setText(String.format("%d", mPatient.weight_kg));
            }
            if (mPatient.height_cm == 0) {
                editHeight.setText("");
            } else {
                editHeight.setText(String.format("%d", mPatient.height_cm));
            }
            editPhone.setText(mPatient.phone);
            editEmail.setText(mPatient.email);
        } else {
            editName.setText("");
            editSurname.setText("");
            editStreet.setText("");
            editCity.setText("");
            editZip.setText("");
            editCountry.setText("");
            editBirthday.setText("");
            editSex.clearCheck();
            editWeight.setText("");
            editHeight.setText("");
            editPhone.setText("");
            editEmail.setText("");
        }
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
                Patient patient;
                if (mPatient == null) {
                    patient = new Patient();
                } else {
                    patient = mPatient;
                }
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
                    if (mPatient != null) {
                        db.updateRecord(patient);
                        showPatientUpdatedAlert();
                    } else {
                        db.insertRecord(patient);
                        showPatientAddedAlert();
                    }
                    db.close();
                    mPatient = patient;
                }
                return true;
            }
            case R.id.patient_list: {
                Intent intent = new Intent(this, PatientListActivity.class);
                startActivityForResult(intent, REQUEST_PATIENT);
                return true;
            }
            case R.id.new_patient: {
                mPatient = null;
                updateUIForPatient();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PATIENT && resultCode == 0) {
            Patient p = (Patient)data.getSerializableExtra("patient");
            mPatient = p;
            updateUIForPatient();
        }
    }

    void showEmptySexAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sex_not_selected))
                .setMessage(getString(R.string.please_select_sex))
                .setPositiveButton("OK", null)
                .show();
    }

    void showPatientUpdatedAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.patient_updated))
                .setPositiveButton("OK", null)
                .show();
    }

    void showPatientAddedAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.patient_added))
                .setPositiveButton("OK", null)
                .show();
    }
}
