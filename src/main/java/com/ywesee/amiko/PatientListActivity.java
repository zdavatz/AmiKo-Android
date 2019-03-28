package com.ywesee.amiko;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class PatientListActivity extends AppCompatActivity {
    public final static String PATIENT_DELETED_EVENT = "patient-deleted";
    public final static String PATIENT_DELETED_EVENT_UID = "patient-deleted-uid";
    public final static int CREATE_PATIENT = 0;

    private RecyclerView mRecyclerView;
    private EditText mSearchField;
    private PatientListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private PatientDBAdapter mDBAdapter;
    private List<Patient> mAllPatients;
    private GestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
        setTitle(R.string.patient_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mDBAdapter = new PatientDBAdapter(this);
        mAllPatients = mDBAdapter.getAllRecords();
        final Context _this = this;

        // specify an adapter (see also next example)
        mAdapter = new PatientListAdapter(mAllPatients);
        final Context context = this;
        mAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = mRecyclerView.getChildLayoutPosition(v);
                Patient patient = mAdapter.mDataset.get(itemPosition);
                Patient.setCurrentPatientId(context, patient.uid);
                Intent data = new Intent();
                data.putExtra("patient",patient);
                setResult(0, data);
                finish();
            }
        };
        mAdapter.onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int itemPosition = mRecyclerView.getChildLayoutPosition(v);
                final Patient patient = mAllPatients.get(itemPosition);
                new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.confirm_delete_patient))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDBAdapter.deleteRecord(patient);
                                mAllPatients = ((PatientListAdapter) mAdapter).mDataset = mDBAdapter.getAllRecords();
                                mAdapter.notifyDataSetChanged();
                                Patient.setCurrentPatientId(context, null);
                                PrescriptionUtility.deletePatientDirectory(_this, patient);
                                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
                                Intent intent = new Intent(PATIENT_DELETED_EVENT);
                                intent.putExtra(PATIENT_DELETED_EVENT_UID, patient.uid);
                                lbm.sendBroadcast(intent);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
                return true;
            }
        };
        mRecyclerView.setAdapter(mAdapter);

        mSearchField = (EditText)findViewById(R.id.search_field);
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyPatientsAndUpdateSearchResult();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
        if (detector != null) {
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

    public void applyPatientsAndUpdateSearchResult() {
        String searchQuery = mSearchField.getText().toString();
        if (searchQuery.equals("")) {
            mAdapter.mDataset = mAllPatients;
        } else {
            ArrayList<Patient> contacts = new ArrayList<>();
            for (Patient p : mAllPatients) {
                if (p.stringForDisplay().toLowerCase().contains(searchQuery.toLowerCase())) {
                    contacts.add(p);
                }
            }
            mAdapter.mDataset = contacts;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (getAllowCreation()) {
            getMenuInflater().inflate(R.menu.prescription_list_actionbar, menu);
            MenuItem addItem = menu.findItem(R.id.add);
            Drawable newIcon = (Drawable)addItem .getIcon();
            newIcon.mutate().setColorFilter(Color.argb(255, 0, 0, 0), PorterDuff.Mode.SRC_IN);
            addItem.setIcon(newIcon);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.add: {
                Intent intent = new Intent(this, PatientActivity.class);
                intent.putExtra("createOnly", true);
                startActivityForResult(intent, CREATE_PATIENT);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CREATE_PATIENT && resultCode == 0 && data != null) {
            Patient patient = (Patient)data.getSerializableExtra("patient");
            Intent newData = new Intent();
            data.putExtra("patient", patient);
            setResult(0, data);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean getAllowCreation() {
        return getIntent().getBooleanExtra("allowCreation", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDBAdapter.close();
    }
}

class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {
    public List<Patient> mDataset;
    public View.OnClickListener onClickListener;
    public View.OnLongClickListener onLongClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PatientListAdapter(List<Patient> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PatientListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // create a new view
        TextView v = new TextView(parent.getContext());
        v.setTextSize(25);
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
        Patient p = mDataset.get(position);
        holder.mTextView.setText(p.stringForDisplay());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
