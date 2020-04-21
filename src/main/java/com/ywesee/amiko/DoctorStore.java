package com.ywesee.amiko;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class DoctorStore {
    public static final String DOC_SIGNATURE_FILENAME = "op_signature.png";

    private String dir;

    public String title;
    public String name;
    public String surname;
    public String street;
    public String city;
    public String zip;
    public String phone;
    public String email;

    public DoctorStore(Context context) {
        dir = context.getFilesDir().toString();
        this.migrateFromOldFormat();
    }

    public DoctorStore(String baseDir) {
        dir = baseDir;
        this.migrateFromOldFormat();
    }

    public void migrateFromOldFormat() {
        // We used to use ObjectOutputStream for saving file, migrate it to JSON for syncing with other platforms
        if (this.oldFileExists()) {
            File doctorFile = new File(dir, "doctor.txt");
            String targetPath = doctorFile.getPath();
            HashMap<String, String> map = new HashMap<String, String>();
            try {
                FileInputStream fileIn = new FileInputStream(targetPath);
                // Make sure there is something to read...
                if (fileIn.available() > 0) {
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    map = (HashMap<String, String>) in.readObject();
                    in.close();
                }
                fileIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            this.title = map.get("title");
            this.name = map.get("name");
            this.surname = map.get("surname");
            this.street = map.get("street");
            this.city = map.get("city");
            this.zip = map.get("zip");
            this.phone = map.get("phone");
            this.email = map.get("email");
            this.save();
            doctorFile.delete();
        }
        File oldDoctorSignature = new File(dir, "doctor.png");
        if (oldDoctorSignature.exists()) {
            oldDoctorSignature.renameTo(new File(dir, DOC_SIGNATURE_FILENAME));
        }
    }

    public boolean exists() {
        if (this.oldFileExists()) {
            return true;
        }
        File f = new File(dir, "doctor.json");
        return f.exists() && f.isFile();
    }

    private boolean oldFileExists() {
        File f = new File(dir, "doctor.txt");
        return f.exists() && f.isFile();
    }

    public void load() {
        String targetPath = new File(dir, "doctor.json").getPath();
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            FileInputStream fileIn = new FileInputStream(targetPath);
            JsonReader reader = new JsonReader(new InputStreamReader(fileIn, StandardCharsets.UTF_8));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("title")) {
                    this.title = reader.nextString();
                } else if (name.equals("name")) {
                    this.name = reader.nextString();
                } else if (name.equals("surname")) {
                    this.surname = reader.nextString();
                }
                else if (name.equals("street")) {
                    this.street = reader.nextString();
                }
                else if (name.equals("city")) {
                    this.city = reader.nextString();
                }
                else if (name.equals("zip")) {
                    this.zip = reader.nextString();
                }
                else if (name.equals("phone")) {
                    this.phone = reader.nextString();
                }
                else if (name.equals("email")) {
                    this.email = reader.nextString();
                }
            }
            reader.endObject();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            String targetPath = new File(dir, "doctor.json").getPath();
            FileOutputStream fileOut = new FileOutputStream(targetPath);

            JsonWriter writer = new JsonWriter(new OutputStreamWriter(fileOut, StandardCharsets.UTF_8));
            writer.beginObject();
            writer.name("title").value(this.title);
            writer.name("name").value(this.name);
            writer.name("surname").value(this.surname);
            writer.name("street").value(this.street);
            writer.name("city").value(this.city);
            writer.name("zip").value(this.zip);
            writer.name("phone").value(this.phone);
            writer.name("email").value(this.email);
            writer.endObject();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getSignature() {
        String targetPath = new File(dir, DOC_SIGNATURE_FILENAME).getPath();
        Bitmap bm = BitmapFactory.decodeFile(targetPath);
        return bm;
    }

    public void saveSignature(Bitmap bitmap) {
        try {
            String targetPath = new File(dir, DOC_SIGNATURE_FILENAME).getPath();
            File file = new File(targetPath);
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
