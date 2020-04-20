package com.ywesee.amiko;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoogleOAuthActivity extends AppCompatActivity {
    private GoogleAuthorizationCodeFlow flow;
    private TextView descriptionTextView;

    static private String redirectUri = "com.ywesee.amiko:/oauth";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_auth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Login Google");

        descriptionTextView = findViewById(R.id.description_textview);

        flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                Constants.googleClientId,
                Constants.googleClientSecret,
                Arrays.asList("https://www.googleapis.com/auth/drive.appdata")
        )
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        Intent intent = getIntent();
        handleIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void getAccessTokenWithCode(String code) {
        GoogleOAuthActivity _this = this;
        descriptionTextView.setText(R.string.loading);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GoogleTokenResponse response = flow.newTokenRequest(code)
                            .setRedirectUri(redirectUri)
                            .execute();
                    // UserId is always 0 because we don't have a Ywesee-id
                    Credential cred = flow.createAndStoreCredential(response, "0");

                    Drive driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();

                    File fileMetadata = new File();
                    String doctorFilename = "doctor.txt";
                    fileMetadata.setName(doctorFilename);
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    java.io.File filePath = new java.io.File(_this.getFilesDir().toString(), doctorFilename);
                    FileContent mediaContent = new FileContent("application/json", filePath);
                    File file = driveService.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                    System.out.println("File ID: " + file.getId());
                    
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

    private void handleIntent(Intent intent) {
        boolean didReceivedCode = false;
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                String code = uri.getQueryParameter("code");
                if (code != null) {
                    didReceivedCode = true;
                    getAccessTokenWithCode(code);
                }
            }
        }
        if (!didReceivedCode) {
            descriptionTextView.setText(R.string.redirecting_to_google);
            String url = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }
    }
}
