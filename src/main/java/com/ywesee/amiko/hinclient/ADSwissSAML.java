package com.ywesee.amiko.hinclient;

import org.json.JSONException;
import org.json.JSONObject;

public class ADSwissSAML {
    public HINToken token;
    public String url;
    public String epdAuthUrl;

    public ADSwissSAML(JSONObject json, HINToken token) throws JSONException {
        this.token = token;
        this.url = json.getString("url");
        this.epdAuthUrl = json.getString("epdAuthUrl");
    }
}
