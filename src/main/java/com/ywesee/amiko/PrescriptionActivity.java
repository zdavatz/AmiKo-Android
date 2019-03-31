package com.ywesee.amiko;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PrescriptionActivity extends AppCompatActivity {
    private Operator doctor;
    private Patient patient;
    private ArrayList<Product> products;
    private File openedFile = null;
    private Prescription openedPrescription;

    private TextView placeDateText;
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
    private Button sendButton;
    private Button interactionButton;

    private RecyclerView medicineRecyclerView;
    private MedicineListAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerView amkRecyclerView;
    private AMKListAdapter mAMKAdapter;
    private RecyclerView.LayoutManager mAMKLayoutManager;

    private DrawerLayout drawerLayout;
    private GestureDetector detector;

    static final int REQUEST_PATIENT = 1;

    public PrescriptionActivity() {
        super();
        products = new ArrayList<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prescription);
        setTitle(R.string.prescription_button);

        this.placeDateText = findViewById(R.id.placedate_text);
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
        this.sendButton = findViewById(R.id.send_button);
        this.interactionButton = findViewById(R.id.interaction_button);

        this.drawerLayout = findViewById(R.id.drawer_layout);
        final PrescriptionActivity _this = this;

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
                handleLongTapForProduct(product);
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
        mAMKAdapter.onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int itemPosition = medicineRecyclerView.getChildLayoutPosition(v);
                final File file = mAMKAdapter.mDataset.get(itemPosition);
                new AlertDialog.Builder(_this, R.style.CustomAlertDialog)
                        .setTitle(getString(R.string.confirm_delete_amk) + " " + file.getName())
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                file.delete();
                                reloadAMKFileList();
                                if (openedFile.equals(file)) {
                                    openNewPrescription();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
                return true;
            }
        };
        amkRecyclerView.setAdapter(mAMKAdapter);

        reloadAMKFileList();

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
                if (openedFile != null && openedPrescription != null) {
                    overwriteOldPrescription();
                } else {
                    saveNewPrescription();
                }
            }
        });
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewPrescription();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (openedFile != null && openedPrescription != null) {
                    overwriteOldPrescription();
                } else {
                    saveNewPrescription();
                }
                Intent emailIntent = createEmailIntent();
                if (emailIntent != null) {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
                }
            }
        });

        interactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity main = MainActivity.instance;
                if (main == null) return;
                ArrayList<Medication> ms = new ArrayList<>();
                for (Product p : products) {
                    ms.add(p.toMedicationForInteraction());
                }
                main.setMedicationsInInteractionBasket(ms);
                finish();
            }
        });

        this.setDoctor(Operator.loadFromStore(this.getFilesDir().toString()));
        this.setPatient(Patient.loadCurrentPatient(this));
        this.setProducts(PrescriptionProductBasket.getShared().products);
        this.reloadMedicinesText();

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String uid = intent.getStringExtra(PatientListActivity.PATIENT_DELETED_EVENT_UID);
                if (uid.equals(_this.patient.uid)) {
                    openNewPrescription();
                    reloadAMKFileList();
                }
            }
        }, new IntentFilter(PatientListActivity.PATIENT_DELETED_EVENT));

        Intent i = getIntent();
        if (i.getData() != null) {
            // wants to open an AMK file from resource uri
            openPrescriptionFromResourceUri(i.getData());
        }
        setupGestureDetector();
    }

    private void setupGestureDetector() {
        GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                if (event1==null || event2==null) {
                    return false;
                }
                if (event1.getPointerCount()>1 || event2.getPointerCount()>1) {
                    return false;
                } else {
                    try {
                        // right to left swipe... return to mSuggestView
                        // float diffX = event1.getX()-event2.getX();
                        // left to right swipe... return to mSuggestView
                        float diffX = event2.getX()-event1.getX();
                        if (diffX>120 && Math.abs(velocityX)>300) {
                            finish();
                            return true;
                        }
                    } catch (Exception e) {
                        // Handle exceptions...
                    }
                    return false;
                }
            }
        };

        detector = new GestureDetector(this, simpleOnGestureListener);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (detector != null && !drawerLayout.isDrawerVisible(Gravity.RIGHT)) {
            if (detector.onTouchEvent(ev)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public void handleLongTapForProduct(final Product p) {
        new AlertDialog.Builder(this)
            .setPositiveButton(getString(R.string.edit_comment), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDialogForEditingProductComment(p);
                }
            })
            .setNeutralButton(getString(R.string.delete_product), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDialogForDeletingProduct(p);
                }
            })
            .show();
    }

    public void showDialogForEditingProductComment(final Product product) {
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 0, 50, 0);

        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(product.comment);
        input.setLines(3);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMaxLines(5);
        input.setTextSize(13);
        input.setVerticalScrollBarEnabled(true);
        layout.addView(input);

        TextView title = new TextView(this);
        title.setText(getString(R.string.edit_comment));
        title.setPadding(50, 30, 0, 0);
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);

        AlertDialog ad = new AlertDialog.Builder(this, R.style.EditTextAlertDialog)
            .setCustomTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    product.comment = input.getText().toString();
                    reloadRecyclerView();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        ad.show();
    }

    public void showDialogForDeletingProduct(final Product product) {
        new AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.delete_product))
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    products.remove(product);
                    setProducts(products);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    public void setDoctor(Operator doctor) {
        this.doctor = doctor;
        this.reloadDoctorTexts();
        this.reloadButtons();
    }

    public void reloadDoctorTexts() {
        if (doctor == null) {
            this.doctorNameText.setText("");
            this.doctorStreetText.setText("");
            this.doctorZipCityText.setText("");
            this.doctorPhoneText.setText("");
            this.doctorEmailText.setText("");
            this.doctorImageView.setImageBitmap(null);
        } else {
            this.doctorNameText.setText(doctor.title + " " + doctor.familyName + " " + doctor.givenName);
            this.doctorStreetText.setText(doctor.postalAddress);
            this.doctorZipCityText.setText(doctor.zipCode + " " + doctor.city);
            this.doctorPhoneText.setText(doctor.phoneNumber);
            this.doctorEmailText.setText(doctor.emailAddress);
            this.doctorImageView.setImageBitmap(doctor.getSignatureImage());
        }
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        this.reloadPatientText();
        this.reloadButtons();
    }
    public void reloadPatientText() {
        if (this.patient == null) {
            this.patientNameText.setText("");
            this.patientWeightHeightGenderBirthdayText.setText("");
            this.patientStreetText.setText("");
            this.patientZipCityCountryText.setText("");
        } else {
            this.patientNameText.setText(patient.familyname + " " + patient.givenname);
            String genderString = patient.gender == Patient.KEY_AMK_PAT_GENDER_M ? "m"
                    : patient.gender == Patient.KEY_AMK_PAT_GENDER_F ? "f" : "";
            this.patientWeightHeightGenderBirthdayText.setText(Integer.toString(patient.weight_kg)+"kg/"+patient.height_cm+"cm " + genderString + " " + patient.birthdate);
            this.patientStreetText.setText(patient.address);
            this.patientZipCityCountryText.setText(patient.zipcode + " " + patient.city + " " + patient.country);
        }
    }

    public void openPrescriptionFromFile(File file) {
        Prescription p = PrescriptionUtility.readFromFile(file);
        if (p == null) return;
        openedPrescription = p;
        openedFile = file;
        setPatient(p.patient);
        setDoctor(p.doctor);
        setProducts(p.medications);
        reloadPlaceDateText();
    }

    public void openPrescriptionFromResourceUri(Uri uri) {
        Prescription prescription = PrescriptionUtility.readFromResourceUri(this, uri);
        if (prescription == null) return;
        Patient p = prescription.patient;
        // Calculate hash instead of using p.uid because it's possible that the hash
        // is calculated with another algorithm
        p.uid = p.hashValue();
        if (p != null) {
            // Import patient if needed
            PatientDBAdapter db = new PatientDBAdapter(this);

            Patient existingPatient = db.getPatientWithUniqueId(p.uid);
            if (existingPatient == null) {
                db.insertRecord(p);
            }
            db.close();

            boolean needToImport = true;
            File existingPrescriptionFile = null;
            ArrayList<File> amkFiles = PrescriptionUtility.amkFilesforPatient(this, p);
            for (File file : amkFiles) {
                Prescription thisPrescription = PrescriptionUtility.readFromFile(file);
                if (thisPrescription == null) continue;
                if (thisPrescription.hash.equals(prescription.hash)) {
                    needToImport = false;
                    existingPrescriptionFile = file;
                    break;
                }
            }
            if (needToImport) {
                File savedFile = PrescriptionUtility.savePrescription(this, prescription);
                openedFile = savedFile;
                openedPrescription = prescription;
                setPatient(prescription.patient);
                setDoctor(prescription.doctor);
                setProducts(prescription.medications);
                reloadPlaceDateText();
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.amk_imported))
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.amk_already_imported))
                        .setPositiveButton("OK", null)
                        .show();
                openPrescriptionFromFile(existingPrescriptionFile);
            }
            Patient.setCurrentPatientId(this, prescription.patient.uid);
            reloadAMKFileList();
        } else {
            // Cannot save prescription if there is no patient, but let user to view it
            openedFile = null;
            openedPrescription = null;
            setPatient(prescription.patient);
            setDoctor(prescription.doctor);
            setProducts(prescription.medications);
            reloadPlaceDateText();
        }
    }

    public void openNewPrescription() {
        openedFile = null;
        openedPrescription = null;
        setPatient(null);
        setDoctor(Operator.loadFromStore(this.getFilesDir().toString()));
        setProducts(new ArrayList<Product>());
        reloadPlaceDateText();
    }

    public void reloadPlaceDateText() {
        if (openedPrescription == null) {
            this.placeDateText.setText("");
        } else {
            this.placeDateText.setText(openedPrescription.placeDate);
        }
    }

    public void setProducts(ArrayList<Product> newProducts) {
        products = newProducts;
        PrescriptionProductBasket.getShared().products = newProducts;
        mRecyclerAdapter.mDataset = newProducts;
        mRecyclerAdapter.notifyDataSetChanged();
        this.reloadMedicinesText();
        this.reloadButtons();
    }

    public void reloadRecyclerView() {
        mRecyclerAdapter.notifyDataSetChanged();
    }

    public void reloadMedicinesText() {
        medicinesText.setText(getString(R.string.search)+"(" + products.size() + ")");
    }

    public void reloadAMKFileList() {
        ArrayList<File> amkFiles = PrescriptionUtility.amkFilesForCurrentPatient(this);
        amkFiles.sort(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return -(o1.getName().compareToIgnoreCase(o2.getName()));
            }
        });
        mAMKAdapter.mDataset = amkFiles;
        mAMKAdapter.notifyDataSetChanged();
    }

    public void reloadButtons() {
        if (doctor != null && patient != null && products.size() > 0) {
            saveButton.setEnabled(true);
            sendButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
            sendButton.setEnabled(false);
        }
        interactionButton.setEnabled(products.size() > 0);
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
                intent.putExtra("allowCreation", true);
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
            reloadAMKFileList();
        }
    }

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
        p.placeDate = doctor.city + ", " + PrescriptionUtility.prettyTime();
        return p;
    }

    void saveNewPrescription() {
        Prescription p = makePrescription(null);
        File savedFile = PrescriptionUtility.savePrescription(this, p);
        reloadAMKFileList();
        openedFile = savedFile;
        openedPrescription = p;
        reloadPlaceDateText();
    }
    void overwriteOldPrescription() {
        if (openedPrescription == null || openedFile == null) {
            saveNewPrescription();
            return;
        }
        openedFile.delete();
        this.setDoctor(Operator.loadFromStore(this.getFilesDir().toString()));
        Prescription p = makePrescription(openedPrescription.hash);
        File savedFile = PrescriptionUtility.savePrescription(this, p);
        reloadAMKFileList();
        openedFile = savedFile;
        openedPrescription = p;
        reloadPlaceDateText();
    }

    Intent createEmailIntent() {
        if (openedPrescription == null) {
            return null;
        }
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        Patient p = openedPrescription.patient;
        Operator d = openedPrescription.doctor;
        String emailSubject =
                getString(R.string.prescription_email_prescription) + " "
                + (p == null ? "" : p.givenname) + " "
                + (p == null ? "" : p.familyname) + ", "
                + (p == null ? "" : p.birthdate) + " "
                + getString(R.string.prescription_email_from) + " "
                + (d == null ? "" : d.title) + " "
                + (d == null ? "" : d.givenName) + " "
                + (d == null ? "" : d.familyName);

        String emailBody =
                getString(R.string.prescription_email_open_with) + "\n\niOS: "
                + "https://itunes.apple.com/ch/app/generika/id520038123?mt=8"
                + "\nAndroid: "
                + "https://play.google.com/store/apps/details?id=org.oddb.generika"
                + "\n";

        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,emailSubject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailBody);
        emailIntent.putExtra(Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".com.ywesee.amiko.provider", openedFile)
        );
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return emailIntent;
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
        layout.setOnLongClickListener(onLongClickListener);

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
    public View.OnLongClickListener onLongClickListener;

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
        filenameTextView.setOnLongClickListener(onLongClickListener);
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
