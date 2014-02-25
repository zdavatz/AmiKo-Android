1. copy databases to AMiKo assets folder (overwrite old ones)

2. copy amiko_report.html from XmlParse folder to AMiKo (adapt version number first!)

3. in Constants.java select APP_NAME, set APP_VERSION, GEN_DATE and DB_VERSION

4a. in AndroidManifest.xml increase versionName (=APP_VERSION in Constants.java)

if (ywesee)

4b. change android:icon to @drawable/ic_launcher

else if (meddrugs)

4b. change android:icon to @drawable/ic_launcher_med

end

5a. if (AMiKo) in AndroiManifest.xml set package = "com.ywesee.amiko.de"
5b. if (CoMed) in AndroidManifest.xml set package = "com.ywesee.amiko.fr"

6a. if (AMiKo) rename "amiko_db_full_idx_fr.db" to ".amiko_db_full_idx_fr.db"
6b. if (CoMed) rename "amiko_db_full_idx_de.db" to ".amiko_db_full_idx_de.db"

if (ywesee)

7a. if (AMiKo) rename "strings.xml" to ".strings_fr.xml" and ".strings_de.xml" to "strings.xml"
7b. if (CoMed) rename "strings.xml" to ".strings_de.xml" and ".strings_fr.xml" to "strings.xml"

else if (meddrugs)

7a. if (med-drugs) rename "strings.xml" to ".strings_med_fr.xml" and ".strings_med_de.xml" to "strings.xml"
7b. if (med-drugs fr) rename "strings.xml" to ".strings_med_de.xml" and ".strings_med_fr.xml" to "strings.xml"

end