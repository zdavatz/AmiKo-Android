package com.ywesee.amiko;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	public static int chmod2(String path, int mode) throws Exception {
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
		} else
			Log.d(TAG, "File " + filePath + "/" + fileName + " does not exists. No need to delete.");
		return true;
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
			return null;
        }
        
		return file_str;	
	}

	public static String currentTimeString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
		return sdf.format(new Date());
	}

	public static Date dateFromTimeString(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
		try {
			return sdf.parse(str);
		} catch (Exception e) { }
		return null;
	}

	public static String timeStringFromDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
		return sdf.format(date);
	}

	public static long foundationHash(String baseString) {
		char[] chars = baseString.toCharArray();
		int len = baseString.length();

		long result = len;
		if (len <= 96)
		{
			int to4 = (len & ~3);
			int end = len;
			int i = 0;
			while (i < to4)
			{
				result = result * 67503105 + chars[i] * 16974593 + chars[i + 1] * 66049 + chars[i + 2] * 257 + chars[i + 3];
				i += 4;
			}


			while (i < end)
				result = result * 257 + chars[i++];
		}
		else
		{
			int end;
			int i = 0;
			end = 29;
			while (i < end)
			{
				result = result * 67503105 + chars[i] * 16974593 + chars[i + 1] * 66049 + chars[i + 2] * 257 + chars[i + 3];
				i += 4;
			}
			int j = ((len / 2) - 16);
			end = ((len / 2) + 15);
			while (j < end)
			{
				result = result * 67503105 + chars[j] * 16974593 + chars[j + 1] * 66049 + chars[j + 2] * 257 + chars[j + 3];
				j += 4;
			}
			int k = (len - 32);
			end = (k + 29);
			while (k < end)
			{
				result = result * 67503105 + chars[k] * 16974593 + chars[k + 1] * 66049 + chars[k + 2] * 257 + chars[k + 3];
				k += 4;
			}
		}
		return (result + (result << (len & 31)));
	}
	public static String foundationHashString(String baseString) {
		return Long.toUnsignedString(foundationHash(baseString));
	}

	public static String sha256(String str) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();
		byte[] hashByte = digest.digest(str.getBytes());
		return bin2hex(hashByte);
	}

	static String bin2hex(byte[] data) {
		StringBuilder hex = new StringBuilder(data.length * 2);
		for (byte b : data)
			hex.append(String.format("%02x", b & 0xFF));
		return hex.toString();
	}

	public static boolean isCharacterNumber(char c) {
		return c >= '0' && c <= '9';
	}

	public static String replaceColoursForNightTheme(String html_str, Context context) {
		int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		switch (currentNightMode) {
			case Configuration.UI_MODE_NIGHT_NO:
				break;
			case Configuration.UI_MODE_NIGHT_YES:
				html_str = html_str.replaceAll("#EEEEEE", "var(--background-color-gray)");
				break;
		}

		switch (currentNightMode) {
			case Configuration.UI_MODE_NIGHT_NO:
				html_str = html_str.replaceAll("var\\(--text-color-normal\\)", "black");
				html_str = html_str.replaceAll("var\\(--background-color-normal\\)", "white");
				html_str = html_str.replaceAll("var\\(--background-color-gray\\)", "eeeeee");
				html_str = html_str.replaceAll("var\\(--lines-color\\)", "E5E7E8");
				break;
			case Configuration.UI_MODE_NIGHT_YES:
				html_str = html_str.replaceAll("var\\(--text-color-normal\\)", "white");
				html_str = html_str.replaceAll("var\\(--background-color-normal\\)", "#333333");
				html_str = html_str.replaceAll("var\\(--background-color-gray\\)", "#444444");
				html_str = html_str.replaceAll("var\\(--lines-color\\)", "orange");
				break;
		}
		return html_str;
	}
}
