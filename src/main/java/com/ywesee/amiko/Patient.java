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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
        try {
            String str = json.optString(KEY_AMK_PAT_WEIGHT);
            this.weight_kg = Integer.parseInt(str);
        } catch (Exception e) {
            this.weight_kg = 0;
        }
        try {
            String str = json.optString(KEY_AMK_PAT_HEIGHT);
            this.height_cm = Integer.parseInt(str);
        } catch (Exception e) {
            this.height_cm = 0;
        }
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
                try {
                    String str = reader.nextString();
                    this.weight_kg = Integer.parseInt(str);
                } catch (Exception e) {
                    this.weight_kg = 0;
                }
            } else if (name.equals(KEY_AMK_PAT_HEIGHT)){
                try {
                    String str = reader.nextString();
                    this.height_cm = Integer.parseInt(str);
                } catch (Exception e) {
                    this.height_cm = 0;
                }
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

    public Patient(Map<String, String> map) {
        this.timestamp = map.get(KEY_TIMESTAMP);
        this.uid = map.get(KEY_UID);
        this.familyname = map.get(KEY_FAMILYNAME);
        this.givenname = map.get(KEY_GIVENNAME);
        this.birthdate = map.get(KEY_BIRTHDATE);
        this.gender = map.get(KEY_GENDER);
        try {
            this.weight_kg = Integer.parseInt(map.get(KEY_WEIGHT_KG));
        } catch (Exception e) {
            this.weight_kg = 0;
        }
        try {
            this.height_cm = Integer.parseInt(map.get(KEY_HEIGHT_CM));
        } catch (Exception e) {
            this.height_cm = 0;
        }
        this.zipcode = map.get(KEY_ZIPCODE);
        this.city = map.get(KEY_CITY);
        this.country = map.get(KEY_COUNTRY);
        this.address = map.get(KEY_ADDRESS);
        this.phone = map.get(KEY_PHONE);
        this.email = map.get(KEY_EMAIL);
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

    HashMap<String, String> toMap() {
        HashMap<String, String> map = new HashMap<>();
        this.uid = this.hashValue();

        map.put(KEY_TIMESTAMP, timestamp);
        map.put(KEY_UID, uid);
        map.put(KEY_FAMILYNAME, familyname);
        map.put(KEY_GIVENNAME, givenname);
        map.put(KEY_BIRTHDATE, birthdate);
        map.put(KEY_GENDER, gender);
        map.put(KEY_WEIGHT_KG, Integer.toString(weight_kg));
        map.put(KEY_HEIGHT_CM, Integer.toString(height_cm));
        map.put(KEY_ZIPCODE, zipcode);
        map.put(KEY_CITY, city);
        map.put(KEY_COUNTRY, country);
        map.put(KEY_ADDRESS, address);
        map.put(KEY_PHONE, phone);
        map.put(KEY_EMAIL, email);

        return map;
    }

    public String hashValue() {
        return Utilities.foundationHashString(String.format("%s.%s.%s", this.familyname, this.givenname, this.birthdate));
    }

    public String stringForDisplay() {
        return this.familyname + " " + this.givenname;
    }

    public String getStringForPrescriptionPrinting() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.givenname).append(" ").append(this.familyname).append("\n");
        sb.append(this.address).append("\n");
        sb.append(this.zipcode).append(" ").append(this.city).append("\n");
        return sb.toString();
    }

    public String getStringForLabelPrinting(Context c) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.givenname).append(" ").append(this.familyname).append(", ").append(c.getString(R.string.born)).append(" ").append(this.birthdate);
        return sb.toString();
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
