package com.ywesee.amiko;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class PatientActivity extends AppCompatActivity {

    static final int REQUEST_PATIENT = 1;
    static final int REQUEST_CONTACTS_PERMISSON = 2;

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

    private DrawerLayout mDrawerLayout;
    private RecyclerView mContactsRecyclerView;
    private ContactListAdapter mContactAdapter;

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

        mContactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        mContactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mContactsRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        mContactAdapter = new ContactListAdapter(new ArrayList<ContactListAdapter.Contact>());
        mContactsRecyclerView.setAdapter(mContactAdapter);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View view) {
                queryContacts();
            }
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {}
            @Override
            public void onDrawerClosed(@NonNull View view) {}
            @Override
            public void onDrawerStateChanged(int i) {}
        });
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

    void queryContacts() {
        int hasPermission = checkSelfPermission(Manifest.permission.READ_CONTACTS);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACTS_PERMISSON);
            return;
        }
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                        new String[] {
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
//                                ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER,
//                                ContactsContract.CommonDataKinds.Phone.NUMBER,
//                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                        },
                        null,
                        null,
                        null
                );
        if (cursor == null) {
            return;
        }
        ArrayList<ContactListAdapter.Contact> contacts = new ArrayList<ContactListAdapter.Contact>();
        while(cursor.moveToNext()) {
            String id = cursor.getString(0);
            Cursor pCur = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
                    },
                    ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID + "= ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{ id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE },
                    null);
            ContactListAdapter.Contact c = new ContactListAdapter.Contact();
            while (pCur.moveToNext()) {
                c.contactId = id;
                c.givenName = pCur.getString(0);
                c.familyName = pCur.getString(1);
                c.displayName = cursor.getString(1);
                contacts.add(c);
            }
            pCur.close();
        }
        cursor.close();
        mContactAdapter.mDataset = contacts;
        mContactAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.READ_CONTACTS) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                queryContacts();
                return;
            }
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



    static class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {
        public List<ContactListAdapter.Contact> mDataset;
        public View.OnClickListener onClickListener;
        public View.OnLongClickListener onLongClickListener;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;
            public ViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }
        }

        static class Contact {
            public String contactId;
            public String displayName;
            public String givenName;
            public String familyName;
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public ContactListAdapter(List<ContactListAdapter.Contact> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ContactListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {
            // create a new view
            TextView v = new TextView(parent.getContext());
            v.setTextSize(20);
            v.setOnClickListener(onClickListener);
            v.setOnLongClickListener(onLongClickListener);
            v.setWidth(parent.getWidth());
            v.setPadding(50, 30, 0, 30);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mDataset.get(position).displayName);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
