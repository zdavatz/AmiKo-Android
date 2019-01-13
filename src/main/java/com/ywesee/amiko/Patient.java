package com.ywesee.amiko;

import android.content.ContentValues;
import android.database.Cursor;

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
}
