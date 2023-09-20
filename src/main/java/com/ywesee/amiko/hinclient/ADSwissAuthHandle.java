package com.ywesee.amiko.hinclient;

import com.ywesee.amiko.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class ADSwissAuthHandle {
    public String token;
    public Date lastUsedAt;
    public Date expiresAt;

    public ADSwissAuthHandle(String token) {
        this.token = token;
        this.lastUsedAt = new Date();
        this.expiresAt = new Date(new Date().getTime() + (12 * 60 * 60) * 1000); // 12 hours
    }

    public ADSwissAuthHandle(JSONObject json) throws JSONException {
        this.token = json.getString("token");
        this.lastUsedAt = Utilities.dateFromTimeString(json.getString("last_used_at"));
        this.expiresAt = Utilities.dateFromTimeString(json.getString("expires_at"));
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("token", this.token);
        json.put("last_used_at", Utilities.timeStringFromDate(this.lastUsedAt));
        json.put("expires_at", Utilities.timeStringFromDate(this.expiresAt));
        return json;
    }

    public boolean isExpired() {
        return this.expiresAt.before(new Date()) || new Date(this.lastUsedAt.getTime() + 60 * 60 * 2 * 1000 /*2 hours*/).before(new Date());
    }


    public void updateLastUsedAt() {
        this.lastUsedAt = new Date();
    }
}
