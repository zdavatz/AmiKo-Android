## AmiKo-Android

### Build

```
make amiko
```

```
make comed
```

### Setup for Android Studio
This setup is [gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html) based.

#### 1. Install Android Studio
https://developer.android.com/studio/

#### 2. Open Project with Android Studio
Then continue with Setup as below

### Setup for Intellij

#### 1. Database
copy databases to `assets` folders (overwrite old ones)
Download
DE: http://pillbox.oddb.org/amiko_db_full_idx_de.zip (AmiKo)
to `src/amiko/assets/`

FR: http://pillbox.oddb.org/amiko_db_full_idx_fr.zip (CoMed)
to `src/comed/assets/`

#### 2. Report File
copy amiko_report.html to `assets` folders (adapt version number first!)
Download
DE: http://pillbox.oddb.org/amiko_report_de.html (AmiKo)
to `src/amiko/assets/`

FR: http://pillbox.oddb.org/amiko_report_fr.html (CoMed)
to `src/comed/assets/`

#### 3. Constants.java
in `Constants.java` set `APP_VERSION`, `GEN_DATE` and `DB_VERSION`
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
