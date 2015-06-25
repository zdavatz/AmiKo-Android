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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
		
	private static String TAG = "DataBaseHelper";	// Tag for LogCat window

	private static String mDBName = "";
	private static String mReportName = "";
	private static String mInteractionsName = "";
	private static String mDBPath = "";

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
		mDBName = Constants.appDatabase();
		mReportName = Constants.appReportFile();
		mInteractionsName = Constants.appInteractionsFile();
		// Initialize persistant storage where databases will go
		mDBPath = context.getApplicationInfo().dataDir + "/databases/";
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
		return (checkFileExistsAtPath(mDBName, mDBPath) && 
				checkFileExistsAtPath(mReportName, mDBPath) &&
				checkFileExistsAtPath(mInteractionsName, mDBPath));
	}
	
	/**
     * Creates a set of empty databases (if there are more than one) and rewrites them with own databases.
     */	
	public void copyFilesFromAssetFolder() throws IOException {
		// If database does not exist, copy it from assets folder		
		if (!checkFileExistsAtPath(mDBName, mDBPath)) {
		// if (checkFileExistsAtPath(mDBName, mDBPath)) {  // Used only for debugging purposes!
			this.getReadableDatabase();
			this.close();
			try {
				// Copy SQLite database from assets
				copyFileFromAssetsToPath(mDBName, mDBPath);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): database created");
			} catch (IOException e) {
				throw new Error("Error copying database!");
			}
		}
		if (!checkFileExistsAtPath(mReportName, mDBPath)) {
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mReportName, mDBPath);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): report file copied");
			} catch (IOException e) {
				throw new Error("Error copying report file!");
			}
		}
		if (!checkFileExistsAtPath(mInteractionsName, mDBPath)) {
			try {
				// Copy report file from assets
				copyFileFromAssetsToPath(mInteractionsName, mDBPath);
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): drug interactions file copied");
			} catch (IOException e) {
				throw new Error("Error copying drug interactions file!");
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
		// Open shipped database from assets folder
		InputStream mInput = mContext.getAssets().open(srcFile);
		OutputStream mOutput = new FileOutputStream(dstPath+srcFile);
		
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
				// Chmod src file
				chmod(srcFile, 755);
				// 
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
	 * Utility function: implements chmod using reflection pattern
	 * @param path
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	private int chmod(String path, int mode) throws Exception {
		Class<?> fileUtils = Class.forName("android.os.FileUtils");
		Method setPermissions = 
				fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
		return (Integer) setPermissions.invoke(null, path, mode, -1, -1);
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
		String mPath = mDBPath + mDBName;
		try {
			mDataBase = SQLiteDatabase.openDatabase(mPath,  null, SQLiteDatabase.OPEN_READONLY);
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
		String mPath = mDBPath + mInteractionsName;
		return readFromCsvToMap( mPath );
	}
	
	/**
	 * Overwrite database
	 */
	public void overwriteSQLiteDataBase(String srcFile, int fileSize) throws IOException {
		getReadableDatabase();
		// Close database
		close();
		try {
        	if (fileSize<0)
        		fileSize = Constants.SQLITE_DB_SIZE;
			// Copy database from src to dst and unzip it!
			copyFileFromSrcToPath(srcFile, mDBPath + mDBName, fileSize, true);
			if (Constants.DEBUG)
				Log.d(TAG, "overwriteDataBase(): old database overwritten");
		} catch (IOException e) {
			throw new Error("Error overwriting database!");
		}
	}	
	
	/**
	 * Overwrite drug interactions file
	 */
	public void overwriteInteractionsFile(String srcFile, int fileSize) throws IOException {
		try {
        	if (fileSize<0)
        		fileSize = Constants.INTERACTIONS_FILE_SIZE;			
			// Copy database from src to dst and unzip it!
			copyFileFromSrcToPath(srcFile, mDBPath + mInteractionsName, fileSize, true);
			if (Constants.DEBUG)
				Log.d(TAG, "overwriteDataBase(): old drug interactions file overwritten");
		} catch (IOException e) {
			throw new Error("Error overwriting drug interactions file!");
		}
	}
	
	/**
	 * 
	 */
	public long getSizeSQLiteDatabaseFile() {
		if (checkFileExistsAtPath(mDBName, mDBPath)) {
			File file = new File(mDBPath + mDBName);
			return file.length();
		}
		return 0;
	}	

	public long getSizeInteractionsFile() {
		if (checkFileExistsAtPath(mInteractionsName, mDBPath)) {
			File file = new File(mDBPath + mInteractionsName);
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
			try { 
				copyFileFromAssetsToPath(mDBName, mDBPath + mDBName);
			} catch (IOException e) { 
				throw new Error("Error upgrading database");
		    } 
		} 
	}		
		
	/**
	 * Called when database is created for the first time
	 */
	@Override
	public void onCreate(SQLiteDatabase db)	{
		/*
		try {
	        db.execSQL("DROP TABLE IF EXISTS amikodb;");
	        db.execSQL("CREATE TABLE amikodb (title, auth, atc, substances, style, content);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		*/
	}	
}