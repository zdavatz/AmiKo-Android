package com.ywesee.amiko;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.StartPageToken;

import java.util.Collections;
import java.util.List;

public class SyncService extends JobIntentService {
    final static String TAG = "SyncService";
    private Drive driveService = null;

    public SyncService() {

    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        this.testUploadDoctorFile();
    }

    private Drive getDriveService() {
        if (driveService != null) {
            return driveService;
        }
        Credential cred = PersistenceManager.getShared().getGoogleCredential();
        if (cred == null) return null;
        driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();
        return driveService;
    }

    protected void testUploadDoctorFile() {
        Drive drive = getDriveService();

        if (drive == null) return;
        List<File> fileList = listFiles();
        fetchChangedList(null);

        File fileMetadata = new File();
        String doctorFilename = "doctor.json";
//        fileMetadata.setName(doctorFilename);
//        fileMetadata.setParents(Collections.singletonList("appDataFolder"));
        java.io.File filePath = new java.io.File(this.getFilesDir().toString(), doctorFilename);
        FileContent mediaContent = new FileContent("application/json", filePath);
        try {
//            File file = drive.files().create(fileMetadata, mediaContent)
//                    .setFields("id")
//                    .execute();
            File file = drive.files()
                    .update(fileList.get(0).getId(), fileMetadata, mediaContent)
                    .execute();
                    // .update(fileList.get(0).getId(), fileMetadata).execute();
            System.out.println("File ID: " + file.getId());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        fetchChangedList(null);
    }

    protected void fetchChangedList(StartPageToken token) {
        Drive drive = getDriveService();
        try {
            if (token == null) {
                token = drive.changes().getStartPageToken().execute();
            }
            ChangeList changeList = drive.changes().list(token.getStartPageToken())
                    .setSpaces("appDataFolder")
                    .setFields("*")
                    .setRestrictToMyDrive(false)
                    .execute();
            Log.d(TAG, changeList.getChanges().toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return;
    }

    protected List<File> listFiles() {
        try {
            Drive drive = getDriveService();
            FileList fl = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setFields("*")
                    .execute();
            Log.d(TAG, fl.getFiles().toString());
            return fl.getFiles();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }
}
