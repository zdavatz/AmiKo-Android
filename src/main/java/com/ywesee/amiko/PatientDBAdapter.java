package com.ywesee.amiko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PatientDBAdapter extends SQLiteOpenHelper {

    private static final String TAG = "PatientDBAdapter"; // Tag for LogCat window

    private final Context mContext;

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TIMESTAMP = "time_stamp";
    public static final String KEY_UID = "uid";
    public static final String KEY_FAMILYNAME = "family_name";
    public static final String KEY_GIVENNAME = "given_name";
    public static final String KEY_BIRTHDATE = "birthdate";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_WEIGHT_KG = "weight_kg";
    public static final String KEY_HEIGHT_CM = "height_cm";
    public static final String KEY_ZIPCODE = "zip";
    public static final String KEY_CITY = "city";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_EMAIL = "email";

    public static final String DATABASE_TABLE = "patients";
    public static final int DB_VERSION = 1;

    /**
     * Table columns used for fast queries
     */
    private static final String SHORT_TABLE = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
            KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
            KEY_EMAIL);

    /**
     * Constructor
     * @param context
     */
    public PatientDBAdapter(Context context) {
        super(context, Constants.appPatientDatabase(), null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String schema = String.format(
            "(%s INTEGER PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
            KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
            KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE, KEY_EMAIL);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " " + schema + ";");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void close() {
        super.close();
    }

    public long insertRecord(Patient p) {
        ContentValues values = p.toContentValues();
        long id = this.getWritableDatabase().insert(DATABASE_TABLE, null, values);
        SyncManager.getShared().triggerSync();
        return id;
    }

    public long upsertRecordByUid(Patient p) {
        Patient existing = this.getPatientWithUniqueId(p.uid);
        if (existing != null) {
            ContentValues values = p.toContentValues();
            long id = this.getWritableDatabase().update(DATABASE_TABLE, values, KEY_UID + "= ? ", new String[]{p.uid});
            SyncManager.getShared().triggerSync();
            return id;
        } else {
            return this.insertRecord(p);
        }
    }

    /**
     * Deletes specific record from database
     * @param p
     * @return
     */
    public boolean deleteRecord(Patient p) {
        boolean result = this.getWritableDatabase().delete(DATABASE_TABLE, KEY_ROWID + "=" + p.rowId, null) > 0;
        SyncManager.getShared().triggerSync();
        return result;
    }

    public boolean deletePatientWithUid(String uid) {
        boolean result = this.getWritableDatabase().delete(DATABASE_TABLE, KEY_UID + "= ?", new String[] { uid }) > 0;
        SyncManager.getShared().triggerSync();
        return result;
    }

    /**
     * Retrieves all records in database
     * @return full list of entries
     */
    public List<Patient> getAllRecords() {
        List<Patient> patients = new ArrayList<Patient>();

        Cursor cursor = this.getReadableDatabase().query(DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
                        KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
                        KEY_EMAIL},
                null, null, null, null, KEY_FAMILYNAME + "," + KEY_GIVENNAME);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Patient patient = new Patient(cursor);
            patients.add(patient);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return patients;
    }

    /**
     * Executes search query. NOTE: increases readability of the code, HOWEVER slows down execution speed!
     * @param q: search query
     * @param m: list of medications returned by query
     */
    public void searchQuery(String q, List<Patient> m) {
        // Execute DB raw query
        Cursor cursor = this.getReadableDatabase().rawQuery(q, null);

        // Iterate through cursor to extract required info
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Patient patient= new Patient(cursor);
            m.add(patient);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        if (Constants.DEBUG)
            Log.d(TAG, q);
    }


    /**
     * Retrieves specific record
     * @param uid
     * @return cursor
     * @throws SQLException
     */
    public Patient getPatientWithUniqueId(String uid) throws SQLException {
        Cursor cursor = this.getReadableDatabase().query(DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
                        KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
                        KEY_EMAIL},
                KEY_UID + " = ?", new String[] {uid}, null, null, KEY_FAMILYNAME + "," + KEY_GIVENNAME);

        if (cursor != null && cursor.moveToFirst()) {
            Patient patient = new Patient(cursor);
            cursor.close();
            return patient;
        }
        return null;
    }

    /**
     * @return A uid -> timestamp map
     * @throws SQLException
     */
    public HashMap<String, String> getAllTimestamps() throws SQLException {
        Cursor cursor = this.getReadableDatabase().query(DATABASE_TABLE,
                new String[] {KEY_TIMESTAMP, KEY_UID},
                null, null, null, null, null);
        HashMap<String, String> map = new HashMap<>();
        // Iterate through cursor to extract required info
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String timestamp = cursor.getString(0);
            String uid = cursor.getString(1);
            map.put(uid, timestamp);
            cursor.moveToNext();
        }
        cursor.close();
        return map;
    }

    public Patient getPatientWithNamesAndBirthday(String familyname, String givenname, String birthdate) {
        Cursor cursor = this.getReadableDatabase().query(DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
                        KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
                        KEY_EMAIL},
                KEY_FAMILYNAME + " = ? AND " + KEY_GIVENNAME  + " = ? AND " + KEY_BIRTHDATE + " = ? ",
                new String[] {familyname, givenname, birthdate}, null, null, KEY_FAMILYNAME + "," + KEY_GIVENNAME);

        if (cursor != null && cursor.moveToFirst()) {
            Patient patient = new Patient(cursor);
            cursor.close();
            return patient;
        }
        return null;
    }

    public ArrayList<Patient> getPatientsWithUids(Set<String> uids) {
        String idsString = "";
        for (String uid : uids) {
            if (idsString.length() != 0) {
                idsString += ",";
            }
            idsString += "'" + uid + "'";
        }
        Cursor cursor = this.getReadableDatabase().query(DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
                        KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
                        KEY_EMAIL},
                KEY_UID + " IN ("+idsString+")", null, null, null, KEY_FAMILYNAME + "," + KEY_GIVENNAME);

        ArrayList<Patient> patients = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Patient patient = new Patient(cursor);
            patients.add(patient);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return patients;
    }

    /**
     * Retrieves specific record
     * @param rowId
     * @return cursor
     * @throws SQLException
     */
    public Cursor getRecord(long rowId) throws SQLException {
        Cursor mCursor = this.getReadableDatabase().query(true, DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_TIMESTAMP, KEY_UID, KEY_FAMILYNAME, KEY_GIVENNAME, KEY_BIRTHDATE, KEY_GENDER,
                        KEY_WEIGHT_KG, KEY_HEIGHT_CM, KEY_ZIPCODE, KEY_CITY, KEY_COUNTRY, KEY_ADDRESS, KEY_PHONE,
                        KEY_EMAIL},
                KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    // Update record
    public boolean updateRecord(Patient patient) {
        ContentValues values = patient.toContentValues();
        boolean result = this.getWritableDatabase().update(DATABASE_TABLE, values, KEY_ROWID + "=" + patient.rowId, null) > 0;
        SyncManager.getShared().triggerSync();
        return result;
    }
}
