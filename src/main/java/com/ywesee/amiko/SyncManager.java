package com.ywesee.amiko;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


public class SyncManager extends Object {
    static SyncManager shared = null;
    private Context context = null;
    private Timer timer;
    static final String TAG = "PersistenceManager";

    private GoogleAuthorizationCodeFlow googleAuthFlow;
    static private String googleRedirectUri = "com.ywesee.amiko:/oauth";

    static void setupShared(Context context) {
        if (shared == null) {
            shared = new SyncManager(context);
        }
    }

    static SyncManager getShared() {
        return shared;
    }

    private SyncManager(Context context) {
        this.context = context;
        DataStore<StoredCredential> credDataStore = null;
        try {
            credDataStore = StoredCredential.getDefaultDataStore(new FileDataStoreFactory(new File(this.context.getFilesDir(), "googleSync/googleCreds")));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        googleAuthFlow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                Constants.googleClientId(),
                "", // No secret for android app
                Arrays.asList("https://www.googleapis.com/auth/drive.appdata")
        )
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .setCredentialDataStore(credDataStore)
                .build();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                triggerSync();
            }
        }, 0, 3 * 60 * 1000); // 3 min
    }

    public void triggerSync() {
        if (isGoogleLoggedIn()) {
            Intent intent = new Intent();
            SyncService.enqueueWork(context, SyncService.class, 0, intent);
        }
    }

    public String getUrlToLoginToGoogle() {
        return googleAuthFlow.newAuthorizationUrl()
                .setRedirectUri(googleRedirectUri)
                .build();
    }

    public Credential receivedAuthCodeFromGoogle(String code) {
        try {
            GoogleTokenResponse response = googleAuthFlow.newTokenRequest(code)
                    .setRedirectUri(googleRedirectUri)
                    .execute();
            // UserId is always 0 because we don't have a Ywesee-id
            return googleAuthFlow.createAndStoreCredential(response, "0");
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    public Credential getGoogleCredential() {
        try {
            Credential c = googleAuthFlow.loadCredential("0");
            return c;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    public boolean isGoogleLoggedIn() {
        return this.getGoogleCredential() != null;
    }

    public void logoutGoogle() {
        try {
            googleAuthFlow.getCredentialDataStore().delete("0");
        } catch (Exception e){}
    }
}
