package com.ywesee.amiko;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class Operator {
    public static final String KEY_AMK_DOC_TITLE = "title";
    public static final String KEY_AMK_DOC_NAME = "given_name";
    public static final String KEY_AMK_DOC_SURNAME = "family_name";
    public static final String KEY_AMK_DOC_ADDRESS = "postal_address";
    public static final String KEY_AMK_DOC_CITY = "city";
    public static final String KEY_AMK_DOC_ZIP = "zip_code";
    public static final String KEY_AMK_DOC_PHONE = "phone_number";
    public static final String KEY_AMK_DOC_EMAIL = "email_address";

    public static final String KEY_AMK_DOC_SIGNATURE = "signature";

    public String title;
    public String givenName;
    public String familyName;
    public String postalAddress;
    public String city;
    public String zipCode;
    public String phoneNumber;
    public String emailAddress;

    // base64 of png of doctor signature image
    private String signature;
    private Bitmap signatureImage;

    private Operator() {}

    public Operator(JSONObject obj) {
        this.title = obj.optString(KEY_AMK_DOC_TITLE, "");
        this.familyName = obj.optString(KEY_AMK_DOC_SURNAME, "");
        this.givenName = obj.optString(KEY_AMK_DOC_NAME, "");
        this.postalAddress = obj.optString(KEY_AMK_DOC_ADDRESS, "");
        this.city = obj.optString(KEY_AMK_DOC_CITY, "");
        this.zipCode = obj.optString(KEY_AMK_DOC_ZIP, "");
        this.phoneNumber = obj.optString(KEY_AMK_DOC_PHONE, "");
        this.emailAddress = obj.optString(KEY_AMK_DOC_EMAIL, "");

        String signatureString = obj.optString(KEY_AMK_DOC_SIGNATURE, "");
        if (!signatureString.equals("")) {
            this.setSignature(signatureString);
        }
    }

    static public Operator loadFromStore(String dirPath) {
        DoctorStore store = new DoctorStore(dirPath);
        if (!store.exists()) {
            Operator o = new Operator();
            o.title = "";
            o.givenName = "";
            o.familyName = "";
            o.postalAddress = "";
            o.city = "";
            o.zipCode = "";
            o.phoneNumber = "";
            o.emailAddress = "";
        }
        store.load();

        Operator o = new Operator();
        o.title = store.title;
        o.givenName = store.name;
        o.familyName = store.surname;
        o.postalAddress = store.street;
        o.city = store.city;
        o.zipCode = store.zip;
        o.phoneNumber = store.phone;
        o.emailAddress = store.email;
        o.setSignatureImage(store.getSignature());

        return o;
    }

    public void setSignatureImage(Bitmap bm) {
        this.signatureImage = bm;
        if (bm != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            this.signature = encoded;
        } else {
            this.signature = null;
        }
    }

    public void setSignature(String encoded) {
        this.signature = encoded;
        byte[] decodedString = Base64.decode(encoded, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        this.signatureImage = decodedByte;
    }

    public Bitmap getSignatureImage() {
        return this.signatureImage;
    }

    public String getSignature() {
        return this.signature;
    }

    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        try {
            if (this.title == null) {
                j.put(KEY_AMK_DOC_TITLE, "");
            } else {
                j.put(KEY_AMK_DOC_TITLE, this.title);
            }
            j.put(KEY_AMK_DOC_NAME, this.givenName);
            j.put(KEY_AMK_DOC_SURNAME, this.familyName);
            j.put(KEY_AMK_DOC_ADDRESS, this.postalAddress);
            j.put(KEY_AMK_DOC_CITY, this.city);
            j.put(KEY_AMK_DOC_ZIP, this.zipCode);
            j.put(KEY_AMK_DOC_PHONE, this.phoneNumber);
            j.put(KEY_AMK_DOC_EMAIL, this.emailAddress);
            j.put(KEY_AMK_DOC_SIGNATURE, this.signature);
        } catch (Exception e) {
            Log.w("Amiko.Operator", e.toString());
        }
        return j;
    }
}
