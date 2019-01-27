package com.ywesee.amiko;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class PatientListActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private EditText mSearchField;
    private PatientListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private PatientDBAdapter mDBAdapter;
    private List<Patient> mAllPatients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
        setTitle(R.string.patient_list);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mDBAdapter = new PatientDBAdapter(this);
        mAllPatients = mDBAdapter.getAllRecords();

        // specify an adapter (see also next example)
        mAdapter = new PatientListAdapter(mAllPatients);
        mAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = mRecyclerView.getChildLayoutPosition(v);
                Patient patient = mAdapter.mDataset.get(itemPosition);
                Intent data = new Intent();
                data.putExtra("patient",patient);
                setResult(0, data);
                finish();
            }
        };
        final Context context = this;
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
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
                return false;
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
