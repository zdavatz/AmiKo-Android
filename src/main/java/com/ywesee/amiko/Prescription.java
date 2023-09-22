package com.ywesee.amiko;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.google.android.gms.common.util.ArrayUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

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

    public String bodyForEPrescription() throws JSONException, IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        DateFormat patientDOBDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        DateFormat ePrescriptionDOBFormat = new SimpleDateFormat("yyyy-MM-dd");
        JSONArray medications = new JSONArray();
        for (Product p : this.medications) {
            JSONObject o = new JSONObject();
            o.put("Id", p.eanCode);
            o.put("IdType", 2); // GTIN
            medications.put(o);
        }
        JSONObject patient = new JSONObject();
        patient.put("FName", this.patient.givenname);
        patient.put("LName", this.patient.familyname);
        patient.put("BDt", ePrescriptionDOBFormat.format(patientDOBDateFormat.parse(this.patient.birthdate)));
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("Patient", patient);
        jsonBody.put("Medicaments", medications);
        jsonBody.put("MedType", 3); // Prescription
        jsonBody.put("Id", UUID.randomUUID().toString());
        jsonBody.put("Auth", this.doctor.gln);
        jsonBody.put("Dt", df.format(new Date()));
        String jsonString = jsonBody.toString();
        ByteArrayOutputStream os = new ByteArrayOutputStream(jsonString.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(jsonString.getBytes(StandardCharsets.UTF_8));
        gos.close();
        byte[] compressed = os.toByteArray();
        Base64.Encoder base64Encoder = Base64.getEncoder();
        String result = "CHMED16A1" + base64Encoder.encodeToString(compressed);
        return result;
    }
}
