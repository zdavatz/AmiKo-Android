package com.ywesee.amiko.hinclient;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.ywesee.amiko.BuildConfig;
import com.ywesee.amiko.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class HINClient {
    public static HINClient Instance = new HINClient();
    private static final String TAG = "HINClient"; // Tag for LogCat window

    private HINClient(){

    }

    public String oauthCallbackScheme() {
        if (Constants.appLanguage().equals("de")) {
            return "amiko";
        } else {
            return "comed";
        }
    }

    public String oauthCallback() {
        return oauthCallbackScheme() + "://oauth";
    }

    public String authURLWithApplication(String hinApplicationName) {
        try {
            return String.format(
                    "https://apps.hin.ch/REST/v1/OAuth/GetAuthCode/%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                    hinApplicationName,
                    HINClientCredentials.HIN_CLIENT_ID,
                    URLEncoder.encode(oauthCallback(), StandardCharsets.UTF_8.toString()),
                    hinApplicationName
            );
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String authURLForSDS() {
        return authURLWithApplication("hin_sds");
    }

    public String authURLForADSwiss() {
        if (BuildConfig.DEBUG) {
            return authURLWithApplication("ADSwiss_CI-Test");
        } else {
            return authURLWithApplication("ADSwiss_CI");
        }
    }

    public String HINDomainForADSwiss() {
        if (BuildConfig.DEBUG) {
            return "oauth2.ci-prep.adswiss.hin.ch";
        } else {
            return "oauth2.ci.adswiss.hin.ch";
        }
    }

    public String certifactionDomain() {
        if (BuildConfig.DEBUG) {
            return HINClientCredentials.CERTIFACTION_TEST_SERVER;
        } else {
            return HINClientCredentials.CERTIFACTION_SERVER;
        }
    }

    public void fetchAccessTokenWithAuthCode(String authCode, HINClientResponseCallback<HINToken> callback) {
        AndroidNetworking.post("https://oauth2.hin.ch/REST/v1/OAuth/GetAccessToken")
                .addHeaders("Accept", "application/json")
                .addHeaders("Content-Type", "application/x-www-form-urlencoded")
                .addBodyParameter("grant_type", "authorization_code")
                .addBodyParameter("redirect_uri", oauthCallback())
                .addBodyParameter("code", authCode)
                .addBodyParameter("client_id", HINClientCredentials.HIN_CLIENT_ID)
                .addBodyParameter("client_secret", HINClientCredentials.HIB_CLIENT_SECRET)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            HINToken token = new HINToken(response);
                            callback.onResponse(token);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getLocalizedMessage());
                            callback.onError(e);
                        }
                    }
                    @Override
                    public void onError(ANError error) {
                        callback.onError(error);
                    }
                });
    }

    public void renewTokenIfNeeded(HINToken token, HINClientResponseCallback<HINToken> callback) {
        AndroidNetworking.post("https://oauth2.hin.ch/REST/v1/OAuth/GetAccessToken")
                .addHeaders("Accept", "application/json")
                .addHeaders("Content-Type", "application/x-www-form-urlencoded")
                .addBodyParameter("grant_type", "refresh_token")
                .addBodyParameter("redirect_uri", this.oauthCallback())
                .addBodyParameter("refresh_token", token.refreshToken)
                .addBodyParameter("client_id", HINClientCredentials.HIN_CLIENT_ID)
                .addBodyParameter("client_secret", HINClientCredentials.HIB_CLIENT_SECRET)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            HINToken newToken = new HINToken(response);
                            // TODO: save newToken
                            callback.onResponse(newToken);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getLocalizedMessage());
                            callback.onError(e);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        callback.onError(anError);
                    }
                });
    }

    public void fetchSDSSelf(HINToken token, HINClientResponseCallback<HINSDSProfile> callback) {
        this.renewTokenIfNeeded(token, new HINClientResponseCallback<HINToken>() {
            @Override
            public void onError(Exception err) {
                callback.onError(err);
            }

            @Override
            public void onResponse(HINToken newToken) {
                AndroidNetworking.get("https://oauth2.sds.hin.ch/api/public/v1/self/")
                        .addHeaders("Accept", "application/json")
                        .addHeaders("Authorization", "Bearer " + token.accessToken)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    HINSDSProfile profile = new HINSDSProfile(response);
                                    callback.onResponse(profile);
                                } catch (JSONException | ParseException e) {
                                    Log.e(TAG, e.getLocalizedMessage());
                                    callback.onError(e);
                                }
                            }

                            @Override
                            public void onError(ANError anError) {
                                callback.onError(anError);
                            }
                        });
            }
        });
    }
}
