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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.TreeMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.ywesee.amiko.MainActivity.AMIKO_PREFS_FILE;
import static com.ywesee.amiko.MainActivity.PREF_DB_UPDATE_DATE_DE;
import static com.ywesee.amiko.MainActivity.PREF_DB_UPDATE_DATE_FR;

public class DataBaseHelper extends SQLiteOpenHelper {

	private static String TAG = "DataBaseHelper";	// Tag for LogCat window

	private static String mMainDBName = "";
	private static String mFullTextDBName = "";
	private static String mReportName = "";
	private static String mInteractionsName = "";
	private static String mAppDataDir = "";

	private SQLiteDatabase mDataBase;
	private Observer mObserver;

	private final Context mContext;

		/**
		 * Constructor
		 * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
		 * @param context
		 */
	public DataBaseHelper(Context context) {
		super(context, Constants.appDatabase(), null, Constants.DB_VERSION);
		this.mContext = context;
		// Initialize all databases and related files
		mMainDBName = Constants.appDatabase();
		mFullTextDBName = Constants.appFullTextSearchDatabase();
		mReportName = Constants.appReportFile();
		mInteractionsName = Constants.appInteractionsFile();
		// Initialize persistant storage where databases will go
		Log.d(TAG, "Android version = " + android.os.Build.VERSION.SDK_INT);
		mAppDataDir = context.getApplicationInfo().dataDir + "/databases/";
		File db_path = new File(mAppDataDir);
		if (!db_path.exists())
			db_path.mkdir();
	}

	/**
	 * Adds observer
	 * @param observer
	 */
	public void addObserver(Observer observer) {
		mObserver = observer;
	}

	/**
	 * Notifies observer
	 * @param totBytes: total downloaded bytes
	 */
	public void notifyObserver(String fileName, int totBytesRead, int totBytes) {
		List<Integer> args = new ArrayList<Integer>();
		if (fileName.startsWith("amiko_db_full"))
			args.add(1);
		else if (fileName.startsWith("drug_interactions_csv"))
			args.add(2);
		else if (fileName.startsWith("amiko_frequency"))
			args.add(3);

		args.add(totBytesRead);
		args.add(totBytes);

		mObserver.update(null, args);
	}

		/**
		 * Check if file already exist to avoid re-copying it each time when application starts.
		 * @return true if it exists, false if it doesn't
		 */
	static private boolean checkFileExistsAtPath(String fileName, String path) {
		File dbFile = new File(path + fileName);
		return dbFile.exists();
	}

	public static boolean checkAllFilesExists() {
		return checkFileExistsAtPath(mMainDBName, mAppDataDir) &&
				checkFileExistsAtPath(mFullTextDBName, mAppDataDir) &&
				checkFileExistsAtPath(mReportName, mAppDataDir) &&
				checkFileExistsAtPath(mInteractionsName, mAppDataDir);
	}

	static public boolean isBuildDateAfterLastUpdate(Context context) {
		SharedPreferences settings = context.getSharedPreferences(AMIKO_PREFS_FILE, 0);
		long timeMillisSince1970 = 0;
		if (Constants.appLanguage().equals("de")) {
			timeMillisSince1970 = settings.getLong(PREF_DB_UPDATE_DATE_DE, 0);
		} else {
			timeMillisSince1970 = settings.getLong(PREF_DB_UPDATE_DATE_FR, 0);
		}
		Date lastUpdate = new Date(timeMillisSince1970);
		Date apkBuildDate = new Date(BuildConfig.TIMESTAMP);
		return apkBuildDate.after(lastUpdate);
	}

	static public boolean shouldCopyFromPersistentFolder(Context context) {
		boolean shouldOverride = isBuildDateAfterLastUpdate(context) || !checkAllFilesExists();
		return shouldOverride;
	}

	/**
		 * Creates a set of empty databases (if there are more than one) and rewrites them with own databases.
		 */
	public void copyFilesFromNonPersistentFolder() throws Exception {
		boolean shouldOverride = shouldCopyFromPersistentFolder(mContext);
		if (shouldOverride) {
			/*
			this.getReadableDatabase();
			this.close();
			*/
			// Copy SQLite database from external storage
			try {
				copyFileFromAssetsToPath(mMainDBName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): database created");
			} catch (IOException e) {
				throw new Exception("Error copying main database!" + e.getLocalizedMessage());
			}
			try {
				copyFileFromAssetsToPath(mFullTextDBName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): database created");
			} catch (IOException e) {
				throw new Exception("Error copying frequency database!" + e.getLocalizedMessage());
			}
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mReportName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): report file copied");
			} catch (IOException e) {
				throw new Exception("Error copying report file!" + e.getLocalizedMessage());
			}
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mInteractionsName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): drug interactions file copied");
			} catch (IOException e) {
				throw new Exception("Error copying drug interactions file!" + e.getLocalizedMessage());
			}
		}

		if (isBuildDateAfterLastUpdate(mContext)) {
			Date apkBuildDate = new Date(BuildConfig.TIMESTAMP);
			SharedPreferences settings = mContext.getSharedPreferences(AMIKO_PREFS_FILE, 0);
			SharedPreferences.Editor editor = settings.edit();
			if (Constants.appLanguage().equals("de")) {
				editor.putLong(PREF_DB_UPDATE_DATE_DE, apkBuildDate.getTime());
			} else if (Constants.appLanguage().equals("fr")) {
				editor.putLong(PREF_DB_UPDATE_DATE_FR, apkBuildDate.getTime());
			}
			// Commit the edits!
			editor.commit();
		}
	}

	/**
		 * Copies file from local assets-folder to system folder (persistant storage),
		 * from where it can be accessed and handled. This is done by transferring bytestream.
	 * @param srcFile
	 * @param dstPath
	 */
	private void copyFileFromAssetsToPath(String srcFile, String dstPath) throws IOException {
		Log.d(TAG, "Copying file " + srcFile + " to " + dstPath);

		// Open shipped database from assets folder
		InputStream mInput = mContext.getAssets().open(srcFile);
		OutputStream mOutput = new FileOutputStream(dstPath + srcFile);

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
	 * Utility function: reads csv file as formatted by EPha.ch
	 * @param filename
	 * @return
	 */
	private Map<String,String> readFromCsvToMap(String filename) {
		Map<String, String> map = new TreeMap<String, String>();
		try {
			File file = new File(filename);
			if (!file.exists())
				return null;
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				String token[] = line.split("\\|\\|");
				map.put(token[0] + "-" + token[1], token[2]);
			}
			br.close();
		} catch (Exception e) {
			System.err.println(">> Error in reading csv file");
		}

		return map;
	}

	/**
	 * Opens SQLite database in read-only mode
	 * @return true if operation is successful, false otherwise
	 * @throws SQLException
	 */
	public boolean openDataBase() throws SQLException {
		String mPath = mAppDataDir + mMainDBName;
		File db_path = new File(mPath);
		if (!db_path.exists())
			return false;
		try {
			Log.d(TAG, "opening database " + mPath + "...");
			mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLException sqle ) {
			Log.e(TAG, "open >> " + mPath + " / " + sqle.toString());
			throw sqle;
		}
		return mDataBase != null;
	}

	/**
	 * Opens drug interactions csv
	 */
	public Map<String,String> openInteractionsFile() {
		String mPath = mAppDataDir + mInteractionsName;
		return readFromCsvToMap( mPath );
	}

	/**
	 * Closes SQLite database
	 */
	@Override
	public synchronized void close() {
		if (mDataBase != null)
			mDataBase.close();
		super.close();
	}

	/**
	 * Called when database is created for the first time
	 */
	@Override
	public void onCreate(SQLiteDatabase db)	{
		super.onOpen(db);
		db.disableWriteAheadLogging();
		/*
		try {
					db.execSQL("DROP TABLE IF EXISTS amikodb;");
					db.execSQL("CREATE TABLE amikodb (title, auth, atc, substances, style, content);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		*/
	}

	/**
	 * Called if database version is decreased
		 Note: override on downgrade, default will throw exception, which is bad.
	 */
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion < oldVersion) {
			if (Constants.DEBUG)
				Log.d(TAG, "onDowngrade(): downgrading database from version " + oldVersion + " to version " + newVersion);
		}
	}

	/**
	 * Called if database version is increased
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < newVersion) {
			if (Constants.DEBUG)
				Log.d(TAG, "onUpgrade(): upgrading database from version " + oldVersion + " to version " + newVersion);
			/*
			try {
				copyFileFromAssetsToPath(mDBName, mDBPath + mDBName);
			} catch (IOException e) {
				throw new Exception("Error upgrading database from version " + oldVersion + " to version " + newVersion);
				}
				*/
		}
	}
}
