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


public class PersistenceManager extends Object {
    static PersistenceManager shared = null;
    private Context context = null;
    static final String TAG = "PersistenceManager";

    private GoogleAuthorizationCodeFlow googleAuthFlow;
    static private String googleRedirectUri = "com.ywesee.amiko:/oauth";

    static void setupShared(Context context) {
        if (shared == null) {
            shared = new PersistenceManager(context);
        }
    }

    static PersistenceManager getShared() {
        return shared;
    }

    private PersistenceManager(Context context) {
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
                Constants.googleClientId,
                Constants.googleClientSecret,
                Arrays.asList("https://www.googleapis.com/auth/drive.appdata")
        )
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .setCredentialDataStore(credDataStore)
                .build();
    }

    public void loginToGoogle(Context context) {
        Intent intent = new Intent(context, GoogleOAuthActivity.class);
        context.startActivity(intent);
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
}
