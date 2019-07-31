package com.ywesee.amiko;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import static com.ywesee.amiko.DoctorActivity.MIN_SIGNATURE_HEIGHT;
import static com.ywesee.amiko.DoctorActivity.MIN_SIGNATURE_WIDTH;
import static java.lang.Math.min;

public class Operator implements Serializable {
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

    public Operator(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(KEY_AMK_DOC_TITLE)) {
                this.title = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_SURNAME)) {
                this.familyName = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_NAME)) {
                this.givenName = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_ADDRESS)) {
                this.postalAddress = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_CITY)) {
                this.city = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_ZIP)) {
                this.zipCode = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_PHONE)) {
                this.phoneNumber = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_EMAIL)) {
                this.emailAddress = reader.nextString();
            } else if (name.equals(KEY_AMK_DOC_SIGNATURE)) {
                this.setSignature(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    static public Operator loadFromStore(String dirPath) {
        DoctorStore store = new DoctorStore(dirPath);
        if (!store.exists()) {
            return null;
        }
        store.load();

        if (store.name == null || store.surname == null) {
            return null;
        }
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

    public Bitmap getSignatureThumbnailForPrinting() {
        Rect rect = new Rect(0, 0, MIN_SIGNATURE_WIDTH, MIN_SIGNATURE_HEIGHT);
        Bitmap bm = this.getSignatureImage();
        if (bm == null) return null;
        Bitmap baseBitmap = Bitmap.createBitmap(rect.width(), rect.height(), bm.getConfig());

        Canvas canvas = new Canvas(baseBitmap );

        Paint bg = new Paint();
        bg.setColor(Color.rgb(245, 245, 245));
        canvas.drawRect(rect, bg);

        // resize
        float widthRatio = rect.width() / (bm.getWidth() / 1.0f);
        float heightRatio = rect.height() / (bm.getHeight() / 1.0f);
        float ratio = min(widthRatio, heightRatio);

        int width = (int)(bm.getWidth() * ratio);
        int height = (int)(bm.getHeight() * ratio);

        Bitmap scaled = Bitmap.createScaledBitmap(bm, width, height, false);
        canvas.drawBitmap(scaled,
                (rect.width() - width) / 2.0f,
                (rect.height() - height) / 2.0f,
                null);

        return baseBitmap;
    }

    public String getStringForPrescriptionPrinting() {
        StringBuilder sb = new StringBuilder();
        if (this.title != null && !this.title.equals("")) {
            sb.append(this.title).append(" ");
        }
        sb.append(this.givenName).append(" ").append(this.familyName).append("\n");
        sb.append(this.postalAddress).append("\n");
        sb.append(this.zipCode).append(" ").append(city).append("\n");
        sb.append(this.emailAddress);
        return sb.toString();
    }

    public String getStringForLabelPrinting() {
        StringBuilder sb = new StringBuilder();
        if (this.title != null && !this.title.equals("")) {
            sb.append(this.title).append(" ");
        }
        sb.append(this.givenName).append(" ");
        sb.append(this.familyName).append(" - ");
        sb.append(this.zipCode).append(" ");
        return sb.toString();
    }

    public void writeJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        if (this.title == null) {
            writer.name(KEY_AMK_DOC_TITLE).value("");
        } else {
            writer.name(KEY_AMK_DOC_TITLE).value(this.title);
        }
        writer.name(KEY_AMK_DOC_NAME).value(this.givenName);
        writer.name(KEY_AMK_DOC_SURNAME).value(this.familyName);
        writer.name(KEY_AMK_DOC_ADDRESS).value(this.postalAddress);
        writer.name(KEY_AMK_DOC_CITY).value(this.city);
        writer.name(KEY_AMK_DOC_ZIP).value(this.zipCode);
        writer.name(KEY_AMK_DOC_PHONE).value(this.phoneNumber);
        writer.name(KEY_AMK_DOC_EMAIL).value(this.emailAddress);
        if (this.signature != null) {
            writer.name(KEY_AMK_DOC_SIGNATURE).value(this.signature);
        } else {
            writer.name(KEY_AMK_DOC_SIGNATURE).value("");
        }
        writer.endObject();
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
