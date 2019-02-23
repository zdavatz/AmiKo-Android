/*
Copyright (c) 2013 Max Lungarella

This file is part of AmiKo for Android.

AmiKo for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.ywesee.amiko;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class DBAdapter {

	private static final String TAG = "DBAdapter"; // Tag for LogCat window

	private final Context mContext;
	private SQLiteDatabase mDb;
	private Map<String,String> mInteractions = null;
	private DataBaseHelper mDbHelper;
	private int mNumRecords;
	private boolean mDatabaseCreated = false;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_AUTH = "auth";
	public static final String KEY_ATCCODE = "atc";
	public static final String KEY_SUBSTANCES = "substances";
	public static final String KEY_REGNRS = "regnrs";
	public static final String KEY_ATCCLASS = "atc_class";
	public static final String KEY_THERAPY = "tindex_str";
	public static final String KEY_APPLICATION = "application_str";
	public static final String KEY_INDICATIONS = "indications_str";
	public static final String KEY_CUSTOMER_ID = "customer_id";
	public static final String KEY_PACK_INFO = "pack_info_str";
	public static final String KEY_ADDINFO = "add_info_str";
	public static final String KEY_IDS = "ids_str";
	public static final String KEY_SECTIONS = "titles_str";
	public static final String KEY_CONTENT = "content";
	public static final String KEY_STYLE = "style_str";
	public static final String KEY_PACKAGES = "packages";

	private static final String DATABASE_TABLE = "amikodb";

	/**
	 * Table columns used for fast queries
	 */
	private static final String SHORT_TABLE = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
				KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATCCODE, KEY_SUBSTANCES, KEY_REGNRS, KEY_ATCCLASS, KEY_THERAPY,
				KEY_APPLICATION, KEY_INDICATIONS, KEY_CUSTOMER_ID, KEY_PACK_INFO, KEY_PACKAGES);

	// private static final String DATABASE_TABLE = "amikodb_fts";

    /**
     * Constructor
     * @param context
     */
	public DBAdapter(Context context) {
		mContext = context;
		mDbHelper = new DataBaseHelper(mContext);
	}

	/**
	 *
	 */
	public void addObserver(Observer observer) {
		mDbHelper.addObserver(observer);
	}

	public int getSizeSQLiteDatabaseFile() {
		return (int)mDbHelper.getSizeSQLiteDatabaseFile();
	}

	public int getSizeInteractionsFile() {
		return (int)mDbHelper.getSizeInteractionsFile();
	}

	/**
	 *
	 */
	public int getSizeZippedFile(String zipFile) {
		ZipEntry ze = null;
		try {
			// Utilities.chmod(zipFile, 755);
			InputStream is = new FileInputStream(zipFile);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
			ze = zis.getNextEntry();
			zis.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (ze!=null)
			return (int)ze.getSize();	// returns -1 if size is UNKNOWN...
		return 0;
	}

	/**
	 *
	 */
	public int getSizeZippedSQLiteDatabaseFile() {
		return getSizeZippedFile( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ "/" + Constants.appZippedDatabase() );
	}

	/**
	 *
	 */
	public int getSizeZippedInteractionsFile() {
		return getSizeZippedFile( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ "/" + Constants.appZippedInteractionsFile() );
	}

	/**
	 *
	 */
	public void copyReportFile() throws IOException {
		String srcReportFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ "/" + Constants.appReportFile();
		String dstReportFile = mContext.getApplicationInfo().dataDir + "/databases/" + Constants.appReportFile();

		InputStream mInput = new FileInputStream(srcReportFile);
		OutputStream mOutput = new FileOutputStream(dstReportFile);

		// Transfer bytes from input to output
		byte[] mBuffer = new byte[1024];
		int mLength;
		while ((mLength = mInput.read(mBuffer))>0) {
			mOutput.write(mBuffer, 0, mLength);
		}

		// Close streams
		mOutput.flush();
		mOutput.close();
		mInput.close();
	}

	/**
	 * Opens drug interactions file
	 */
	void openInteractionsFile() {
		mInteractions = mDbHelper.openInteractionsFile();
	}

	/**
	 * Returns number of interactions in drug interactions map
	 * @return
	 */
	public int getNumInteractions() {
		return mInteractions.size();
	}

	public boolean checkDatabasesExist() {
		return mDbHelper.checkAllFilesExists();
	}

	/**
	 * Creates database
	 * @throws IOException
	 */
	public void create() throws Exception {
		try {
			if (!mDatabaseCreated) {
				// Copies interactions db and report file from asset folder
				mDbHelper.copyFilesFromNonPersistentFolder();
				mDatabaseCreated = true;
			} else {
				this.closeSQLiteDB();
				overwriteSQLiteDB();
				this.openSQLiteDB();
				overwriteInteractionsFile();
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString() + " Unable to create database");
			throw new Exception("Unable to create database");
		}
	}

	/**
	 * Overwrites old database
	 * @throws IOException
	 */
	public void overwriteSQLiteDB() throws Exception {
		try {
			String zippedFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					+ "/" + Constants.appZippedDatabase();
			// copies and overwrites (if necessary) while unzipping (if necessary)
			mDbHelper.overwriteSQLiteDataBase(zippedFile, getSizeZippedFile(zippedFile));
		} catch (Exception e) {
			Log.e(TAG, e.toString() + " Unable to overwrite database");
			throw new Exception("Unable to overwrite database");
		}
	}

	/**
	 * Overwrites old drug interactions file
	 * @throws IOException
	 */
	public void overwriteInteractionsFile() throws Exception {
		try {
			String zippedFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					+ "/" + Constants.appZippedInteractionsFile();
			// copies and overwrites (if necessary) while unzipping (if necessary)
			mDbHelper.overwriteInteractionsFile(zippedFile, getSizeZippedFile(zippedFile));
		} catch (IOException e) {
			Log.e(TAG, e.toString() + " Unable to overwrite drug interactions file");
			throw new Exception("Unable to overwrite drug interactions file");
		}
	}


	/**
	 * Opens database and initializes number of stored records
	 * @throws SQLException
	 */
	public boolean openSQLiteDB() throws SQLException {
		boolean success = false;
		try {
			success = mDbHelper.openDataBase();
			if (success) {
				mDbHelper.close();
				mDb = mDbHelper.getReadableDatabase();
				mNumRecords = getNumRecords();
			}
		} catch (SQLException sqle) {
			Log.e(TAG, "openSQLiteDB >> " + sqle.toString() + " with " + mNumRecords + " entries");
			throw sqle;
		}
		return success;
	}

	/**
	 * Closes the database
	 */
	public void closeSQLiteDB() {
		mDbHelper.close();
	}

	/**
	 * Queries the number of records in the database
	 * @return number of records
	 */
	public int getNumRecords() {
		String query = "select count(*) from " + DATABASE_TABLE;
		// rawQuery often throws an exception!!
		try {
			Cursor mCursor = mDb.rawQuery(query, null);
			mCursor.moveToNext();
			return mCursor.getInt(0);
		} catch (Exception e) {
			Log.e(TAG, "DBAdapter: exception thrown while getting number of records in database.");
			return 0;
		}
	}

	/**
	 *
	 * @param title
	 * @param auth
	 * @param atccode
	 * @param substances
	 * @param regnrs
	 * @param atcclass
	 * @param tindex_str
	 * @param application_str
	 * @param indications_str
	 * @param customer_id
	 * @param ids_str
	 * @param titles_str
	 * @param content
	 * @return
	 */
	public long insertRecord(String title, String auth, String atccode, String substances,
			String regnrs, String atcclass,	String tindex_str, String application_str, String indications_str,
			int customer_id, String pack_info_str, String add_info_str, String ids_str, String titles_str,
			String content, String style_str) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TITLE, title);
		initialValues.put(KEY_AUTH, auth);
		initialValues.put(KEY_ATCCODE, atccode);
		initialValues.put(KEY_SUBSTANCES, substances);
		initialValues.put(KEY_REGNRS, regnrs);
		initialValues.put(KEY_ATCCLASS, atcclass);
		initialValues.put(KEY_THERAPY, tindex_str);
		initialValues.put(KEY_APPLICATION, application_str);
		initialValues.put(KEY_INDICATIONS, indications_str);
		initialValues.put(KEY_CUSTOMER_ID, customer_id);
		initialValues.put(KEY_PACK_INFO, pack_info_str);
		initialValues.put(KEY_ADDINFO, add_info_str);
		initialValues.put(KEY_IDS, ids_str);
		initialValues.put(KEY_SECTIONS, titles_str);
		initialValues.put(KEY_CONTENT, content);
		initialValues.put(KEY_STYLE, style_str);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Deletes specific record from database
	 * @param rowId
	 * @return
	 */
	public boolean deleteRecord(long rowId) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Retrieves all records in database
	 * @return full list of entries
	 */
	public List<Medication> getAllRecords() {
		List<Medication> medis = new ArrayList<Medication>();

		Cursor cursor = mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATCCODE, KEY_SUBSTANCES, KEY_REGNRS,
				KEY_ATCCLASS, KEY_THERAPY, KEY_APPLICATION, KEY_INDICATIONS, KEY_CUSTOMER_ID, KEY_PACK_INFO,
				KEY_ADDINFO, KEY_IDS, KEY_SECTIONS, KEY_CONTENT},
				null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Medication medi = cursorToShortMedi(cursor);
			medis.add(medi);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return medis;
	}

	/**
	 * Executes search query. NOTE: increases readability of the code, HOWEVER slows down execution speed!
	 * @param q: search query
	 * @param m: list of medications returned by query
	 */
	public void searchQuery(String q, List<Medication> m) {
		// Execute DB raw query
		Cursor mCursor = mDb.rawQuery(q, null);

		// Iterate through cursor to extract required info
		mCursor.moveToFirst();
		while (!mCursor.isAfterLast()) {
			Medication medi = cursorToShortMedi(mCursor);
			m.add(medi);
			mCursor.moveToNext();
		}
		// Make sure to close the cursor
		mCursor.close();
		if (Constants.DEBUG)
			Log.d(TAG, q);
	}

	/**
	 * Executes search query on column "title"
	 * @param title
	 * @return list of medications with a specific name/title
	 */
	public List<Medication> searchTitle(String title) {

		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE
				+ " where " + KEY_TITLE + " like " + "'" + title + "%'";
		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Executes search on column "auth"
	 * @param auth
	 * @return list of medications with a specific author
	 */
	public List<Medication> searchAuth(String auth) {
		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE
				+ " where " + KEY_AUTH + " like " + "'" + auth + "%'";
		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Executes search on column "atccode"
	 * @param atccode
	 * @return list of medications with a specific atccode
	 */
	public List<Medication> searchATC(String atccode) {
		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
				+ KEY_ATCCODE + " like " + "'%;" + atccode + "%' or "
				+ KEY_ATCCODE + " like " + "'" + atccode + "%' or "
				+ KEY_ATCCODE + " like " + "'% " + atccode + "%' or "
				+ KEY_ATCCLASS + " like " + "'" + atccode + "%' or "
				+ KEY_ATCCLASS + " like " + "'%;" + atccode + "%' or "
				+ KEY_ATCCLASS + " like " + "'%#" + atccode + "%' or "
				+ KEY_SUBSTANCES + " like " + "'%, " + atccode + "%' or "
				+ KEY_SUBSTANCES + " like " + "'" + atccode + "%'";

		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Executes search on column "substance"
	 * @param substance
	 * @return list of medications containing a specific substance
	 */
	public List<Medication> searchSubstance(String substance) {
		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
				+ KEY_SUBSTANCES + " like " + "'%, " + substance + "%' or "
				+ KEY_SUBSTANCES + " like " + "'" + substance + "%'";
		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Executes search on column "regnrs"
	 * @param regnr
	 * @return list of medication with a specific Swissmedic registration number
	 */
	public List<Medication> searchRegnr(String regnr) {
		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
				+ KEY_REGNRS + " like " + "'%, " + regnr + "%' or "
				+ KEY_REGNRS + " like " + "'" + regnr + "%'";
		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Executes search on column "therapy"
	 * @param therapy
	 * @return
	 */
	public List<Medication> searchTherapy(String therapy) {
		List<Medication> medis = new ArrayList<Medication>();
		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
				+ KEY_THERAPY + " like " + "'%," + therapy + "%' or "
				+ KEY_THERAPY + " like " + "'" + therapy + "%'";
		searchQuery(query, medis);
		return medis;
	}

	/**
	 *
	 * @param application
	 * @return
	 */
	public List<Medication> searchApplication(String application) {
		List<Medication> medis = new ArrayList<Medication>();

		String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
				+ KEY_APPLICATION + " like " + "'%," + application + "%' or "
				+ KEY_APPLICATION + " like " + "'" + application + "%' or "
				+ KEY_APPLICATION + " like " + "'% " + application + "%' or "
				+ KEY_APPLICATION + " like " + "'%;" + application +"%' or "
				+ KEY_INDICATIONS + " like " + "'" + application + "%' or "
				+ KEY_INDICATIONS + " like " + "'%;" + application + "%'";
		searchQuery(query, medis);

		return medis;
	}

	/**
	 * Retrieves database entry based on id
	 * @param rowId
	 * @return database entry
	 */
	public Medication searchId(long rowId) {
		if (rowId>=0)
			return cursorToMedi(getRecord(rowId));
		else
			return null;
	}

	/**
	 * Retrieves specific record
	 * @param rowId
	 * @return cursor
	 * @throws SQLException
	 */
	public Cursor getRecord(long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATCCODE, KEY_SUBSTANCES, KEY_REGNRS,
					KEY_ATCCLASS, KEY_THERAPY, KEY_APPLICATION, KEY_INDICATIONS, KEY_CUSTOMER_ID,
					KEY_PACK_INFO, KEY_PACKAGES, KEY_ADDINFO, KEY_IDS, KEY_SECTIONS, KEY_CONTENT, KEY_STYLE},
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Maps cursor to medication (short version, fast)
	 * @param cursor
	 * @return
	 */
	private Medication cursorToShortMedi(Cursor cursor) {
		Medication medi = new Medication();

		medi.setId(cursor.getLong(0));
		medi.setTitle(cursor.getString(1));
		medi.setAuth(cursor.getString(2));
		medi.setAtcCode(cursor.getString(3));
		medi.setSubstances(cursor.getString(4));
		medi.setRegnrs(cursor.getString(5));
		medi.setAtcClass(cursor.getString(6));
		medi.setTherapy(cursor.getString(7));
		medi.setApplication(cursor.getString(8));
		medi.setIndications(cursor.getString(9));
		medi.setCustomerId(cursor.getInt(10));
		medi.setPackInfo(cursor.getString(11));
		medi.setPackages(cursor.getString(12));
		return medi;
	}

	/**
	 * Maps cursor to medication (long version, slow)
	 * @param cursor
	 * @return
	 */
	private Medication cursorToMedi(Cursor cursor) {
		Medication medi = new Medication();

		medi.setId(cursor.getLong(0));
		medi.setTitle(cursor.getString(1));
		medi.setAuth(cursor.getString(2));
		medi.setAtcCode(cursor.getString(3));
		medi.setSubstances(cursor.getString(4));
		medi.setRegnrs(cursor.getString(5));
		medi.setAtcClass(cursor.getString(6));
		medi.setTherapy(cursor.getString(7));
		medi.setApplication(cursor.getString(8));
		medi.setIndications(cursor.getString(9));
		medi.setCustomerId(cursor.getInt(10));
		medi.setPackInfo(cursor.getString(11));
		medi.setPackages(cursor.getString(12));
		medi.setAddInfo(cursor.getString(13));
		medi.setSectionIds(cursor.getString(14));
		medi.setSectionTitles(cursor.getString(15));
		medi.setContent(cursor.getString(16));
		medi.setStyle(cursor.getString(17));

		return medi;
	}

	// Update record
	public boolean updateRecord(long rowId, String title, String auth, String atccode, String substances,
			String regnrs, String atcclass, String therapy_str, String application_str, String indications_str,
			int customer_id, String pack_info_str, String packages_str, String add_info_str, String ids_str, String titles_str,
			String content, String style_str) {
		ContentValues args = new ContentValues();

		args.put(KEY_TITLE, title);
		args.put(KEY_AUTH, auth);
		args.put(KEY_ATCCODE, atccode);
		args.put(KEY_SUBSTANCES, substances);
		args.put(KEY_REGNRS, regnrs);
		args.put(KEY_ATCCLASS, atcclass);
		args.put(KEY_THERAPY, therapy_str);
		args.put(KEY_APPLICATION, application_str);
		args.put(KEY_INDICATIONS, indications_str);
		args.put(KEY_CUSTOMER_ID, customer_id);
		args.put(KEY_PACK_INFO, pack_info_str);
		args.put(KEY_PACKAGES, packages_str);
		args.put(KEY_ADDINFO, add_info_str);
		args.put(KEY_IDS, ids_str);
		args.put(KEY_SECTIONS, titles_str);
		args.put(KEY_CONTENT, content);
		args.put(KEY_STYLE, style_str);

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
