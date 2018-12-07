AmiKo-Android
=============

### AmiKo for Android setup for Intellij

#### 1. Database
copy databases to AMiKo assets folder (overwrite old ones)
Download DB from here: http://pillbox.oddb.org/amiko_db_full_idx_de.zip

#### 2. Report File
copy amiko_report.html from XmlParse folder to AMiKo (adapt version number first!)
Download Report file from here: http://pillbox.oddb.org/amiko_report_de.html

#### 3. Constants.java
in Constants.java select APP_NAME, set APP_VERSION, GEN_DATE and DB_VERSION

#### 4a. AndroidManifest.xml
in AndroidManifest.xml increase versionName (=APP_VERSION in Constants.java)

#### 4b. Launch Icon Desitin
change android:icon to @drawable/ic_launcher

#### 4b. Launch Icon med-drugs 
change android:icon to @drawable/ic_launcher_med

#### 5. Package name
5a. if (AMiKo, med-drugs) in AndroidManifest.xml set package = "com.ywesee.amiko.de"  
5b. if (CoMed, med-drugs fr) in AndroidManifest.xml set package = "com.ywesee.amiko.fr"

#### 6. Database name
6a. if (AMiKo, med-drugs) rename "amiko_db_full_idx_fr.db" to ".amiko_db_full_idx_fr.db"  
6b. if (CoMed, med-drugs fr) rename "amiko_db_full_idx_de.db" to ".amiko_db_full_idx_de.db"

#### 7. strings.xml Desitin
7a. if (AMiKo) rename "strings.xml" to ".strings_fr.xml" and ".strings_de.xml" to "strings.xml"  
7b. if (CoMed) rename "strings.xml" to ".strings_de.xml" and ".strings_fr.xml" to "strings.xml"  
7c. splash_screen -> splash_screen_meddrugs, splash_screen_desitin -> splash_screen

#### 8. strings.xml med-drugs
8a. if (med-drugs) rename "strings.xml" to ".strings_med_fr.xml" and ".strings_med_de.xml" to "strings.xml"  
8b. if (med-drugs fr) rename "strings.xml" to ".strings_med_de.xml" and ".strings_med_fr.xml" to "strings.xml"  
8c. splash_screen -> splash_screen_desitin, splash_screen_meddrugs -> splash_screen
