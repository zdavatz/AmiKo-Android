package com.ywesee.amiko;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionActivity extends AppCompatActivity {
    // TODO: view for prescription.placeDate

    private Operator doctor;
    private Patient patient;
    private ArrayList<Product> products;

    private TextView doctorNameText;
    private TextView doctorStreetText;
    private TextView doctorZipCityText;
    private TextView doctorPhoneText;
    private TextView doctorEmailText;
    private ImageView doctorImageView;
    private LinearLayout patientLayout;
    private TextView patientNameText;
    private TextView patientWeightHeightGenderBirthdayText;
    private TextView patientStreetText;
    private TextView patientZipCityCountryText;
    private TextView medicinesText;
    private Button saveButton;
    private Button newButton;

    private RecyclerView medicineRecyclerView;
    private MedicineListAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerView amkRecyclerView;
    private AMKListAdapter mAMKAdapter;
    private RecyclerView.LayoutManager mAMKLayoutManager;

    static final int REQUEST_PATIENT = 1;

    public PrescriptionActivity() {
        super();
        products = PrescriptionProductBasket.getShared().products;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prescription);
        setTitle(R.string.prescription_button);

        this.doctorNameText = findViewById(R.id.doctor_name_text);
        this.doctorStreetText = findViewById(R.id.doctor_street_text);
        this.doctorZipCityText = findViewById(R.id.doctor_zip_city_text);
        this.doctorPhoneText = findViewById(R.id.doctor_phone_text);
        this.doctorEmailText = findViewById(R.id.doctor_email_text);
        this.doctorImageView = findViewById(R.id.doctor_image_view);
        this.patientLayout = findViewById(R.id.patient_layout);
        this.patientNameText = findViewById(R.id.patient_name_text);
        this.patientWeightHeightGenderBirthdayText = findViewById(R.id.patient_weight_height_gender_birthday_text);
        this.patientStreetText = findViewById(R.id.patient_street_text);
        this.patientZipCityCountryText = findViewById(R.id.patient_zip_city_country_text);
        this.medicinesText = findViewById(R.id.medicines_text);
        this.medicineRecyclerView = findViewById(R.id.medicine_recycler_view);
        this.amkRecyclerView = findViewById(R.id.amk_recycler_view);

        this.saveButton = findViewById(R.id.save_button);
        this.newButton = findViewById(R.id.new_button);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        medicineRecyclerView.setLayoutManager(mLayoutManager);

        medicineRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mRecyclerAdapter = new MedicineListAdapter();
        mRecyclerAdapter.mDataset = products;
        mRecyclerAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = medicineRecyclerView.getChildLayoutPosition(v);
                Product product = mRecyclerAdapter.mDataset.get(itemPosition);
            }
        };
        mRecyclerAdapter.onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int itemPosition = medicineRecyclerView.getChildLayoutPosition(v);
                final Product product = mRecyclerAdapter.mDataset.get(itemPosition);
                return true;
            }
        };
        medicineRecyclerView.setAdapter(mRecyclerAdapter);

        // use a linear layout manager
        mAMKLayoutManager= new LinearLayoutManager(this);
        amkRecyclerView.setLayoutManager(mAMKLayoutManager);

        amkRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mAMKAdapter = new AMKListAdapter();
        mAMKAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = amkRecyclerView.getChildLayoutPosition(v);
                File file = mAMKAdapter.mDataset.get(itemPosition);
                openPrescriptionFromFile(file);
                drawerLayout.closeDrawers();
            }
        };
        amkRecyclerView.setAdapter(mAMKAdapter);

        // TODO: really read from file system
        mAMKAdapter.mDataset.add("file 1");
        mAMKAdapter.mDataset.add("file 2");
        mAMKAdapter.notifyDataSetChanged();

        final Context _this = this;
        patientLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(_this, PatientListActivity.class);
                startActivityForResult(intent, REQUEST_PATIENT);
                return true;
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    saveNewPrescription();
            }
        });

        this.setDoctor(Operator.loadFromStore(this.getFilesDir().toString()));
        this.reloadMedicinesText();
    }

    public void setDoctor(Operator doctor) {
        this.doctor = doctor;
        this.reloadDoctorTexts();
    }

    public void reloadDoctorTexts() {
        this.doctorNameText.setText(doctor.title + " " + doctor.familyName + " " + doctor.givenName);
        this.doctorStreetText.setText(doctor.postalAddress);
        this.doctorZipCityText.setText(doctor.zipCode + " " + doctor.city);
        this.doctorPhoneText.setText(doctor.phoneNumber);
        this.doctorEmailText.setText(doctor.emailAddress);
        this.doctorImageView.setImageBitmap(doctor.getSignatureImage());
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        this.reloadPatientText();
    }
    public void reloadPatientText() {
        this.patientNameText.setText(patient.familyname + " " + patient.givenname);
        String genderString = patient.gender == Patient.KEY_AMK_PAT_GENDER_M ? "m"
                            : patient.gender == Patient.KEY_AMK_PAT_GENDER_F ? "f" : "";
        this.patientWeightHeightGenderBirthdayText.setText(Integer.toString(patient.weight_kg)+"kg/"+patient.height_cm+"cm " + genderString + " " + patient.birthdate);
        this.patientStreetText.setText(patient.address);
        this.patientZipCityCountryText.setText(patient.zipcode + " " + patient.city + " " + patient.country);
    }

    public void addMedicine(Product p) {
        products.add(p);
        mRecyclerAdapter.addProduct(p);
    public void openPrescriptionFromFile(File file) {
        Prescription p = PrescriptionUtility.readFromFile(file);
        openedPrescription = p;
        openedFile = file;
        setPatient(p.patient);
        setDoctor(p.doctor);
        setProducts(p.medications);
        // TODO: show place date
    }

        this.reloadMedicinesText();
    }
    public void reloadMedicinesText() {
        medicinesText.setText(getString(R.string.search)+"(" + products.size() + ")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.prescription_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.patient_list: {
                Intent intent = new Intent(this, PatientListActivity.class);
                startActivityForResult(intent, REQUEST_PATIENT);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PATIENT && resultCode == 0 && data != null) {
            Patient p = (Patient)data.getSerializableExtra("patient");
            this.setPatient(p);

    Prescription makePrescription(String uniqueId) {
        Prescription p = new Prescription();
        p.patient = patient;
        p.doctor = doctor;
        p.medications = products;
        if (uniqueId == null) {
            p.hash = UUID.randomUUID().toString();
        } else {
            p.hash = uniqueId;
        }
        p.placeDate = doctor.city + " " + PrescriptionUtility.prettyTime();
        return p;
    }

    void saveNewPrescription() {
        Prescription p = makePrescription(null);
        File savedFile = PrescriptionUtility.savePrescription(this, p);
        reloadAMKFileList();
        openedFile = savedFile;
        openedPrescription = p;
    }
        }
    }
}

class MedicineListAdapter extends RecyclerView.Adapter<MedicineListAdapter.ViewHolder> {
    public List<Product> mDataset;
    public View.OnClickListener onClickListener;
    public View.OnLongClickListener onLongClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView packageTextView;
        public TextView eanCodeTextView;
        public TextView commentTextView;
        public ViewHolder(View v, TextView packageTextView, TextView eanCodeTextView, TextView commentTextView) {
            super(v);
            this.packageTextView = packageTextView;
            this.eanCodeTextView = eanCodeTextView;
            this.commentTextView = commentTextView;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MedicineListAdapter() {
        mDataset = new ArrayList<Product>();
    }

    public void addProduct(Product p) {
        mDataset.add(p);
        this.notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MedicineListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        Context context = parent.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView packageTextView = new TextView(context);
        packageTextView.setTextColor(Color.rgb(0, 0, 0));
        packageTextView.setWidth(parent.getWidth());

        TextView eanCodeTextView = new TextView(context);
        eanCodeTextView.setWidth(parent.getWidth());

        TextView commentTextView = new TextView(context);
        commentTextView.setWidth(parent.getWidth());

        layout.addView(packageTextView);
        layout.addView(eanCodeTextView);
        layout.addView(commentTextView);
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

        ViewHolder vh = new ViewHolder(layout, packageTextView, eanCodeTextView, commentTextView);

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product p = mDataset.get(position);
        holder.packageTextView.setText(p.packageInfo);
        holder.eanCodeTextView.setText(p.eanCode);
        holder.commentTextView.setText(p.comment);
        // TODO: need to make this editable
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}

class AMKListAdapter extends RecyclerView.Adapter<AMKListAdapter.ViewHolder> {
    public List<File> mDataset;
    public View.OnClickListener onClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView filenameTextView;
        public ViewHolder(TextView filenameTextView) {
            super(filenameTextView);
            this.filenameTextView = filenameTextView;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AMKListAdapter() {
        mDataset = new ArrayList<File>();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AMKListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        TextView filenameTextView = new TextView(parent.getContext());
        filenameTextView.setOnClickListener(onClickListener);
        filenameTextView.setWidth(parent.getWidth());
        ViewHolder vh = new ViewHolder(filenameTextView);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = mDataset.get(position);
        holder.filenameTextView.setText(file.getName());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
