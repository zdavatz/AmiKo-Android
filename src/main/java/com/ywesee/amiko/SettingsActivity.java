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
import com.ywesee.amiko.hinclient.HINClient;
import com.ywesee.amiko.hinclient.HINClientResponseCallback;
import com.ywesee.amiko.hinclient.HINSDSProfile;
import com.ywesee.amiko.hinclient.HINSettingsStore;
import com.ywesee.amiko.hinclient.HINToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends AppCompatActivity {
    static private String TAG = "GoogleSyncActivity";
    private TextView descriptionTextView;
    private Button loginButton;
    private TextView syncTextView;
    private Button syncButton;
    private TextView loginWithSDSTextView;
    private Button loginWithSDSButton;
    private TextView loginWithADSwissTextView;
    private Button loginWithADSwissButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SyncManager.setupShared(this);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.menu_settings));

        SettingsActivity _this = this;
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
        HINSettingsStore store = new HINSettingsStore(this);
        loginWithSDSTextView = findViewById(R.id.hin_sds_textview);
        loginWithSDSButton = findViewById(R.id.login_with_sds_button);
        loginWithSDSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HINToken token = store.getSDSToken();
                if (token == null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HINClient.Instance.authURLForSDS())));
                } else {
                    store.deleteSDSToken();
                    updateUI();
                }
            }
        });
        loginWithADSwissTextView = findViewById(R.id.adswiss_textview);
        loginWithADSwissButton = findViewById(R.id.login_with_adswiss_button);
        loginWithADSwissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HINToken token = store.getADSwissToken();
                if (token == null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HINClient.Instance.authURLForADSwiss())));
                } else {
                    store.deleteADSwissToken();
                    updateUI();
                }
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
            descriptionTextView.setText("Google: " + getString(R.string.logged_in));
            loginButton.setText(R.string.logout);
            syncButton.setEnabled(true);
        } else {
            descriptionTextView.setText("Google: " + getString(R.string.not_logged_in));
            loginButton.setText(R.string.login);
            syncButton.setEnabled(false);
        }
        syncTextView.setText("Last synced: " + SyncService.lastSynced(this));
        HINSettingsStore store = new HINSettingsStore(this);
        HINToken sdsToken = store.getSDSToken();
        HINToken adswissToken = store.getADSwissToken();
        if (sdsToken == null) {
            loginWithSDSTextView.setText("HIN (SDS): " + getString(R.string.not_logged_in));
            loginWithSDSButton.setText(getString(R.string.login_with_hin_sds));
        } else {
            loginWithSDSTextView.setText("HIN (SDS): " + sdsToken.hinId);
            loginWithSDSButton.setText(getString(R.string.logout_from_hin_sds));
        }
        if (adswissToken == null) {
            loginWithADSwissTextView.setText("HIN (ADSwiss): " + getString(R.string.not_logged_in));
            loginWithADSwissButton.setText(getString(R.string.login_with_adswiss));
        } else {
            loginWithADSwissTextView.setText("HIN (ADSwiss): " + adswissToken.hinId);
            loginWithADSwissButton.setText(getString(R.string.logout_from_adswiss));
        }
    }

    private void getAccessTokenWithCode(String code) {
        SettingsActivity _this = this;
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
                            SyncManager.getShared().triggerSync();
                        }
                    });
                } catch (Exception e) {
                    showError(e);
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
        if (uri.getScheme().equals("amiko") || uri.getScheme().equals("comed")) {
            // HIN OAuth
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            HINSettingsStore store = new HINSettingsStore(this);
            HINToken.Application app = state.equals(HINClient.Instance.hinSDSAppName()) ? HINToken.Application.SDS : HINToken.Application.ADSwiss;
            if (code != null) {
                HINClient.Instance.fetchAccessTokenWithAuthCode(code, app, new HINClientResponseCallback<HINToken>() {
                    @Override
                    public void onResponse(HINToken res) {
                        if (app == HINToken.Application.SDS) {
                            store.saveSDSToken(res);
                            updateDoctorViaSDS(res);
                        } else if (app == HINToken.Application.ADSwiss) {
                            store.saveADSwissToken(res);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception err) {
                        showError(err);
                    }
                });
            }
        } else if (uri.getScheme().equals("com.ywesee.amiko.de") || uri.getScheme().equals("com.ywesee.amiko.fr")) {
            // Google OAuth
            String code = uri.getQueryParameter("code");
            if (code == null) return;
            getAccessTokenWithCode(code);
        }
        updateUI();
    }

    private void updateDoctorViaSDS(HINToken sdsToken) {
        DoctorStore store = new DoctorStore(this);
        store.load();
        HINClient.Instance.fetchSDSSelf(sdsToken, new HINClientResponseCallback<HINSDSProfile>() {
            @Override
            public void onResponse(HINSDSProfile res) {
                res.mergeToOperator(store);
                store.save();
            }

            @Override
            public void onError(Exception err) {
                showError(err);
            }
        });
    }

    private void showError(Exception e) {
        Context _this = this;
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
