package com.ywesee.amiko;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PatientActivity extends AppCompatActivity {
    static final String TAG = "PatientActivity";
    static final int REQUEST_PATIENT = 1;
    static final int REQUEST_CONTACTS_PERMISSON = 2;
    static final int REQUEST_SMARTCARD = 3;

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

    private EditText mSearchField;

    private DrawerLayout mDrawerLayout;
    private ArrayList<ContactListAdapter.Contact> allContacts;
    private RecyclerView mContactsRecyclerView;
    private ContactListAdapter mContactAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);
        setTitle(R.string.menu_patients);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        mSearchField = findViewById(R.id.search_field);
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyContactsAndUpdateSearchResult();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        mContactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        mContactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mContactsRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        mContactAdapter = new ContactListAdapter(new ArrayList<ContactListAdapter.Contact>());
        mContactsRecyclerView.setAdapter(mContactAdapter);

        final Context c = this;
        ((ContactListAdapter) mContactAdapter).onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = mContactsRecyclerView.getChildLayoutPosition(v);
                ContactListAdapter.Contact contact= mContactAdapter.mDataset.get(itemPosition);
                mPatient = contact.toPatient(c.getContentResolver());
                updateUIForPatient();
                // Reset patient so new record will be saved
                mPatient = null;
                mDrawerLayout.closeDrawers();
            }
        };

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View view) {
                queryContacts();
                mSearchField.requestFocus();
            }
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {}
            @Override
            public void onDrawerClosed(@NonNull View view) {
                mSearchField.clearFocus();
            }
            @Override
            public void onDrawerStateChanged(int i) {}
        });

        SmartcardScanResult r = getPrefillFromScanResult();
        if (r != null) {
            updateUIForSmartcardScanResult(r);
        }
    }

    boolean getIsCreateOnly() {
        Intent intent = getIntent();
        return intent.getBooleanExtra("createOnly", false);
    }

    SmartcardScanResult getPrefillFromScanResult() {
        Intent intent = getIntent();
        return (SmartcardScanResult)intent.getSerializableExtra("card_scan_result");
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
            if (mPatient.gender == null) {
                editSex.clearCheck();
            } else if (mPatient.gender.equals(Patient.KEY_AMK_PAT_GENDER_M)) {
                editSex.check(R.id.patient_sex_male);
            } else if (mPatient.gender.equals(Patient.KEY_AMK_PAT_GENDER_F)) {
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

    void updateUIForSmartcardScanResult(SmartcardScanResult r) {
        editName.setText(r.givenName);
        editSurname.setText(r.familyName);
        editBirthday.setText(r.birthDate);
        if (r.gender == null) {
            editSex.clearCheck();
        } else if (r.gender.equals(Patient.KEY_AMK_PAT_GENDER_M)) {
            editSex.check(R.id.patient_sex_male);
        } else if (r.gender.equals(Patient.KEY_AMK_PAT_GENDER_F)) {
            editSex.check(R.id.patient_sex_female);
        } else {
            editSex.clearCheck();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (getIsCreateOnly()) {
            getMenuInflater().inflate(R.menu.patient_create_only_actionbar, menu);
        } else {
            getMenuInflater().inflate(R.menu.patient_actionbar, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
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
                        sexString = Patient.KEY_AMK_PAT_GENDER_M;
                        break;
                    case R.id.patient_sex_female:
                        sexString = Patient.KEY_AMK_PAT_GENDER_F;
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

                for (EditText v : Arrays.asList(editName, editSurname, editStreet, editCity, editZip, editBirthday, editPhone, editEmail)) {
                    if (v.length() == 0) {
                        v.setError(getString(R.string.required));
                        errored = true;
                    }
                }

                if (!errored) {
                    PatientDBAdapter db = new PatientDBAdapter(this);
                    if (mPatient != null) {
                        patient.timestamp = Utilities.currentTimeString();
                        db.updateRecord(patient);
                        showPatientUpdatedAlert();
                    } else {
                        db.insertRecord(patient);
                        showPatientAddedAlert();
                    }
                    db.close();
                    mPatient = patient;
                    Patient.setCurrentPatientId(this, patient.uid);
                    if (getIsCreateOnly()) {
                        Intent intent = new Intent();
                        intent.putExtra("patient", patient);
                        setResult(0, intent);
                        finish();
                    }
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
            case R.id.scan_card: {
                Intent intent = new Intent(this, SmartcardActivity.class);
                startActivityForResult(intent, REQUEST_SMARTCARD);
                return true;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PATIENT && resultCode == 0 && data != null) {
            Patient p = (Patient)data.getSerializableExtra("patient");
            mPatient = p;
            updateUIForPatient();
        } else if (requestCode == REQUEST_SMARTCARD && resultCode == 0 && data != null) {
            SmartcardScanResult r = (SmartcardScanResult)data.getSerializableExtra("result");
            PatientDBAdapter db = new PatientDBAdapter(this);
            Patient p = null;
            try {
                p = r.findExistingPatient(db);
            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
            finally {
                db.close();
            }
            mPatient = p;
            updateUIForPatient();
            if (p == null) {
                updateUIForSmartcardScanResult(r);
            }
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
                        },
                        null,
                        null,
                        null
                );
        if (cursor == null) {
            return;
        }

        ArrayList<ContactListAdapter.Contact> contacts = new ArrayList<ContactListAdapter.Contact>();
        HashMap<String, ContactListAdapter.Contact> contactById = new HashMap<>();
        while(cursor.moveToNext()) {
            ContactListAdapter.Contact c = new ContactListAdapter.Contact();
            String id = cursor.getString(0);
            c.contactId = id;
            contacts.add(c);
            contactById.put(id, c);
        }
        cursor.close();

        Cursor pCur = cr.query(
                ContactsContract.Data.CONTENT_URI,
                new String[] {
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                },
                ContactsContract.Data.MIMETYPE + " = ? ",
                new String[] { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE },
                null);

        while (pCur.moveToNext()) {
            String id = pCur.getString(0);
            ContactListAdapter.Contact c = contactById.get(id);
            if (c == null) {
                continue;
            }
            c.givenName = pCur.getString(1);
            c.familyName = pCur.getString(2);
        }
        pCur.close();

        Collections.sort(contacts, new Comparator<ContactListAdapter.Contact>() {
            @Override
            public int compare(ContactListAdapter.Contact c1, ContactListAdapter.Contact c2) {
                String familyName1 = c1.familyName;
                String familyName2 = c2.familyName;
                String givenName1 = c1.givenName;
                String givenName2 = c2.givenName;
                if (familyName1 == null) {
                    familyName1 = "";
                }
                if (familyName2 == null) {
                    familyName2 = "";
                }
                if (givenName1 == null) {
                    givenName1 = "";
                }
                if (givenName2 == null) {
                    givenName2 = "";
                }
                int result1 = familyName1.compareToIgnoreCase(familyName2);
                if (result1 != 0) {
                    return result1;
                }
                return givenName1.compareToIgnoreCase(givenName2);
            }
        });

        allContacts = contacts;
        applyContactsAndUpdateSearchResult();
    }

    public void applyContactsAndUpdateSearchResult() {
        String searchQuery = mSearchField.getText().toString();
        if (searchQuery.equals("")) {
            mContactAdapter.mDataset = allContacts;
        } else {
            ArrayList<ContactListAdapter.Contact> contacts = new ArrayList<>();
            for (ContactListAdapter.Contact c : allContacts) {
                if (c.stringForDisplay().toLowerCase().contains(searchQuery.toLowerCase())) {
                    contacts.add(c);
                }
            }
            mContactAdapter.mDataset = contacts;
        }
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
            public String givenName;
            public String familyName;

            Patient toPatient(ContentResolver cr) {
                String phoneNumber = null;
                String emailAddress = null;
                String birthday = null;
                String street = null;
                String city = null;
                String country = null;
                String zip = null;

                Cursor emailCur = cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    new String[] {
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                    },
                    ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID + "= ? ",
                    new String[]{ this.contactId },
                    null);

                if (emailCur.moveToFirst()) {
                    emailAddress = emailCur.getString(0);
                }
                emailCur.close();

                Cursor phoneCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] {
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                        },
                        ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID + " = ?",
                        new String[] { this.contactId },
                        null);
                if (phoneCur.moveToFirst()) {
                    phoneNumber = phoneCur.getString(0);
                }
                phoneCur.close();

                Cursor eventCur = cr.query(
                        ContactsContract.Data.CONTENT_URI,
                        new String[] {
                            ContactsContract.CommonDataKinds.Event.START_DATE,
                        },
                        ContactsContract.CommonDataKinds.Event.RAW_CONTACT_ID + "= ? AND "
                        + ContactsContract.Data.MIMETYPE + "= ? AND "
                        + ContactsContract.CommonDataKinds.Event.TYPE + "= " + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                        new String[] {
                            this.contactId,
                            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                        },
                        null);
                if (eventCur.moveToFirst()) {
                    birthday = eventCur.getString(0);
                }
                if (birthday != null) {
                    String[] parts = birthday.split("-");
                    if (parts.length == 3) {
                        birthday = parts[2] + "." + parts[1] + "." + parts[0];
                    } else if (parts.length == 4) {
                        // birthday event without year
                        birthday = parts[3] + "." + parts[2] + ".";
                    }
                }
                eventCur.close();

                Cursor addressCur = cr.query(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        new String[] {
                                ContactsContract.CommonDataKinds.StructuredPostal.CITY,
                                ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY,
                                ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                                ContactsContract.CommonDataKinds.StructuredPostal.STREET,
                        },
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + "= ? ",
                        new String[] {
                                this.contactId,
                        },
                        null);
                if (addressCur.moveToFirst()) {
                    city = addressCur.getString(0);
                    country = addressCur.getString(1);
                    zip = addressCur.getString(2);
                    street = addressCur.getString(3);
                }
                addressCur.close();

                Patient p = new Patient();
                p.givenname = this.givenName;
                p.familyname = this.familyName;
                p.email = emailAddress;
                p.birthdate = birthday;
                p.phone = phoneNumber;
                p.city = city;
                p.country = country;
                p.zipcode = zip;
                p.address = street;
                return p;
            }

            public String stringForDisplay() {
                String f = this.familyName;
                String g= this.givenName;
                if (f == null) {
                    f = "";
                }
                if (g == null) {
                    g = "";
                }
                return f + " " + g;
            }
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
            Contact c = mDataset.get(position);
            holder.mTextView.setText(c.stringForDisplay());
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
