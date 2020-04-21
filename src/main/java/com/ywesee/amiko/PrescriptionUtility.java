package com.ywesee.amiko;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PrescriptionUtility {
    public static String amkDirectory(Context context) {
        return new File(context.getFilesDir(), "amk").getAbsolutePath();
    }
    public static String pdfDirectory(Context context) {
        return new File(context.getCacheDir(), "pdf").getAbsolutePath();
    }
    public static String amkDirectoryForPatient(Context context, Patient p) {
        if (p == null) {
            return PrescriptionUtility.amkDirectory(context);
        }
        return new File(
            PrescriptionUtility.amkDirectory(context),
            p.uid
        ).getAbsolutePath();
    }
    public static String prettyTime() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy (HH:mm:ss)");
        return format.format(new Date());
    }
    public static String currentTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
        return format.format(new Date());
    }

    public static String currentFilenameWithExtension(String extension) {
        return "RZ_" + PrescriptionUtility.currentTime().replace(":", "").replace(".", "") + "." + extension;
    }

    public static File savePrescription(Context c, Prescription p) {
        String filename = currentFilenameWithExtension("amk");
        File amkFile = new File(
            PrescriptionUtility.amkDirectoryForPatient(c, p.patient),
            filename
        );
        ensureDirectory(PrescriptionUtility.amkDirectoryForPatient(c, p.patient));
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(amkFile);
            Base64OutputStream b64Output = new Base64OutputStream(stream, Base64.NO_WRAP);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(b64Output, "UTF-8"));
            p.writeJSON(writer);
            writer.close();
        } catch(Exception e) {

        } finally {
            try {
                stream.close();
            } catch (Exception e) { }
        }
        return amkFile;
    }
    public static Prescription readFromFile(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            Base64InputStream b64Stream = new Base64InputStream(inputStream, Base64.DEFAULT );
            JsonReader jsonReader = new JsonReader(new InputStreamReader(b64Stream, "UTF-8"));
            Prescription p = new Prescription(jsonReader);
            jsonReader.close();
            return p;
        } catch (Exception e) {
            Log.e("PrescriptionUtility", "Cannot parse file json: " + e.toString() + ":" + e.getLocalizedMessage());
        }
        return null;
    }

    public static Prescription readFromResourceUri(Context c, Uri uri) throws IOException {
        InputStream inputStream = c.getContentResolver().openInputStream(uri);
        Base64InputStream b64Stream = new Base64InputStream(inputStream, Base64.DEFAULT);
        JsonReader jsonReader = new JsonReader(new InputStreamReader(b64Stream, "UTF-8"));
        Prescription p = new Prescription(jsonReader);
        jsonReader.close();
        return p;
    }

    public static ArrayList<File> amkFilesInDirectory(String path) {
        ensureDirectory(path);
        ArrayList<File> result = new ArrayList<File>();
        File folder = new File(path);
        File[] filesInFolder = folder.listFiles();
        for (File file : filesInFolder) {
            String filename = file.getName();
            if (!file.isDirectory() && filename.endsWith(".amk")) {
                result.add(file);
            }
        }
        return result;
    }
    public static ArrayList<File> amkFilesAtBaseDirectory(Context c) {
        return PrescriptionUtility.amkFilesInDirectory(PrescriptionUtility.amkDirectory(c));
    }
    public static ArrayList<File> amkFilesforPatient(Context c, Patient p) {
        return PrescriptionUtility.amkFilesInDirectory(PrescriptionUtility.amkDirectoryForPatient(c, p));
    }
    public static ArrayList<File> amkFilesForCurrentPatient(Context c) {
        Patient p = Patient.loadCurrentPatient(c);
        if (p == null) {
            return new ArrayList<>();
        }
        return PrescriptionUtility.amkFilesforPatient(c, p);
    }
    public static void ensureAmkDirectory(Context c) {
        PrescriptionUtility.ensureDirectory(PrescriptionUtility.amkDirectory(c));
    }
    public static void ensureAmkDirectoryForPatient(Context c, Patient p) {
        PrescriptionUtility.ensureDirectory(PrescriptionUtility.amkDirectoryForPatient(c, p));
    }
    public static void ensureDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
    public static void deletePatientDirectory(Context c, Patient p) {
        File folder = new File(amkDirectoryForPatient(c, p));
        if (!folder.exists()) return;
        for (File file: folder.listFiles()) {
            file.delete();
        }
        folder.delete();
    }
}
