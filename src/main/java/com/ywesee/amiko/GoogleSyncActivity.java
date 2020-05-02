package com.ywesee.amiko;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class GoogleSyncActivity extends AppCompatActivity {
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
                SyncService.BROADCAST_ACTION);
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
        SyncManager.getShared().logoutGoogle();
        updateUI();
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
}
