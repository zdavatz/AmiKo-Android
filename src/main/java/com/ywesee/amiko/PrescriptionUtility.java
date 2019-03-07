package com.ywesee.amiko;

import android.content.Context;
import android.util.Base64;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class PrescriptionUtility {
    public static String amkDirectory(Context context) {
        return new File(context.getFilesDir(), "amk").getAbsolutePath();
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

    public static String placeDate(Prescription p) {
        return p.doctor.city + " " + PrescriptionUtility.prettyTime();
    }
    public static File savePrescription(Context c, Prescription p) {
        JSONObject jsonObj = p.toJSON();
        String jsonString = jsonObj.toString();
        String base64 = Base64.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        String filename = "RZ_" + PrescriptionUtility.currentTime().replace(":", "").replace(".", "") + ".amk";
        File amkFile = new File(
            PrescriptionUtility.amkDirectoryForPatient(c, p.patient),
            filename
        );
        ensureDirectory(PrescriptionUtility.amkDirectoryForPatient(c, p.patient));
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(amkFile);
            stream.write(base64.getBytes());
        } catch(Exception e) {

        } finally {
            try {
                stream.close();
            } catch (Exception e) { }
        }
        return amkFile;
    }
    public static Prescription readFromFile(File file) {
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(bytes);
        } catch(Exception e) {

        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {

            }
        }
        String base64Encoded = new String(bytes, StandardCharsets.UTF_8);
        String jsonString = new String(Base64.decode(base64Encoded, Base64.DEFAULT), StandardCharsets.UTF_8);
        try {
            JSONObject obj = new JSONObject(jsonString);
            return new Prescription(obj);
        } catch (Exception e) {

        }
        return null;
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
}