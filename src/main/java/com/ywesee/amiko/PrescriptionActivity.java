package com.ywesee.amiko;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    private TextView patientNameText;
    private TextView patientWeightHeightGenderBirthdayText;
    private TextView patientStreetText;
    private TextView patientZipCityCountryText;
    private TextView medicinesText;

    private RecyclerView medicineRecyclerView;
    private MedicineListAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public PrescriptionActivity() {
        super();
        products = new ArrayList<Product>();
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
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
        this.patientNameText = findViewById(R.id.patient_name_text);
        this.patientWeightHeightGenderBirthdayText = findViewById(R.id.patient_weight_height_gender_birthday_text);
        this.patientStreetText = findViewById(R.id.patient_street_text);
        this.patientZipCityCountryText = findViewById(R.id.patient_zip_city_country_text);
        this.medicinesText = findViewById(R.id.medicines_text);
        this.medicineRecyclerView = findViewById(R.id.medicine_recycler_view);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        medicineRecyclerView.setLayoutManager(mLayoutManager);

        medicineRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mRecyclerAdapter = new MedicineListAdapter();
        mRecyclerAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = medicineRecyclerView.getChildLayoutPosition(v);
                Product product = mRecyclerAdapter.mDataset.get(itemPosition);
            }
        };
        final Context context = this;
        mRecyclerAdapter.onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int itemPosition = medicineRecyclerView.getChildLayoutPosition(v);
                final Product product = mRecyclerAdapter.mDataset.get(itemPosition);
                return true;
            }
        };
        medicineRecyclerView.setAdapter(mRecyclerAdapter);

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
        this.patientWeightHeightGenderBirthdayText.setText(Integer.toString(patient.weight_kg)+"kg/"+patient.height_cm+"cm " + patient.gender + " " + patient.birthdate);
        this.patientStreetText.setText(patient.address);
        this.patientZipCityCountryText.setText(patient.zipcode + " " + patient.city + " " + patient.country);
    }

    public void addMedicine(Product p) {
        products.add(p);
        mRecyclerAdapter.addProduct(p);
        this.reloadMedicinesText();
    }
    public void reloadMedicinesText() {
        medicinesText.setText(getString(R.string.search)+"(" + products.size() + ")");
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
//        v.setTextSize(25);
//        v.setOnClickListener(onClickListener);
//        v.setOnLongClickListener(onLongClickListener);
        packageTextView.setWidth(parent.getWidth());
//        v.setPadding(50, 30, 0, 30);

        TextView eanCodeTextView = new TextView(context);
        eanCodeTextView.setWidth(parent.getWidth());

        TextView commentTextView = new TextView(context);
        commentTextView.setWidth(parent.getWidth());

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
