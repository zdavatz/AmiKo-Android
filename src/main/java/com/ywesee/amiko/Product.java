package com.ywesee.amiko;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

public class Product {
    public static final String KEY_AMK_MED_EAN = "eancode";
    public static final String KEY_AMK_MED_PACKAGE = "package";
    public static final String KEY_AMK_MED_PROD_NAME = "product_name";
    public static final String KEY_AMK_MED_COMMENT = "comment";

    public static final String KEY_AMK_MED_TITLE = "title";
    public static final String KEY_AMK_MED_OWNER = "owner";
    public static final String KEY_AMK_MED_REGNRS = "regnrs";
    public static final String KEY_AMK_MED_ATC = "atccode";

    public static final int INDEX_EAN_CODE_IN_PACK = 9;

    public String eanCode;                 // eancode
    public String packageInfo;             // package
    public String prodName;                // product_name
    public String comment;                 // comment
    public String title;                   // title
    public String auth;                    // owner
    public String regnrs;                  // regnrs
    public String atccode;                 // atccode

    public Product(Medication medi, int packageIndex) {
        String[] listOfPacks = medi.getPackages().split("\n");
        if (packageIndex < listOfPacks.length) {
            String[] p = listOfPacks[packageIndex].split("\\|");
            if (p.length > INDEX_EAN_CODE_IN_PACK) {
                this.eanCode = p[INDEX_EAN_CODE_IN_PACK]; // 2nd line in prescription view
            }
        }

        String[] listOfPackInfos = medi.getPackInfo().split("\n");
        if (packageIndex < listOfPackInfos.length) {
            this.packageInfo = listOfPackInfos[packageIndex];
        }

        this.prodName = "";    // TODO
        this.comment = "";

        this.title = medi.getTitle();
        this.auth = medi.getAuth();
        this.atccode = medi.getAtcCode();
        this.regnrs = medi.getRegnrs();

    }

    public Product(JSONObject obj) {
        this.eanCode = obj.optString(KEY_AMK_MED_EAN);
        this.packageInfo = obj.optString(KEY_AMK_MED_PACKAGE);
        this.prodName = obj.optString(KEY_AMK_MED_PROD_NAME);
        this.comment = obj.optString(KEY_AMK_MED_COMMENT);
        this.title = obj.optString(KEY_AMK_MED_TITLE);
        this.auth = obj.optString(KEY_AMK_MED_OWNER);
        this.regnrs = obj.optString(KEY_AMK_MED_REGNRS);
        this.atccode = obj.optString(KEY_AMK_MED_ATC);
    }

    public Product(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(KEY_AMK_MED_EAN)) {
                this.eanCode = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_PACKAGE)) {
                this.packageInfo = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_PROD_NAME)) {
                this.prodName = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_COMMENT)) {
                this.comment = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_TITLE)) {
                this.title = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_OWNER)) {
                this.auth = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_REGNRS)) {
                this.regnrs = reader.nextString();
            } else if (name.equals(KEY_AMK_MED_ATC)) {
                this.atccode = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        try {
            j.put(KEY_AMK_MED_EAN, this.eanCode);
            j.put(KEY_AMK_MED_PACKAGE, this.packageInfo);
            j.put(KEY_AMK_MED_PROD_NAME, this.prodName);
            j.put(KEY_AMK_MED_COMMENT, this.comment);
            j.put(KEY_AMK_MED_TITLE, this.title);
            j.put(KEY_AMK_MED_OWNER, this.auth);
            j.put(KEY_AMK_MED_REGNRS, this.regnrs);
            j.put(KEY_AMK_MED_ATC, this.atccode);
        } catch(Exception e) {
            Log.w("Amiko.Product", e.toString());
        }
        return j;
    }

    public void writeJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(KEY_AMK_MED_EAN).value(this.eanCode);
        writer.name(KEY_AMK_MED_PACKAGE).value(this.packageInfo);
        writer.name(KEY_AMK_MED_PROD_NAME).value(this.prodName);
        writer.name(KEY_AMK_MED_COMMENT).value(this.comment);
        writer.name(KEY_AMK_MED_TITLE).value(this.title);
        writer.name(KEY_AMK_MED_OWNER).value(this.auth);
        writer.name(KEY_AMK_MED_REGNRS).value(this.regnrs);
        writer.name(KEY_AMK_MED_ATC).value(this.atccode);
        writer.endObject();
    }

    /**
     * This is not a proper function converting a product to medication,
     * the result is missing details and should be used only for interactions.
     */
    public Medication toMedicationForInteraction() {
        Medication m = new Medication();
        m.setRegnrs(this.regnrs);
        m.setAuth(this.auth);
        m.setAtcCode(this.atccode); // We only need this one for interactions
        m.setTitle(this.title);
        return m;
    }
}
