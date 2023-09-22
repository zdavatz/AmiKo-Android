package com.ywesee.amiko.hinclient;

import com.ywesee.amiko.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class HINToken {
    public enum Application {
        SDS,
        ADSwiss,
    }

    public Application application;
    public String accessToken;
    public String refreshToken;
    public Date expiresAt;
    public String hinId;

    // From saved tokens
    public HINToken(JSONObject json) throws JSONException {
        this.importFromJSON(json);
        String appString = json.getString("application");
        if (appString.equals("sds")) {
            this.application = Application.SDS;
        } else if (appString.equals("adswiss")) {
            this.application = Application.ADSwiss;
        }
        String expiresAtStr = json.getString("expires_at");
        this.expiresAt = Utilities.dateFromTimeString(expiresAtStr);
    }

    // From response json
    public HINToken(JSONObject json, Application app) throws JSONException {
        this.importFromJSON(json);
        this.application = app;
        long expiresIn = json.getLong("expires_in");
        this.expiresAt = new Date(new Date().getTime() + expiresIn * 1000);
    }

    private void importFromJSON(JSONObject json) throws JSONException {
        this.accessToken = json.getString("access_token");
        this.refreshToken = json.getString("refresh_token");
        this.hinId = json.getString("hin_id");
    }

    public boolean isExpired() {
        return this.expiresAt.before(new Date());
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("access_token", this.accessToken);
        json.put("refresh_token", this.refreshToken);
        json.put("expires_at", Utilities.timeStringFromDate(this.expiresAt));
        json.put("hin_id", this.hinId);
        switch (this.application) {
            case SDS:
                json.put("application", "sds");
                break;
            case ADSwiss:
                json.put("application", "adswiss");
                break;
        }
        return json;
    }
}
