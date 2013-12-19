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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
		
	private static String TAG = "DataBaseHelper";	// Tag for LogCat window
	private static String DB_PATH = "";
	private static String DB_NAME = "amiko_db_full_idx_de.db";
	private static int DB_VERSION = 127;	// Database (1.2.7), AmiKo Release 1.2.0 (06/12/2013)
	
	private SQLiteDatabase mDataBase;		
	private final Context mContext;
	
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
	public DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
		this.mContext = context;
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
				throw new Error("Error copying database");
			}	
		}
	}
	
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */		
	private boolean checkDataBase() {
		File dbFile = new File(DB_PATH + DB_NAME);
		return dbFile.exists();
	}
	
    /**
     * Copies database from local assets-folder to just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transferring bytestream.
     * */
	private void copyDataBase() throws IOException {
		InputStream mInput = mContext.getAssets().open(DB_NAME);		
		String outFileName = DB_PATH + DB_NAME;
		OutputStream mOutput = new FileOutputStream(outFileName);
		
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
	 * Opens SQLite database in read-only mode
	 * @return true if operation is successful, false otherwise
	 * @throws SQLException
	 */
	public boolean openDataBase() throws SQLException {
		String mPath = DB_PATH + DB_NAME;
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