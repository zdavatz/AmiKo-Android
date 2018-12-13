## AmiKo-Android

### Build

```
make amiko
```

```
make comed
```
this will also download the all the files.

run `make -B amiko` to rebuild.

### Setup for Android Studio
This setup is [gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html) based.

#### 1. Install Android Studio
https://developer.android.com/studio/

#### 2. Open Project with Android Studio
Then continue with Setup as below

#### 3. Database
databases are

`src/amiko/assets/`  
DE: http://pillbox.oddb.org/amiko_db_full_idx_de.zip (AmiKo)

to `src/comed/assets/`  
FR: http://pillbox.oddb.org/amiko_db_full_idx_fr.zip (CoMed)

#### 4. Report File
report files are in

`src/amiko/assets/`  
DE: http://pillbox.oddb.org/amiko_report_de.html (AmiKo)

to `src/comed/assets/`  
FR: http://pillbox.oddb.org/amiko_report_fr.html (CoMed)

#### Constants.java
in `Constants.java` set `APP_VERSION`, `GEN_DATE` and `DB_VERSION`
_to build with Intellij this does not have to change._

#### AndroidManifest.xml
in `AndroidManifest.xml` increase versionName (=`APP_VERSION` in `Constants.java`)

#### Launch Icon Desitin
change android:icon to `@drawable/ic_launcher`

in these locations:
```
res/layout/splash_screen.xml
res/values/styles.xml
```
