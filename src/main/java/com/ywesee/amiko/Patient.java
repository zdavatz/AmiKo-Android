package com.ywesee.amiko;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

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

    private static final String PATIENT_PREFS_FILE = "PatientPrefsFile";

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

    public Patient(JsonReader reader) throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(KEY_AMK_PAT_ID)){
                this.uid = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_NAME)){
                this.givenname = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_SURNAME)){
                this.familyname = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_BIRTHDATE)){
                this.birthdate = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_WEIGHT)){
                this.weight_kg = reader.nextInt();
            } else if (name.equals(KEY_AMK_PAT_HEIGHT)){
                this.height_cm = reader.nextInt();
            } else if (name.equals(KEY_AMK_PAT_GENDER)){
                this.gender = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_ADDRESS)){
                this.address = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_ZIP)){
                this.zipcode = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_CITY)){
                this.city = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_COUNTRY)){
                this.country = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_PHONE)){
                this.phone = reader.nextString();
            } else if (name.equals(KEY_AMK_PAT_EMAIL)){
                this.email = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        this.uid = this.hashValue();

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

    public String hashValue() {
        return Utilities.foundationHashString(String.format("%s.%s.%s", this.familyname, this.givenname, this.birthdate));
    }

    public String stringForDisplay() {
        return this.familyname + " " + this.givenname;
    }

    public void writeJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(KEY_AMK_PAT_ID).value(this.uid);
        writer.name(KEY_AMK_PAT_NAME).value(this.givenname);
        writer.name(KEY_AMK_PAT_SURNAME).value(this.familyname);
        writer.name(KEY_AMK_PAT_BIRTHDATE).value(this.birthdate);
        writer.name(KEY_AMK_PAT_WEIGHT).value(Integer.toString(this.weight_kg));
        writer.name(KEY_AMK_PAT_HEIGHT).value(Integer.toString(this.height_cm));
        writer.name(KEY_AMK_PAT_GENDER).value(this.gender);
        writer.name(KEY_AMK_PAT_ADDRESS).value(this.address);
        writer.name(KEY_AMK_PAT_ZIP).value(this.zipcode);
        writer.name(KEY_AMK_PAT_CITY).value(this.city);
        writer.name(KEY_AMK_PAT_COUNTRY).value(this.country);
        writer.name(KEY_AMK_PAT_PHONE).value(this.phone);
        writer.name(KEY_AMK_PAT_EMAIL).value(this.email);
        writer.endObject();
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

    static public String getCurrentPatientId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PATIENT_PREFS_FILE, Context.MODE_PRIVATE);
        return preferences.getString("currentPatient", null);
    }
    static public void setCurrentPatientId(Context context, String patientId) {
        SharedPreferences preferences = context.getSharedPreferences(PATIENT_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (patientId != null) {
            editor.putString("currentPatient", patientId);
        } else {
            editor.remove("currentPatient");
        }
        editor.commit();
    }
    static public Patient loadCurrentPatient(Context c) {
        String uid = Patient.getCurrentPatientId(c);
        if (uid == null) return null;
        PatientDBAdapter db = new PatientDBAdapter(c);
        Patient p = db.getPatientWithUniqueId(uid);
        return p;
    }
 }
