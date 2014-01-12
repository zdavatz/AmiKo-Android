1. copy databases to AMiKo assets folder (overwrite old ones)

2. copy amiko_report.html from XmlParse folder to AMiKo (adapt version number first!)

3. in DataBaseHelper.java increase DB_VERSION and set appropriate DB_NAME

4. in AndroidManifest.xml increase versionName

5a. if (AMiKo) in AndroiManifest.xml set package = "com.ywesee.amiko.de"
5b. if (CoMed) in AndroidManifest.xml set package = "com.ywesee.amiko.fr"

6a. if (AMiKo) rename "amiko_db_full_idx_fr.db" to ".amiko_db_full_idx_fr.db"
6b. if (CoMed) rename "amiko_db_full_idx_de.db" to ".amiko_db_full_idx_de.db"

7a. if (AMiKo) rename "strings.xml" to ".strings_fr.xml" and ".strings_de.xml" to "strings.xml"
7b. if (CoMed) rename "strings.xml" to ".strings_de.xml" and ".strings_fr.xml" to "strings.xml"
