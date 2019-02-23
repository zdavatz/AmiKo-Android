package com.ywesee.amiko;

import android.util.Log;

import org.json.JSONObject;

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
            String[] p = listOfPacks[packageIndex].split("|");
            if (p.length > INDEX_EAN_CODE_IN_PACK) {
                this.eanCode = p[INDEX_EAN_CODE_IN_PACK]; // 2nd line in prescription view
            }
        }

        String[] listOfPackInfos = medi.getPackInfo().split("\n");
        if (packageIndex < listOfPackInfos.length) {
            this.packageInfo = listOfPackInfos[packageIndex];
        }

        this.prodName = "";    // TODO
        this.comment = "";     // TODO

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
}
