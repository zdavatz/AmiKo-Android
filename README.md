## AmiKo-Android

### Build

`make amiko` or `make comed`

this will also download the all the files (DB, Report and Interactions).

run `make -B amiko` to rebuild.

### Setup for Android Studio
This setup is [gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html) based.

#### 1. Install Android Studio
https://developer.android.com/studio/

#### 2. Open Project with Android Studio

#### 3. Database
databases are in

`src/amiko/assets/` form here: http://pillbox.oddb.org/amiko_db_full_idx_de.zip (AmiKo, DE)  
`src/comed/assets/` from here: http://pillbox.oddb.org/amiko_db_full_idx_fr.zip (CoMed, FR)

#### 4. Report File
report files are in

`src/amiko/assets/` from here: http://pillbox.oddb.org/amiko_report_de.html (AmiKo, DE)  
`src/comed/assets/` from here: http://pillbox.oddb.org/amiko_report_fr.html (CoMed, FR)

#### 5. Constants.java
in `Constants.java` set `APP_VERSION`, `GEN_DATE` and `DB_VERSION`
_to build with Intellij this does not have to change._

#### 6. AndroidManifest.xml
in `AndroidManifest.xml` increase versionName (=`APP_VERSION` in `Constants.java`)

#### 7. Launch Icon Desitin
change android:icon to `@drawable/ic_launcher`

in these locations:
```
res/layout/splash_screen.xml
res/values/styles.xml
```
## License
[GPLv3.0](https://github.com/zdavatz/AmiKo-Android/blob/master/LICENSE.txt)
