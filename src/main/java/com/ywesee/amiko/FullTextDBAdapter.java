package com.ywesee.amiko;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class FullTextDBAdapter extends SQLiteOpenHelper {

    private static final String TAG = "FullTextDBAdapter"; // Tag for LogCat window

    private final Context mContext;
    private boolean databaseIsOpened = false;

    public static final String KEY_ROWID = "id";
    public static final String KEY_KEYWORD = "keyword";
    public static final String KEY_REGNR = "regnr";

    public static final String DATABASE_TABLE = "frequency";

    public static final int DB_VERSION = 1;

    /**
     * Constructor
     * @param context
     */
    public FullTextDBAdapter(Context context) {
        super(context, Constants.appFullTextSearchDatabase(), null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        databaseIsOpened = true;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        super.onOpen(db);
    }

    public void close() {
        super.close();
        databaseIsOpened = false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public ArrayList<Entry> searchKeyword(String searchTerm) {
        // Execute DB raw query
        Cursor cursor = this.getReadableDatabase().rawQuery(
            "SELECT " + KEY_ROWID + "," + KEY_KEYWORD + "," + KEY_REGNR +  " FROM " + DATABASE_TABLE + " WHERE " + KEY_KEYWORD + " LIKE ?" ,
            new String[]{searchTerm + "%"});

        // Iterate through cursor to extract required info
        ArrayList<Entry> results = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Entry entry = new Entry(cursor);
            results.add(entry);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return results;
    }

    public Entry searchHash(String searchTerm) {
        // Execute DB raw query
        Cursor cursor = this.getReadableDatabase().rawQuery(
            "SELECT " + KEY_ROWID + "," + KEY_KEYWORD + "," + KEY_REGNR +  " FROM " + DATABASE_TABLE + " WHERE " + KEY_ROWID + " LIKE ?" ,
            new String[]{searchTerm});

        Entry result = null;
        if (cursor.moveToFirst()) {
            result = new Entry(cursor);
        }
        cursor.close();
        return result;
    }

    public class Entry {
        String hash;
        String keyword;
        String regnrs;
        HashMap<String, ArrayList<String>> regChaptersDict;

        Entry(Cursor cursor) {
            this.hash = cursor.getString(0);
            this.keyword = cursor.getString(1);
            this.setRegnrs(cursor.getString(2));
        }

        void setRegnrs(String input) {
            this.regnrs = input;
            HashMap<String, ArrayList<String>> hashmap = new HashMap<>();
            String pairs[] = input.split("\\|");
            for (String pair : pairs) {
                String parts[] = pair.split("\\(");
                String regnr = parts[0];

                ArrayList<String> existingChapters = hashmap.get(regnr);
                if (existingChapters == null) {
                    existingChapters = new ArrayList<String>();
                    hashmap.put(regnr, existingChapters);
                }

                if (parts.length > 1) {
                    String chapters[] = parts[1].replace(")", "").split(",");
                    for (String s : chapters) {
                        existingChapters.add(s);
                    }
                }
            }
            this.regChaptersDict = hashmap;
        }

        String getRegnrs() {
            return String.join(",", this.getRegnrsArray());
        }

        ArrayList<String> getChaptersForKey(String regnr) {
            return this.regChaptersDict.get(regnr);
        }

        ArrayList<String> getRegnrsArray() {
            return new ArrayList<>(regChaptersDict.keySet());
        }

        int numHits() {
            return regChaptersDict.size();
        }
    }
}
