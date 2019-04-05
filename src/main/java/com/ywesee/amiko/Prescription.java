package com.ywesee.amiko;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class Prescription {
    public static final String KEY_AMK_HASH = "prescription_hash";
    public static final String KEY_AMK_PLACE_DATE = "place_date";
    public static final String KEY_AMK_PATIENT = "patient";
    public static final String KEY_AMK_OPERATOR = "operator";
    public static final String KEY_AMK_MEDICATIONS = "medications";

    public String hash;
    public String placeDate;
    public Operator doctor;
    public Patient patient;
    public ArrayList<Product> medications;

    public Prescription() {
    }

    public Prescription(JSONObject json) {
        this.hash = json.optString(KEY_AMK_HASH);
        this.placeDate = json.optString(KEY_AMK_PLACE_DATE, json.optString("date"));
        try {
            this.patient = new Patient(json.getJSONObject(KEY_AMK_PATIENT));
            this.doctor = new Operator(json.getJSONObject(KEY_AMK_OPERATOR));
            this.medications = new ArrayList<Product>();
            JSONArray arr = json.getJSONArray(KEY_AMK_MEDICATIONS);;
            for (int i = 0; i < arr.length(); i++) {
                this.medications.add(new Product(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.w("Amiko.Prescription", e.toString());
        }
    }

    public Prescription(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(KEY_AMK_HASH)) {
                this.hash = reader.nextString();
            } else if (name.equals(KEY_AMK_PLACE_DATE)) {
                this.placeDate = reader.nextString();
            } else if (name.equals(KEY_AMK_PATIENT)) {
                this.patient = new Patient(reader);
            } else if (name.equals(KEY_AMK_OPERATOR)) {
                this.doctor = new Operator(reader);
            } else if (name.equals(KEY_AMK_MEDICATIONS)) {
                this.medications = new ArrayList<Product>();
                reader.beginArray();
                while (reader.hasNext()) {
                    this.medications.add(new Product(reader));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    public void writeJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(KEY_AMK_HASH).value(this.hash);
        writer.name(KEY_AMK_PLACE_DATE).value(this.placeDate);

        writer.name(KEY_AMK_PATIENT);
        this.patient.writeJSON(writer);

        writer.name(KEY_AMK_OPERATOR);
        this.doctor.writeJSON(writer);

        writer.name(KEY_AMK_MEDICATIONS);
        writer.beginArray();
        for (Product p : this.medications) {
            p.writeJSON(writer);
        }
        writer.endArray();

        writer.endObject();
    }

    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        try {
            j.put(KEY_AMK_HASH, this.hash);
            j.put(KEY_AMK_PLACE_DATE, this.placeDate);
            j.put(KEY_AMK_PATIENT, this.patient.toJSON());
            j.put(KEY_AMK_OPERATOR, this.doctor.toJSON());

            JSONArray medis = new JSONArray();
            for (Product p : this.medications) {
                medis.put(p.toJSON());
            }
            j.put(KEY_AMK_MEDICATIONS, medis);
        } catch (Exception e) {
            Log.w("Amiko.Prescription", e.toString());
        }
        return j;
    }
}
