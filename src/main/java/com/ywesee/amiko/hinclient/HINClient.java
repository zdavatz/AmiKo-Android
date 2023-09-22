package com.ywesee.amiko.hinclient;

import android.graphics.Bitmap;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.BitmapRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.ywesee.amiko.BuildConfig;
import com.ywesee.amiko.Constants;
import com.ywesee.amiko.DoctorStore;
import com.ywesee.amiko.Prescription;

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

    public String adswissAuthOauthCallback() {
        return oauthCallbackScheme() + "://adswissoauth";
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

    public String hinSDSAppName() {
        return "hin_sds";
    }

    public String hinADSwissAppName() {
        if (BuildConfig.DEBUG) {
            return "ADSwiss_CI-Test";
        } else {
            return "ADSwiss_CI";
        }
    }

    public String authURLForSDS() {
        return authURLWithApplication(hinSDSAppName());
    }

    public String authURLForADSwiss() {
        return authURLWithApplication(hinADSwissAppName());
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

    public void fetchAccessTokenWithAuthCode(String authCode, HINToken.Application app, HINClientResponseCallback<HINToken> callback) {
        AndroidNetworking.post("https://oauth2.hin.ch/REST/v1/OAuth/GetAccessToken")
                .addHeaders("Accept", "application/json")
                .addHeaders("Content-Type", "application/x-www-form-urlencoded")
                .addBodyParameter("grant_type", "authorization_code")
                .addBodyParameter("redirect_uri", oauthCallback())
                .addBodyParameter("code", authCode)
                .addBodyParameter("client_id", HINClientCredentials.HIN_CLIENT_ID)
                .addBodyParameter("client_secret", HINClientCredentials.HIN_CLIENT_SECRET)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            HINToken token = new HINToken(response, app);
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
        if (!token.isExpired()) {
            callback.onResponse(token);
            return;
        }
        AndroidNetworking.post("https://oauth2.hin.ch/REST/v1/OAuth/GetAccessToken")
                .addHeaders("Accept", "application/json")
                .addHeaders("Content-Type", "application/x-www-form-urlencoded")
                .addBodyParameter("grant_type", "refresh_token")
                .addBodyParameter("redirect_uri", this.oauthCallback())
                .addBodyParameter("refresh_token", token.refreshToken)
                .addBodyParameter("client_id", HINClientCredentials.HIN_CLIENT_ID)
                .addBodyParameter("client_secret", HINClientCredentials.HIN_CLIENT_SECRET)
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
            public void onResponse(HINToken token) {
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

    public void fetchADSwissSAML(HINToken token, HINClientResponseCallback<ADSwissSAML> callback) {
        this.renewTokenIfNeeded(token, new HINClientResponseCallback<HINToken>() {
            @Override
            public void onError(Exception err) {
                callback.onError(err);
            }
            @Override
            public void onResponse(HINToken token) {
                String url = String.format("https://%s/authService/EPDAuth?targetUrl=%s&style=redirect", HINDomainForADSwiss(), adswissAuthOauthCallback());
                AndroidNetworking.post(url)
                        .addHeaders("Accept", "application/json")
                        .addHeaders("Authorization", "Bearer " + token.accessToken)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    ADSwissSAML saml = new ADSwissSAML(response, token);
                                    callback.onResponse(saml);
                                } catch (JSONException e) {
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

    public void fetchADSwissAuthHandle(HINToken token, String authCode, HINClientResponseCallback<ADSwissAuthHandle> callback) {
        this.renewTokenIfNeeded(token, new HINClientResponseCallback<HINToken>() {
            @Override
            public void onError(Exception err) {
                callback.onError(err);
            }
            @Override
            public void onResponse(HINToken token) {
                String url = String.format("https://%s/authService/EPDAuth/auth_handle", HINDomainForADSwiss());
                JSONObject body = new JSONObject();
                try {
                    body.put("authCode", authCode);
                } catch (JSONException e) {
                    callback.onError(e);
                }
                AndroidNetworking.post(url)
                        .addHeaders("Accept", "application/json")
                        .addHeaders("Content-Type", "application/json")
                        .addHeaders("Authorization", "Bearer " + token.accessToken)
                        .addJSONObjectBody(body)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String authHandleStr = response.getString("authHandle");
                                    ADSwissAuthHandle handle = new ADSwissAuthHandle(authHandleStr);
                                    callback.onResponse(handle);
                                } catch (JSONException e) {
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

    public void makeQRCode(ADSwissAuthHandle authHandle, Prescription prescription, HINClientResponseCallback<Bitmap> callback) {
        String url = "https://" + this.certifactionDomain() + "/ePrescription/create?output-format=qrcode";
        authHandle.updateLastUsedAt();
        // TODO save new auth handle
        try {
            AndroidNetworking.post(url)
                    .addHeaders("Content-Type", "text/plain")
                    .addHeaders("Authorization", "Bearer " + authHandle.token)
                    .addStringBody(prescription.bodyForEPrescription())
                    .build()
                    .getAsBitmap(new BitmapRequestListener() {
                        @Override
                        public void onResponse(Bitmap response) {
                            callback.onResponse(response);
                        }

                        @Override
                        public void onError(ANError anError) {
                            callback.onError(anError);
                        }
                    });
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
