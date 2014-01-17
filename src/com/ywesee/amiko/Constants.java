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

public class Constants {
	public static final boolean DEBUG = false;
	
	public static final String AMIKO_NAME = "AmiKo";
	public static final String COMED_NAME = "CoMed";
	
	// --> Note: uncomment name of app to compile!
	public static final String APP_NAME = AMIKO_NAME;
	// public static final String APP_NAME = COMED_NAME;	
	public static final String APP_VERSION = "1.2.1";
	public static final String GEN_DATE = "16.01.2014";
	public static final int DB_VERSION = 128; 	
	
	/** Release history
	 *  25/03/2013 - AmiKo/CoMed Release = 0.9.0, Database = 090
	 *  08/04/2013 - AmiKo/CoMed Release = 1.1.0, Database = 110
	 *  16/04/2013 - AmiKo/CoMed Release = 1.1.1, Database = 111
	 *  22/04/2013 - AmiKo/CoMed Release = 1.1.2, Database = 112
	 *  02/05/2013 - AmiKo/CoMed Release = 1.1.4, Database = 114
	 *  06/05/2013 - AmiKo/CoMed Release = 1.1.5, Database = 115
     *	12/01/2014 - AmiKo/CoMed Release = 1.2.0, Database = 127
     *  16/01/2014 - AmiKo/CoMed Release = 1.2.1, Database = 128
	*/
	
	/**
	 * Returns the language of the app
	 * @return
	 */
	public static String appLanguage() {
		if (APP_NAME.equals(AMIKO_NAME)) {
			return "de";
		} else if (APP_NAME.equals(COMED_NAME)) {
			return "fr";
		}
		return "";
	}
	
	/**
	 * Returns the database used in the app
	 * @return
	 */
	public static String appDatabase() {
		if (appLanguage().equals("de"))
			return "amiko_db_full_idx_de.db";
		else if (appLanguage().equals("fr"))
			return "amiko_db_full_idx_fr.db";
		else
			return "amiko_db_full_idx_de.db";
	}
	
	/**
	 * Returns the name of the zipped database
	 * @return
	 */
	public static String appZippedDatabase() {
		if (appLanguage().equals("de"))
			return "amiko_db_full_idx_de.zip";
		else if (appLanguage().equals("fr"))
			return "amiko_db_full_idx_fr.zip";
		else
			return "amiko_db_full_idx_de.zip";
	}
	
	/**
	 * Returns the name of the report file
	 * @return
	 */
	public static String appReportFile() {
		if (appLanguage().equals("de"))
			return "amiko_report_de.html";
		else if (appLanguage().equals("fr"))
			return "amiko_report_fr.html";
		else
			return "amiko_report_de.html";
	}
	
}
