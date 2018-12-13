## AmiKo-Android

### Setup for Android Studio
This setup is [gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html) based.

#### 1. Install Android Studio
https://developer.android.com/studio/

#### 2. Open Project with Android Studio
Then continue with Setup as below

### Setup for Intellij

#### 1. Database
copy databases to AMiKo `assets` folder (overwrite old ones)  
Download  
DE: http://pillbox.oddb.org/amiko_db_full_idx_de.zip (AmiKo)  
FR: http://pillbox.oddb.org/amiko_db_full_idx_fr.zip (CoMed)

#### 2. Report File
copy amiko_report.html to AMiKo `assets` folder (adapt version number first!)  
Download    
DE: http://pillbox.oddb.org/amiko_report_de.html (AmiKo)  
FR: http://pillbox.oddb.org/amiko_report_fr.html (CoMed)

#### 3. Constants.java
in `Constants.java` select `APP_NAME`, set `APP_VERSION`, `GEN_DATE` and `DB_VERSION`  
_to build with Intellij this does not have to change._

#### 4a. AndroidManifest.xml
in `AndroidManifest.xml` increase versionName (=`APP_VERSION` in `Constants.java`)

#### 4b. Launch Icon Desitin
change android:icon to `@drawable/ic_launcher`  

in these locations:
```
res/layout/splash_screen.xml  
res/values/styles.xml
```
#### 4b. Launch Icon med-drugs 
change android:icon to `@drawable/ic_launcher_med`

#### 5. Package name
5a. if (AMiKo, med-drugs) in AndroidManifest.xml set package = "`com.ywesee.amiko.de`"  
5b. if (CoMed, med-drugs fr) in AndroidManifest.xml set package = "`com.ywesee.amiko.fr`"

#### 6. Database name
6a. if (AMiKo, med-drugs) rename "amiko_db_full_idx_fr.db" to "`.amiko_db_full_idx_fr.db`"  
6b. if (CoMed, med-drugs fr) rename "amiko_db_full_idx_de.db" to "`.amiko_db_full_idx_de.db`"

#### 7. strings.xml Desitin
7a. if (AMiKo) rename "strings.xml" to "`.strings_de.xml`" and ".strings_de.xml" to "`strings.xml`"  
7b. if (CoMed) rename "strings.xml" to "`.strings_fr.xml`" and ".strings_fr.xml" to "`strings.xml`"  
7c. splash_screen -> `splash_screen_meddrugs`, splash_screen_desitin -> `splash_screen`

#### 8. strings.xml med-drugs
8a. if (med-drugs) rename "strings.xml" to "`.strings_med_de.xml`" and ".strings_med_de.xml" to "`strings.xml`"  
8b. if (med-drugs fr) rename "strings.xml" to "`.strings_med_fr.xml`" and ".strings_med_fr.xml" to "`strings.xml`"  
8c. splash_screen -> `splash_screen_desitin`, splash_screen_meddrugs -> `splash_screen`
