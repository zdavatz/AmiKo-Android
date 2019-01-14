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

import com.ywesee.amiko.ProductFlavour;

public class Constants {

	public static final boolean DEBUG = true;

	public static final int SQLITE_DB_SIZE = 160000000;
	public static final int INTERACTIONS_FILE_SIZE = 10000000;

	public static final String APP_VERSION = "1.3.9";
	public static final String GEN_DATE = "01.11.2015";
	public static final int DB_VERSION = 140;

	/** Release history
	 *  25/03/2013 - AmiKo/CoMed Release = 0.9.0, Database = 090
	 *  08/04/2013 - AmiKo/CoMed Release = 1.1.0, Database = 110
	 *  16/04/2013 - AmiKo/CoMed Release = 1.1.1, Database = 111
	 *  22/04/2013 - AmiKo/CoMed Release = 1.1.2, Database = 112
	 *  02/05/2013 - AmiKo/CoMed Release = 1.1.4, Database = 114
	 *  06/05/2013 - AmiKo/CoMed Release = 1.1.5, Database = 115
     *	12/01/2014 - AmiKo/CoMed Release = 1.2.0, Database = 127
     *  16/01/2014 - AmiKo/CoMed Release = 1.2.1, Database = 128
     *  19/01/2014 - AmiKo/CoMed Release = 1.2.2, Database = 128
     *  20/01/2014 - AmiKo/CoMed Release = 1.2.3, Database = 128
     *  14/02/2014 - AmiKo/CoMed Release = 1.2.4, Database = 128
     *  25/02/2014 - AmiKo/CoMed Release = 1.2.5, Database = 128 (med-drugs release)
     *  29/07/2014 - AmiKo/CoMed Release = 1.2.6, Database = 129 (interactions)
     *  30/07/2014 - AmiKo/CoMed Release = 1.2.7, Database = 129 (interactions)
     *  06/10/2014 - AmiKo/CoMed Release = 1.2.8, Database = 130 (shopping)
     *  30/05/2015 - AmiKo/CoMed Release = 1.3.1, Database = 130
     *  04/07/2015 - AmiKo/CoMed Release = 1.3.2, Database = 140 (main database not shipped, must be downloaded)
     *  02/08/2015 - AmiKo/CoMed Release = 1.3.3, Database = 140 (main database not shipped, must be downloaded)
     *  08/08/2015 - AmiKo/CoMed Release = 1.3.4, Database = 140 (main database not shipped, must be downloaded)
     *  19/08/2015 - AmiKo/CoMed Release = 1.3.7, Database = 140 (main database not shipped, must be downloaded)
	*/

	/**
	 * Returns the language of the app
	 * @return
	 */
	public static String appLanguage() {
      if (ProductFlavour.APP_NAME.equals(ProductFlavour.AMIKO_NAME)) {
			return "de";
      } else if (ProductFlavour.APP_NAME.equals(ProductFlavour.COMED_NAME)) {
			return "fr";
		}
		return "";
	}

	/**
	 * Returns app owner
	 *
	 */
	public static String appOwner() {
			return "ywesee";
	}

	/**
	 * Returns the name SQLite database
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
	 * Returns the name of the zipped SQLite database
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

	/**
	 * Returns the name of the zipped drug interactions file
	 */
	public static String appInteractionsFile() {
		if (appLanguage().equals("de"))
			return "drug_interactions_csv_de.csv";
		else if (appLanguage().equals("fr"))
			return "drug_interactions_csv_fr.csv";
		else
			return "drug_interactions_csv_de.csv";
	}

	/**
	 * Returns the name of the zipped drug interactions file
	 */
	public static String appZippedInteractionsFile() {
		if (appLanguage().equals("de"))
			return "drug_interactions_csv_de.zip";
		else if (appLanguage().equals("fr"))
			return "drug_interactions_csv_fr.zip";
		else
			return "drug_interactions_csv_de.zip";
	}

	public static String appPatientDatabase() {
		return "patients.db";
	}
}
