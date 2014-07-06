package com.ywesee.amiko;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

public class Utilities {
		
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
