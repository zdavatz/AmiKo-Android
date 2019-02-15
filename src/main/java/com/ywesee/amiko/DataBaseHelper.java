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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {

	private static String TAG = "DataBaseHelper";	// Tag for LogCat window

	private static String mMainDBName = "";
	private static String mReportName = "";
	private static String mInteractionsName = "";
	private static String mAppDataDir = "";

	private SQLiteDatabase mDataBase;
	private Observer mObserver;

	private final Context mContext;

	public static boolean checkIfDatabaseIsCorrupted(String path) {
		final AtomicBoolean success = new AtomicBoolean(true);
		try {
			Log.d(TAG, "checking database " + path);
			SQLiteDatabase db = SQLiteDatabase.openDatabase(
					path,
					null,
					SQLiteDatabase.OPEN_READONLY,
					new DatabaseErrorHandler() {
						@Override
						public void onCorruption(SQLiteDatabase dbObj) {
							success.set(false);
						}
					});
			if (db == null) {
				success.set(false);
			} else {
				try {
					db.close();
				} catch (Exception e){
					/* Ignore */
				}
			}
		} catch (SQLException sqle ) {
			Log.e(TAG, "detected corrupted database: " + path + " : " + sqle.toString());
			success.set(false);
		}
		return !success.get();
	}

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
	public void notifyObserver(String fileName, int totBytes) {
		List<Integer> args = new ArrayList<Integer>();

		args.add(totBytes);
		if (fileName.startsWith("amiko_db_full"))
			args.add(1);
		else if (fileName.startsWith("drug_interactions_csv"))
			args.add(2);

		mObserver.update(null, args);
	}

    /**
     * Check if file already exist to avoid re-copying it each time when application starts.
     * @return true if it exists, false if it doesn't
     */
	private boolean checkFileExistsAtPath(String fileName, String path) {
		File dbFile = new File(path + fileName);
		return dbFile.exists();
	}

	public boolean checkAllFilesExists() {
		return (checkFileExistsAtPath(mMainDBName, mAppDataDir) &&
				checkFileExistsAtPath(mReportName, mAppDataDir) &&
				checkFileExistsAtPath(mInteractionsName, mAppDataDir));
	}

	/**
     * Creates a set of empty databases (if there are more than one) and rewrites them with own databases.
     */
	public void copyFilesFromNonPersistentFolder() throws Exception {
		if (!checkFileExistsAtPath(mMainDBName, mAppDataDir)) {
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
				throw new Exception("Error copying database!");
			}
		}
		if (!checkFileExistsAtPath(mReportName, mAppDataDir)) {
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mReportName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): report file copied");
			} catch (IOException e) {
				throw new Exception("Error copying report file!");
			}
		}
		if (!checkFileExistsAtPath(mInteractionsName, mAppDataDir)) {
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mInteractionsName, mAppDataDir);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): drug interactions file copied");
			} catch (IOException e) {
				throw new Exception("Error copying drug interactions file!");
			}
		}
	}

	/**
	 * Copy file from external storage to system folder (persistant storage),
	 * from where it can be accessed and handled. This is done by transferring a byte stream.
	 */
	private void copyFileFromExternalStorageToPath(String fileName, String srcPath, String dstPath) throws IOException {
		if (mContext!=null && !mContext.getPackageName().isEmpty()) {
			// Check if file exists
			File srcFile = new File(srcPath + "/" + fileName);
			if (srcFile.exists()) {
				InputStream mInput = new FileInputStream(srcFile);
				OutputStream mOutput = new FileOutputStream(dstPath + "/" + fileName);

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

	private void copyFileFromSrcToPath(String srcFile, String dstFile, int totBytes, boolean zipped) throws IOException {
		if (!zipped) {
			// Open database
			InputStream mInput = new FileInputStream(srcFile);
			OutputStream mOutput = new FileOutputStream(dstFile);

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
		} else {
			byte buffer[] = new byte[2048];
			int bytesRead = -1;

			try {
				// Utilities.chmod(srcFile, 755);
				InputStream is = new FileInputStream(srcFile);
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
				ZipEntry ze;

				while ((ze = zis.getNextEntry()) != null) {
					FileOutputStream fout = new FileOutputStream(dstFile);
					int totBytesRead = 0;	// @Max (03/01/2014) -> used to be 'long'!!

					while ((bytesRead = zis.read(buffer)) != -1) {
						fout.write(buffer, 0, bytesRead);
						totBytesRead += bytesRead;
						notifyObserver(ze.getName(), (int)(100*(float)totBytesRead/(float)totBytes));
					}

					Log.d(TAG, "Unzipped file " + ze.getName() + " (" + totBytesRead/1000 + "kB)");

					fout.close();
					zis.closeEntry();
				}

				zis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
	 * Overwrite database
	 */
	public void overwriteSQLiteDataBase(String srcFile, int fileSize) throws Exception {
		/*
		this.getReadableDatabase();
		this.close();
		*/
		try {
        	if (fileSize<0) {
				fileSize = Constants.SQLITE_DB_SIZE;
			}
			String dbPath = mAppDataDir + mMainDBName;
        	String tempFilename = mMainDBName + ".temp.db";
        	String tempPath= mAppDataDir + tempFilename;
			// Copy database from src to a temp file, check if it's valid, if yes, override the existing db
			copyFileFromSrcToPath(srcFile, tempPath, fileSize, true);
			boolean corrupted = DataBaseHelper.checkIfDatabaseIsCorrupted(tempPath);
			if (!corrupted) {
				Log.d(TAG, "overwriteDataBase(): database is not corrupted, overwritting");
				copyFileFromSrcToPath(tempPath, dbPath, fileSize, false);
			}
			File file = new File(mAppDataDir, tempFilename);
			file.delete();
			Log.d(TAG, "overwriteDataBase(): deleted temp database file");
			if (corrupted) {
				Log.d(TAG, "overwriteDataBase(): database is corrupted, not overwritting");
				throw new Exception("Corrupted database");
			}
			Log.d(TAG, "overwriteDataBase(): old database overwritten");
		} catch (IOException e) {
			throw new Exception("Error overwriting database: " + e);
		}
	}

	/**
	 * Overwrite drug interactions file
	 */
	public void overwriteInteractionsFile(String srcFile, int fileSize) throws Exception {
		try {
        	if (fileSize<0)
        		fileSize = Constants.INTERACTIONS_FILE_SIZE;
			// Copy database from src to dst and unzip it!
			copyFileFromSrcToPath(srcFile, mAppDataDir + mInteractionsName, fileSize, true);
			if (Constants.DEBUG)
				Log.d(TAG, "overwriteDataBase(): old drug interactions file overwritten");
		} catch (IOException e) {
			throw new Exception("Error overwriting drug interactions file!");
		}
	}

	/**
	 *
	 */
	public long getSizeSQLiteDatabaseFile() {
		if (checkFileExistsAtPath(mMainDBName, mAppDataDir)) {
			File file = new File(mAppDataDir + mMainDBName);
			return file.length();
		}
		return 0;
	}

	public long getSizeInteractionsFile() {
		if (checkFileExistsAtPath(mInteractionsName, mAppDataDir)) {
			File file = new File(mAppDataDir + mInteractionsName);
			return file.length();
		}
		return 0;
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
