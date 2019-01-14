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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;

public class FavoriteStore {
	
	private String m_dir = "";
	
	public FavoriteStore(String dir) {
		m_dir = dir;
       	File app_data_folder = new File(dir);
       	if (!app_data_folder.exists()) {
       		app_data_folder.mkdirs();		
       		System.out.println("Created application data folder in " + app_data_folder);
       	} else
       		System.out.println("Found application data folder is in " + app_data_folder);
       	// Check if favorites.txt exists, otherwise create file
		File wfile = new File(dir + "\\favorites.txt");
		try {
			if (!wfile.exists()) {
				wfile.getParentFile().mkdirs();
				wfile.createNewFile();
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HashSet<String> load() {
		HashSet<String> hs = new HashSet<String>();
		try {
			FileInputStream file_in = new FileInputStream(m_dir + "\\favorites.txt");
			// Make sure there is something to read...
			if (file_in.available()>0) {
				ObjectInputStream in = new ObjectInputStream(file_in);
				hs = (HashSet<String>)in.readObject();
				in.close();
			}
			file_in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return hs;
	}
	
	public void save(HashSet<String> hs) {
		try {
			FileOutputStream file_out = new FileOutputStream(m_dir + "\\favorites.txt");
	        ObjectOutputStream out = new ObjectOutputStream(file_out);
	        out.writeObject(hs);
	        out.close();
	        file_out.close();
	        // System.out.println("Serialized data is saved in " + m_dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
