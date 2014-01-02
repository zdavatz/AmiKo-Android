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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
		
	private static String TAG = "DataBaseHelper";	// Tag for LogCat window
	private static String DB_NAME = "amiko_db_full_idx_de.db";
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
		super(context, DB_NAME, null, Constants.DB_VERSION);
		mDBPath = context.getApplicationInfo().dataDir + "/databases/";
		this.mContext = context;
	}
	
	/**
	 * 
	 */
	public void addObserver(Observer observer) {
		mObserver = observer;
	}
	
	/**
	 * 
	 */
	public void notifyObserver(long totBytes) {
		mObserver.update(null, totBytes);
	}
	
	/**
     * Creates a empty database on the system and rewrites it with your own database.
     */	
	public void createDataBase() throws IOException {
		// If database does not exist, copy it from assets folder		
		boolean dbExist = checkDataBase();

		if (!dbExist) {
		// if (dbExist) {  // Used only for debugging purposes!
			this.getReadableDatabase();
			this.close();
			try {
				// Copy database from assets
				copyDataBase();
				if (Constants.DEBUG)
					Log.d(TAG, "createDataBase(): database created");
			} catch (IOException e) {
				throw new Error("Error copying database!");
			}	
		}
	}
	
	/**
	 * Overwrite database
	 */
	public void overwriteDataBase(String srcFile) throws IOException {
		getReadableDatabase();
		// Close database
		close();
		try {
			// Copy database from src to dst
			copyDataBase(srcFile, true);
			if (Constants.DEBUG)
				Log.d(TAG, "overwriteDataBase(): old database overwritten");
		} catch (IOException e) {
			throw new Error("Error overwriting database!");
		}
	}	
	
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */		
	private boolean checkDataBase() {
		File dbFile = new File(mDBPath + DB_NAME);
		return dbFile.exists();
	}
	
    /**
     * Copies database from local assets-folder to just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transferring bytestream.
     * */
	private void copyDataBase() throws IOException {
		// Open shipped database from assets folder
		InputStream mInput = mContext.getAssets().open(DB_NAME);
		String dbFileName = mDBPath + DB_NAME;
		OutputStream mOutput = new FileOutputStream(dbFileName);
		
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
	
	private void copyDataBase(String srcFile, boolean zipped) throws IOException {
		if (!zipped) {
			// Open database
			InputStream mInput = new FileInputStream(srcFile);
			String dbFileName = mDBPath + DB_NAME;
			OutputStream mOutput = new FileOutputStream(dbFileName);
			
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
					FileOutputStream fout = new FileOutputStream(mDBPath + DB_NAME);
					long totBytesRead = 0;

					while ((bytesRead = zis.read(buffer)) != -1) {
						fout.write(buffer, 0, bytesRead);
						totBytesRead += bytesRead;
						notifyObserver(totBytesRead);
					}
					
					Log.d(TAG, "Unzipped file " + ze.getName() + "(" + totBytesRead/1000 + "kB)");
					
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
	 * Implements chmod using reflection pattern
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
	 * Opens SQLite database in read-only mode
	 * @return true if operation is successful, false otherwise
	 * @throws SQLException
	 */
	public boolean openDataBase() throws SQLException {
		String mPath = mDBPath + DB_NAME;
		try {
			mDataBase = SQLiteDatabase.openDatabase(mPath,  null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLException sqle ) {
			Log.e(TAG, "open >> " + mPath + " / " + sqle.toString());
			throw sqle;
		}
		return mDataBase != null;
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
	 * Called if database version is increased
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < newVersion) {
			if (Constants.DEBUG)
				Log.d(TAG, "onUpgrade(): upgrading database from version " + oldVersion + " to version " + newVersion);
			try { 
				copyDataBase(); 
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