package com.ywesee.amiko;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.Batch;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GoogleSyncActivity extends AppCompatActivity {
    static private String TAG = "GoogleSyncActivity";
    private TextView descriptionTextView;
    private Button loginButton;
    private TextView syncTextView;
    private Button syncButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sync);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Login Google");

        GoogleSyncActivity _this = this;
        descriptionTextView = findViewById(R.id.description_textview);
        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SyncManager.getShared().isGoogleLoggedIn()) {
                    logout();
                } else {
                    login();
                }
            }
        });
        syncTextView = findViewById(R.id.sync_description);
        syncButton = findViewById(R.id.sync_button);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SyncManager.getShared().triggerSync();
            }
        });
        updateUI();

        Intent intent = getIntent();
        handleIntent(intent);

        IntentFilter statusIntentFilter = new IntentFilter(
                SyncService.BROADCAST_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        receivedSyncStatus(intent.getStringExtra("status"));
                    }
                },
                statusIntentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void updateUI() {
        if (SyncManager.getShared().isGoogleLoggedIn()) {
            descriptionTextView.setText(R.string.logged_in);
            loginButton.setText(R.string.logout);
            syncButton.setEnabled(true);
        } else {
            descriptionTextView.setText(R.string.not_logged_in);
            loginButton.setText(R.string.login);
            syncButton.setEnabled(false);
        }
        syncTextView.setText("Last synced: " + SyncService.lastSynced(this));
    }

    private void getAccessTokenWithCode(String code) {
        GoogleSyncActivity _this = this;
        descriptionTextView.setText(R.string.loading);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncManager.getShared().receivedAuthCodeFromGoogle(code);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            descriptionTextView.setText("");
                            updateUI();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(_this)
                                    .setTitle("Error")
                                    .setMessage(e.getLocalizedMessage())
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                    });

                }
            }
        });
    }

    private void login() {
        descriptionTextView.setText(R.string.redirecting_to_google);
        String url = SyncManager.getShared().getUrlToLoginToGoogle();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
        updateUI();
    }

    private void logout() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.desitin_new);
        alert.setMessage(R.string.delete_on_google_drive);

        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                clearGoogleDriveStorageThenLogout();
            }
        });
        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                SyncManager.getShared().logoutGoogle();
                updateUI();
            }
        });
        alert.show();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = intent.getData();
        if (uri == null) return;
        String code = uri.getQueryParameter("code");
        if (code == null) return;
        getAccessTokenWithCode(code);
        updateUI();
    }

    protected void receivedSyncStatus(String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                syncTextView.setText(status);
            }
        });
    }

    protected void clearGoogleDriveStorageThenLogout() {
        ProgressDialog progressBar = new ProgressDialog(this);
        progressBar.setMessage(getString(R.string.loading));
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setIndeterminate(true);
        progressBar.setCancelable(false);
        progressBar.show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                performClearGoogleDriveStorage();
                SyncManager.getShared().logoutGoogle();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.dismiss();
                        updateUI();
                    }
                });
            }
        });
    }

    protected void performClearGoogleDriveStorage() {
        Credential cred = SyncManager.getShared().getGoogleCredential();
        if (cred == null) return;
        Drive driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();
        List<File> files = this.listRemoteFilesAndFolders(driveService);
        BatchRequest batch = driveService.batch();
        try {
            for (File file : files) {
                JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
                    @Override
                    public void onFailure(GoogleJsonError e,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.i(TAG, "Deleted file " + file.getId());
                    }
                };
                driveService.files().delete(file.getId()).queue(batch, callback);
            }
            if (batch.size() > 0) {
                batch.execute();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    protected List<File> listRemoteFilesAndFolders(Drive driveService) {
        ArrayList<File> files = new ArrayList<>();
        try {
            String pageToken = null;
            do {
                FileList fl = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("nextPageToken, files(id)")
                        .execute();
                files.addAll(fl.getFiles());
                pageToken = fl.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return files;
    }
}
