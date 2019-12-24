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

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
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
		return this.load("favorites.txt");
	}

	public void save(HashSet<String> hs) {
		this.save("favorites.txt", hs);
	}

	public HashSet<String> loadFullText() {
		return this.load("favorites-full-text.txt");
	}

	public void saveFullText(HashSet<String> hs) {
		this.save("favorites-full-text.txt", hs);
	}

	public HashSet<String> load(String path) {
		HashSet<String> hs = new HashSet<String>();
		boolean canReadJson = false;
		try {
			FileInputStream inputStream = new FileInputStream(m_dir + "/" + path);
			JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
			reader.beginArray();
			while (reader.hasNext()) {
				hs.add(reader.nextString());
			}
			reader.endArray();
			canReadJson = true;
		} catch (Exception e) {
			canReadJson = false;
		}

		if (!canReadJson) {
			// Fallback to the old format
			try {
				FileInputStream file_in = new FileInputStream(m_dir + "/" + path);
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
		}
		return hs;
	}

	public void save(String path, HashSet<String> hs) {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(m_dir + "/" + path);
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));

			writer.beginArray();
			for (String str : hs) {
				writer.value(str);
			}
			writer.endArray();
			writer.close();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				stream.close();
			} catch (Exception e) { }
		}
	}
}
