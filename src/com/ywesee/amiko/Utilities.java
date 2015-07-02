package com.ywesee.amiko;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;

public class Utilities {

	private static final String TAG = "Utilities";
	
	/**
	 * Check if app runs on phone or tablet
	 * @param context
	 * @return
	 */
	public static boolean isTablet(Context context) {
	    boolean ret = (context.getResources().getConfiguration().screenLayout
	            & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;   
	    return ret;
	}	
	
	/** 
	 * Check if external storage is available for read and write 
	 * @return
	 */
	public static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	/** 
	 * Check if external storage is available to at least read
	 * @return
	 */
	public static boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * Implements chmod using reflection pattern
	 * @param path
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	public static int chmod(String path, int mode) throws Exception {
		Class<?> fileUtils = Class.forName("android.os.FileUtils");
		Method setPermissions = fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
		return (Integer) setPermissions.invoke(null, path, mode, -1, -1);
	}	
	
	/** Get absolute path of apk expansion folder.
	 *  Only main and patch expansion files go in here.
	 */
	public static String expansionFileDir(String package_name) {
		String main_expansion_file_dir = Environment.getExternalStorageDirectory().getAbsolutePath();
		main_expansion_file_dir += "/Android/obb/" + package_name + "/";
		// If path does not exist, create it...
		File main_expansion_file = new File(main_expansion_file_dir);
		if (!main_expansion_file.exists())
			main_expansion_file.mkdir();
		return main_expansion_file_dir;
	}
		
	public static boolean deleteFile(String fileName, File filePath) {	
		File file = new File(filePath, fileName);
		if (file.exists()) {
			boolean ret = file.delete();
			Log.d(TAG, "File " + fileName + " found and deleted. Return code = " + ret);
			return ret;
		} else {
			Log.d(TAG, "File " + filePath + "/" + fileName + " does not exists. No need to delete.");
		}
		return false;
	}	
	
	public static String loadFromAssetsFolder(Context context, String file_name, String encoding) {
		String file_str = "";		
        try {
            InputStream is = context.getAssets().open(file_name); 
            InputStreamReader isr = new InputStreamReader(is, encoding);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                file_str += line;
            }
            is.close(); 
        }
        catch (Exception e) {
        	// TODO: Handle exception        	
        }
        
		return file_str;			
	}
	
	public static String loadFromApplicationFolder(Context context, String file_name, String encoding) {
		String file_str = "";
        try {
            InputStream is = new FileInputStream(context.getApplicationInfo().dataDir + "/databases/" + file_name); 
            InputStreamReader isr = new InputStreamReader(is, encoding);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                file_str += line;
            }
            is.close(); 
        }
        catch (Exception e) {
        	// TODO: Handle exception        	
        }
        
		return file_str;			
	}
		
	public static String readFromFile(String filename) {
		String file_str = "";		
        try {
        	FileInputStream fis = new FileInputStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                file_str += (line + "\n");
            }
            br.close();
        }
        catch (Exception e) {
        	System.err.println(">> Error in reading file");        	
        }
        
		return file_str;	
	}
}
