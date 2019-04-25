package com.ywesee.amiko;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductPrintingActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    private PrinterListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    NsdManager nsdManager;
    NsdManager.DiscoveryListener listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_printing);

        setTitle(R.string.choose_a_printer);

        recyclerView = findViewById(R.id.recycler_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));


        final Context _this = this;

        // specify an adapter (see also next example)
        adapter = new PrinterListAdapter(new ArrayList<NsdServiceInfo>());
        final Context context = this;
        adapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = recyclerView.getChildLayoutPosition(v);
                NsdServiceInfo serviceInfo = adapter.mDataset.get(itemPosition);
                printWithService(serviceInfo);
            }
        };
        recyclerView.setAdapter(adapter);
        nsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);
        findPrinters();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nsdManager.stopServiceDiscovery(listener);
    }

    void findPrinters() {
        listener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {

            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {

            }

            @Override
            public void onDiscoveryStarted(String serviceType) {

            }

            @Override
            public void onDiscoveryStopped(String serviceType) {

            }

            @Override
            public void onServiceFound(final NsdServiceInfo serviceInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.mDataset.add(serviceInfo);
                        adapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onServiceLost(final NsdServiceInfo serviceInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.mDataset.remove(serviceInfo);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        };
        nsdManager.discoverServices("_ipp._tcp", NsdManager.PROTOCOL_DNS_SD, listener);
    }

    void printWithService(NsdServiceInfo serviceInfo) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle(R.string.printing);
        progress.setMessage(getString(R.string.connecting));
        progress.setCancelable(false);
        progress.show();

        final Context context = this;
        nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                progress.dismiss();
                new AlertDialog.Builder(context)
                        .setMessage(getString(R.string.cannot_connect_error) + " " + errorCode)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                try {
                    CupsClient cupsClient = new CupsClient(serviceInfo.getHost().getHostAddress(), serviceInfo.getPort());
                    final List<CupsPrinter> printers = cupsClient.getPrinters();
                    if (printers.size() == 0) {
                        progress.dismiss();
                        new AlertDialog.Builder(context)
                                .setMessage(getString(R.string.no_printer_found))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return;
                    }
                    Intent i = getIntent();
                    final Operator doctor = Operator.loadFromStore(getFilesDir().toString());
                    final Patient patient = (Patient)i.getSerializableExtra("patient");
                    final Product product = (Product)i.getSerializableExtra("product");

                    if (printers.size() == 1) {
                        progress.setMessage(getString(R.string.printing));
                        ProductPrintingUtility.printToService(context, printers.get(0), doctor, patient, product);
                        progress.dismiss();
                        finish();
                        return;
                    }
                    ArrayList<String> names = new ArrayList<String>();
                    for (CupsPrinter p : printers) {
                        names.add(p.getName());
                    }
                    final AtomicInteger choice = new AtomicInteger(0);
                    new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                            .setSingleChoiceItems((String[])names.toArray(), -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    choice.set(which);
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int index = choice.get();
                                    progress.setMessage(getString(R.string.printing));
                                    try {
                                        ProductPrintingUtility.printToService(context, printers.get(index), doctor, patient, product);
                                        progress.dismiss();
                                        finish();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (progress.isShowing()) {
                                            progress.dismiss();
                                        }
                                        new AlertDialog.Builder(context)
                                                .setMessage(getString(R.string.cannot_connect_error) + " " + e.getLocalizedMessage())
                                                .setPositiveButton(android.R.string.ok, null)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    progress.dismiss();
                                }
                            })
                            .create()
                            .show();

                } catch (Exception e) {
                    e.printStackTrace();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    new AlertDialog.Builder(context)
                            .setMessage(getString(R.string.cannot_connect_error) + " " + e.getLocalizedMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        });
    }
}

class PrinterListAdapter extends RecyclerView.Adapter<PrinterListAdapter.ViewHolder> {
    public List<NsdServiceInfo> mDataset;
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
    public PrinterListAdapter(List<NsdServiceInfo> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PrinterListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
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
        NsdServiceInfo info = mDataset.get(position);
        holder.mTextView.setText(info.getServiceName());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
