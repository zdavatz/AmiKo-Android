package com.ywesee.amiko;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONObject;

import java.io.Serializable;

import static com.ywesee.amiko.PatientDBAdapter.KEY_ADDRESS;
import static com.ywesee.amiko.PatientDBAdapter.KEY_BIRTHDATE;
import static com.ywesee.amiko.PatientDBAdapter.KEY_CITY;
import static com.ywesee.amiko.PatientDBAdapter.KEY_COUNTRY;
import static com.ywesee.amiko.PatientDBAdapter.KEY_EMAIL;
import static com.ywesee.amiko.PatientDBAdapter.KEY_FAMILYNAME;
import static com.ywesee.amiko.PatientDBAdapter.KEY_GENDER;
import static com.ywesee.amiko.PatientDBAdapter.KEY_GIVENNAME;
import static com.ywesee.amiko.PatientDBAdapter.KEY_HEIGHT_CM;
import static com.ywesee.amiko.PatientDBAdapter.KEY_PHONE;
import static com.ywesee.amiko.PatientDBAdapter.KEY_ROWID;
import static com.ywesee.amiko.PatientDBAdapter.KEY_TIMESTAMP;
import static com.ywesee.amiko.PatientDBAdapter.KEY_UID;
import static com.ywesee.amiko.PatientDBAdapter.KEY_WEIGHT_KG;
import static com.ywesee.amiko.PatientDBAdapter.KEY_ZIPCODE;

public class Patient implements Serializable {
    public long rowId;
    public String timestamp = null;
    public String uid = null;
    public String familyname = null;
    public String givenname = null;
    public String birthdate = null;
    public String gender = null;
    public int weight_kg;
    public int height_cm;
    public String zipcode = null;
    public String city = null;
    public String country = null;
    public String address = null;
    public String phone = null;
    public String email = null;

    static public final String KEY_AMK_PAT_ID = "patient_id";
    static public final String KEY_AMK_PAT_NAME = "given_name";
    static public final String KEY_AMK_PAT_SURNAME = "family_name";
    static public final String KEY_AMK_PAT_BIRTHDATE = "birth_date";
    static public final String KEY_AMK_PAT_WEIGHT = "weight_kg";
    static public final String KEY_AMK_PAT_HEIGHT = "height_cm";
    static public final String KEY_AMK_PAT_GENDER = "gender";
    static public final String KEY_AMK_PAT_GENDER_M = "man";
    static public final String KEY_AMK_PAT_GENDER_F = "woman";
    static public final String KEY_AMK_PAT_ADDRESS = "postal_address";
    static public final String KEY_AMK_PAT_ZIP = "zip_code";
    static public final String KEY_AMK_PAT_CITY = "city";
    static public final String KEY_AMK_PAT_COUNTRY = "country";
    static public final String KEY_AMK_PAT_PHONE = "phone_number";
    static public final String KEY_AMK_PAT_EMAIL = "email_address";

    Patient() {
        this.timestamp = Utilities.currentTimeString();
    }
    Patient(Cursor cursor) {
        rowId = cursor.getLong(0);
        timestamp = cursor.getString(1);
        uid = cursor.getString(2);
        familyname = cursor.getString(3);
        givenname = cursor.getString(4);
        birthdate = cursor.getString(5);
        gender = cursor.getString(6);
        weight_kg = cursor.getInt(7);
        height_cm = cursor.getInt(8);
        zipcode = cursor.getString(9);
        city = cursor.getString(10);
        country = cursor.getString(11);
        address = cursor.getString(12);
        phone = cursor.getString(13);
        email = cursor.getString(14);
    }
    public Patient(JSONObject json) {
        this.uid = json.optString(KEY_AMK_PAT_ID);
        this.givenname = json.optString(KEY_AMK_PAT_NAME);
        this.familyname = json.optString(KEY_AMK_PAT_SURNAME);
        this.birthdate = json.optString(KEY_AMK_PAT_BIRTHDATE);
        this.weight_kg = json.optInt(KEY_AMK_PAT_WEIGHT);
        this.height_cm = json.optInt(KEY_AMK_PAT_HEIGHT);
        this.gender = json.optString(KEY_AMK_PAT_GENDER);
        this.address = json.optString(KEY_AMK_PAT_ADDRESS);
        this.zipcode = json.optString(KEY_AMK_PAT_ZIP);
        this.city = json.optString(KEY_AMK_PAT_CITY);
        this.country = json.optString(KEY_AMK_PAT_COUNTRY);
        this.phone = json.optString(KEY_AMK_PAT_PHONE);
        this.email = json.optString(KEY_AMK_PAT_EMAIL);
    }

    ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        this.uid = String.format("%d", this.hashValue());

//        values.put(KEY_ROWID, rowid);
        values.put(KEY_TIMESTAMP, timestamp);
        values.put(KEY_UID, uid);
        values.put(KEY_FAMILYNAME, familyname);
        values.put(KEY_GIVENNAME, givenname);
        values.put(KEY_BIRTHDATE, birthdate);
        values.put(KEY_GENDER, gender);
        values.put(KEY_WEIGHT_KG, weight_kg);
        values.put(KEY_HEIGHT_CM, height_cm);
        values.put(KEY_ZIPCODE, zipcode);
        values.put(KEY_CITY, city);
        values.put(KEY_COUNTRY, country);
        values.put(KEY_ADDRESS, address);
        values.put(KEY_PHONE, phone);
        values.put(KEY_EMAIL, email);

        return values;
    }

    public int hashValue() {
        return String.format("%s.%s.%s", this.familyname, this.givenname, this.birthdate).hashCode();
    }

    public String stringForDisplay() {
        return this.familyname + " " + this.givenname;
    }

    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        try {
            j.put(KEY_AMK_PAT_ID, this.uid);
            j.put(KEY_AMK_PAT_NAME, this.givenname);
            j.put(KEY_AMK_PAT_SURNAME, this.familyname);
            j.put(KEY_AMK_PAT_BIRTHDATE, this.birthdate);
            j.put(KEY_AMK_PAT_WEIGHT, Integer.toString(this.weight_kg));
            j.put(KEY_AMK_PAT_HEIGHT, Integer.toString(this.height_cm));
            j.put(KEY_AMK_PAT_GENDER, this.gender);
            j.put(KEY_AMK_PAT_ADDRESS, this.address);
            j.put(KEY_AMK_PAT_ZIP, this.zipcode);
            j.put(KEY_AMK_PAT_CITY, this.city);
            j.put(KEY_AMK_PAT_COUNTRY, this.country);
            j.put(KEY_AMK_PAT_PHONE, this.phone);
            j.put(KEY_AMK_PAT_EMAIL, this.email);
        } catch (Exception e) {
            Log.w("Amiko.Patient", e.toString());
        }
        return j;
    }
}
