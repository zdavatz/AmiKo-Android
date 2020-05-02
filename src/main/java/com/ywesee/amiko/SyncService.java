package com.ywesee.amiko;

import android.content.Context;
import android.content.Intent;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyncService extends JobIntentService {
    final static String TAG = "SyncService";
    public static final String BROADCAST_STATUS = "com.ywesee.amiko.SYNC_BROADCAST";
    public static final String BROADCAST_SYNCED_FILE = "com.ywesee.amiko.SYNC_FILE_BROADCAST";
    public static final String BROADCAST_SYNCED_PATIENT = "com.ywesee.amiko.SYNC_PATIENT_BROADCAST";
    final static String FILE_FIELDS = "id, name, version, parents, mimeType, modifiedTime, size, properties";
    private Drive driveService = null;

    public SyncService() {

    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        this.synchronise();
    }

    private Drive getDriveService() {
        if (driveService != null) {
            return driveService;
        }
        Credential cred = SyncManager.getShared().getGoogleCredential();
        if (cred == null) return null;
        driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();
        return driveService;
    }

    /*
    To sync data, we:
    1. Load remote file-metadata list (with file version and last modified)
    2. Compare with local files and versions
    3. Upload / download files
    4. Record the versions to local (googleSync/version.json)

    To sync patients, we:
    1. Get remote patients from file-metadata, under /patients/
    2. Get patients last modified from db
    3. Compare them, upload or download
    4. To upload patient, create empty remote files /patients/<uid>, put the real data under the properties of the file
     */
    protected void synchronise() {
        Log.i(TAG, "Start syncing");
        this.reportStatus("Starting sync");
        List<File> remoteFiles = this.listRemoteFilesAndFolders();
        List<java.io.File> localFiles = this.listLocalFilesAndFolders();
        Map<String, File> remoteFilesMap = this.remoteFilesToMap(remoteFiles);
        Map<String, File> remotePatientsMap = this.extractPatientsFromFilesMap(remoteFilesMap);
        Map<String, java.io.File> localFilesMap = this.localFilesToMap(localFiles);
        Map<String, Long> localVersions = this.localFileVersionMap();
        Map<String, Long> patientVersions = this.extractPatientsFromFilesMap(localVersions);

        PatientDBAdapter db = new PatientDBAdapter(this);
        Map<String, Date> localPatientTimestamps = this.localPatientTimestamps(db);

        Log.i(TAG, "remoteFiles " + remoteFiles.toString());
        Log.i(TAG, "localFiles " + localFiles.toString());
        Log.i(TAG, "remoteFilesMap " + remoteFilesMap.toString());
        Log.i(TAG, "localFilesMap " + localFilesMap.toString());
        Log.i(TAG, "localVersions " + localVersions.toString());
        Log.i(TAG, "remotePatientsMap " + remotePatientsMap.toString());
        Log.i(TAG, "patientVersions " + patientVersions.toString());

        SyncPlan sp = new SyncPlan(
                this.getFilesDir(),
                localFilesMap,
                localVersions,
                remoteFilesMap,
                patientVersions,
                localPatientTimestamps,
                remotePatientsMap,
                db
        );

        HashMap<String, Long> newVersionMap = sp.execute();
        db.close();
        this.saveLocalFileVersionMap(newVersionMap);
        this.reportStatus("Finished sync");
        Log.i(TAG, "End syncing");
    }

    protected List<File> listRemoteFilesAndFolders() {
        ArrayList<File> files = new ArrayList<>();
        try {
            Drive drive = getDriveService();
            String pageToken = null;

            do {
                FileList fl = drive.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("nextPageToken, files(" + FILE_FIELDS + ")")
                        .execute();
                files.addAll(fl.getFiles());
                pageToken = fl.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return files;
    }

    protected List<java.io.File> listLocalFilesAndFolders() {
        ArrayList<java.io.File> allFiles = new ArrayList<>();
        java.io.File filesDir = this.getFilesDir();
        ArrayList<java.io.File> pendingFolders = new ArrayList<>();
        pendingFolders.add(filesDir);

        java.io.File syncFolder = new java.io.File(this.getFilesDir(), "googleSync");

        while (!pendingFolders.isEmpty()) {
            java.io.File currentFolder = pendingFolders.get(0);
            java.io.File[] children = currentFolder.listFiles();
            for (java.io.File child : children) {
                if (child.getPath().equals(syncFolder.getPath()) || child.getPath().startsWith(syncFolder.getPath())) {
                    // Skip "googleSync" folder which is used to store sync and creds info
                    continue;
                }
                allFiles.add(child);
                if (child.isDirectory()) {
                    pendingFolders.add(child);
                }
            }
            pendingFolders.remove(0);
        }
        return allFiles;
    }

    /**
     * Convert remote files into a ([path] -> File) map
     * e.g. { "amk/111/abc.amk": File }
     *
     * @param files
     * @return the files map
     */
    protected Map<String, File> remoteFilesToMap(List<File> files) {
        Map<String, File> fileMap = new HashMap<>();
        Map<String, File> idMap = new HashMap<>();

        for (File file : files) {
            idMap.put(file.getId(), file);
        }

        for (String key : idMap.keySet()) {
            File file = idMap.get(key);
            ArrayList<String> parents = new ArrayList<>();
            String fileId = key;
            while (fileId != null) {
                try {
                    File thisFile = idMap.get(fileId);
                    String parent = thisFile.getParents().get(0);
                    if (parent != null) {
                        File parentFile = idMap.get(parent);
                        parents.add(0, parentFile.getName());
                    }
                    fileId = parent;
                } catch (Exception e) {
                    fileId = null;
                }
            }
            parents.add(file.getName());
            String fullPath = String.join("/", parents);
            fileMap.put(fullPath, file);
        }
        return fileMap;
    }

    /**
     * Modify the given map, move the "patient entries" in the map to the new returned map
     * @param filesMap
     * @return
     */
    protected <T> HashMap<String, T> extractPatientsFromFilesMap(Map<String, T> filesMap) {
        HashMap<String, T> map = new HashMap<>();
        for (String path : new HashSet<>(filesMap.keySet())) {
            if (path.startsWith("patients/")) {
                map.put(path.replace("patients/", ""), filesMap.get(path));
                filesMap.remove(path);
            }
        }
        return map;
    }

    protected HashMap<String, Date> localPatientTimestamps(PatientDBAdapter db) {
        HashMap<String, String> strMap = db.getAllTimestamps();
        HashMap<String, Date> map = new HashMap<>();
        for (String uid : strMap.keySet()) {
            Date timeStamp = Utilities.dateFromTimeString(strMap.get(uid));
            if (timeStamp != null) {
                map.put(uid, timeStamp);
            }
        }
        return map;
    }

    protected Map<String, java.io.File> localFilesToMap(List<java.io.File> files) {
        java.io.File filesDir = this.getFilesDir();
        Map<String, java.io.File> fileMap = new HashMap<>();
        for (java.io.File file : files) {
            String path = filesDir.toPath().relativize(file.toPath()).toString();
            fileMap.put(path, file);
        }
        return fileMap;
    }

    protected Map<String, Long> localFileVersionMap() {
        java.io.File versionFile = new java.io.File(this.getFilesDir(), "googleSync/versions.json");
        if (!versionFile.exists()) {
            return new HashMap<>();
        }
        HashMap<String, Long> map = new HashMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(versionFile);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                Long version = reader.nextLong();
                map.put(name, version);
            }
            reader.endObject();
            reader.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return map;
    }

    protected void saveLocalFileVersionMap(Map<String, Long> map) {
        java.io.File versionFile = new java.io.File(this.getFilesDir(), "googleSync/versions.json");
        java.io.File parent = versionFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            FileOutputStream stream = new FileOutputStream(versionFile);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.beginObject();
            for (String key : map.keySet()) {
                Long version = map.get(key);
                writer.name(key).value(version);
            }
            writer.endObject();
            writer.close();
            stream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    protected void reportStatus(String status) {
        Intent localIntent =
                new Intent(SyncService.BROADCAST_STATUS)
                        .putExtra("status", status);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    protected void reportUpdatedFile(java.io.File file) {
        Intent localIntent =
                new Intent(SyncService.BROADCAST_SYNCED_FILE)
                        .putExtra("path", file.getAbsolutePath());
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    protected void reportUpdatedPatient(String uid) {
        Intent localIntent =
                new Intent(SyncService.BROADCAST_SYNCED_PATIENT)
                        .putExtra("uid", uid);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public static String lastSynced(Context context) {
        java.io.File versionFile = new java.io.File(context.getFilesDir(), "googleSync/versions.json");
        if (!versionFile.exists()) {
            return "None";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
        return sdf.format(new Date(versionFile.lastModified()));
    }

    class SyncPlan {
        public java.io.File filesDir;

        private Map<String, java.io.File> localFilesMap;
        private Map<String, Long> localVersionMap;
        private Map<String, File> remoteFilesMap;
        private Map<String, Long> patientVersions;
        private Map<String, Date> localPatientTimestamps;
        private Map<String, File> remotePatientsMap;

        public HashSet<String> pathsToCreate;
        // { path: remote file id }
        public HashMap<String, String> pathsToUpdate;
        // { path: remote file id }
        public HashMap<String, String> pathsToDownload;
        // set of path
        public HashSet<String> localFilesToDelete;
        // set of path
        public HashSet<String> remoteFilesToDelete;

        public HashSet<String> patientsToCreate; // uid
        public HashMap<String, File> patientsToUpdate; // uid : remote file
        public HashMap<String, File> patientsToDownload; // uid : remote file
        public HashSet<String> localPatientsToDelete; // uid
        public HashMap<String, File> remotePatientsToDelete;
        private PatientDBAdapter patientDB;

        SyncPlan(java.io.File filesDir,
                 Map<String, java.io.File> localFilesMap,
                 Map<String, Long> localVersionMap,
                 Map<String, File> remoteFilesMap,
                 Map<String, Long> patientVersions,
                 Map<String, Date> localPatientTimestamps,
                 Map<String, File> remotePatientsMap,
                 PatientDBAdapter patientDB
                 ) {

            this.filesDir = filesDir;
            this.localFilesMap = localFilesMap;
            this.localVersionMap = localVersionMap;
            this.remoteFilesMap = remoteFilesMap;
            this.patientVersions = patientVersions;
            this.localPatientTimestamps = localPatientTimestamps;
            this.remotePatientsMap = remotePatientsMap;
            this.patientDB = patientDB;

            prepareFiles();
            preparePatients();
        }

        void prepareFiles() {
            pathsToCreate = new HashSet<>();
            pathsToUpdate = new HashMap<>();
            pathsToDownload = new HashMap<>();
            localFilesToDelete = new HashSet<>();
            remoteFilesToDelete = new HashSet<>();

            pathsToCreate.addAll(localFilesMap.keySet());
            pathsToCreate.removeAll(localVersionMap.keySet());
            pathsToCreate.removeAll(remoteFilesMap.keySet());

            for (String path : localVersionMap.keySet()) {
                boolean localHasFile = localFilesMap.containsKey(path);
                boolean remoteHasFile = remoteFilesMap.containsKey(path);

                if (localHasFile && remoteHasFile) {
                    Long localVersion = localVersionMap.get(path);
                    java.io.File localFile = localFilesMap.get(path);
                    File remoteFile = remoteFilesMap.get(path);
                    Long remoteVerion = remoteFile.getVersion();

                    if (localFile.isDirectory() || remoteFile.getMimeType().equals("application/vnd.google-apps.folder")) {
                        continue;
                    }
                    if (remoteVerion == localVersion) {
                        long localModified = localFile.lastModified();
                        long remoteModified = remoteFile.getModifiedTime().getValue();
                        if (localModified > remoteModified) {
                            pathsToUpdate.put(path, remoteFile.getId());
                        }
                    } else if (remoteVerion > localVersion) {
                        pathsToDownload.put(path, remoteFile.getId());
                    }
                }
                if (!localHasFile && remoteHasFile) {
                    File remoteFile = remoteFilesMap.get(path);
                    if (!remoteFile.getMimeType().equals("application/vnd.google-apps.folder")) {
                        remoteFilesToDelete.add(path);
                    }
                }
                if (localHasFile && !remoteHasFile) {
                    localFilesToDelete.add(path);
                }
            }

            for (String path : remoteFilesMap.keySet()) {
                File remoteFile = remoteFilesMap.get(path);
                if (remoteFile.getMimeType().equals("application/vnd.google-apps.folder")) {
                    continue;
                }
                if (localVersionMap.containsKey(path)) {
                    // We already handled this in the above loop
                    continue;
                }
                if (localFilesMap.containsKey(path)) {
                    // File exist both on server and local, but has no local version
                    java.io.File localFile = localFilesMap.get(path);
                    long localModified = localFile.lastModified();
                    long remoteModified = remoteFile.getModifiedTime().getValue();
                    if (localModified > remoteModified) {
                        pathsToUpdate.put(path, remoteFile.getId());
                    } else if (localModified < remoteModified) {
                        pathsToDownload.put(path, remoteFile.getId());
                    }
                } else {
                    pathsToDownload.put(path, remoteFile.getId());
                }
            }

            Log.i(TAG, "pathsToCreate: " + pathsToCreate.toString());
            Log.i(TAG, "pathsToUpdate: " + pathsToUpdate.toString());
            Log.i(TAG, "pathsToDownload: " + pathsToDownload.toString());
            Log.i(TAG, "localFilesToDelete: " + localFilesToDelete.toString());
            Log.i(TAG, "remoteFilesToDelete: " + remoteFilesToDelete.toString());
            Log.i(TAG, "pathsToCreate: " + pathsToCreate.toString());
        }

        void preparePatients() {
            patientsToCreate = new HashSet<>();
            patientsToUpdate = new HashMap<>();
            patientsToDownload = new HashMap<>();
            localPatientsToDelete = new HashSet<>();
            remotePatientsToDelete = new HashMap<>();

            patientsToCreate.addAll(localPatientTimestamps.keySet());
            patientsToCreate.removeAll(patientVersions.keySet());
            patientsToCreate.removeAll(remotePatientsMap.keySet());

            for (String uid : patientVersions.keySet()) {
                boolean localHasPatient = localPatientTimestamps.containsKey(uid);
                boolean remoteHasPatient = remotePatientsMap.containsKey(uid);

                if (localHasPatient && remoteHasPatient) {
                    Long localVersion = patientVersions.get(uid);
                    Date localTimestamp = localPatientTimestamps.get(uid);
                    File remoteFile = remotePatientsMap.get(uid);
                    Long remoteVerion = remoteFile.getVersion();

                    if (remoteVerion == localVersion) {
                        long localModified = localTimestamp.getTime();
                        long remoteModified = remoteFile.getModifiedTime().getValue();
                        if (localModified > remoteModified) {
                            patientsToUpdate.put(uid, remoteFile);
                        }
                    } else if (remoteVerion > localVersion) {
                        patientsToDownload.put(uid, remoteFile);
                    }
                }
                if (!localHasPatient && remoteHasPatient) {
                    remotePatientsToDelete.put(uid, remotePatientsMap.get(uid));
                }
                if (localHasPatient && !remoteHasPatient) {
                    localPatientsToDelete.add(uid);
                }
            }

            for (String uid : remotePatientsMap.keySet()) {
                if (patientVersions.containsKey(uid)) {
                    // We already handled this in the above loop
                    continue;
                }
                if (localPatientTimestamps.containsKey(uid)) {
                    // File exist both on server and local, but has no local version
                    Date timestamp = localPatientTimestamps.get(uid);
                    File remoteFile = remotePatientsMap.get(uid);
                    long localModified = timestamp.getTime();
                    long remoteModified = remoteFile.getModifiedTime().getValue();
                    if (localModified > remoteModified) {
                        patientsToUpdate.put(uid, remoteFile);
                    } else if (remoteModified > localModified) {
                        patientsToDownload.put(uid, remoteFile);
                    }
                } else {
                    File remoteFile = remotePatientsMap.get(uid);
                    patientsToDownload.put(uid, remoteFile);
                }
            }

            Log.i(TAG, "patientsToCreate " + patientsToCreate.toString());
            Log.i(TAG, "patientsToUpdate " + patientsToUpdate.toString());
            Log.i(TAG, "patientsToDownload " + patientsToDownload.toString());
            Log.i(TAG, "localPatientsToDelete " + localPatientsToDelete.toString());
            Log.i(TAG, "remotePatientsToDelete " + remotePatientsToDelete.toString());
        }

        public void createFolders() throws IOException {
            while (true) {
                List<java.io.File> allFoldersToCreate = this.pathsToCreate.stream()
                        .map(path -> new java.io.File(this.filesDir, path))
                        .filter(java.io.File::isDirectory)
                        .collect(Collectors.toList());
                if (allFoldersToCreate.size() == 0) {
                    break;
                }

                Drive driveService = getDriveService();
                if (driveService == null) return;
                BatchRequest batch = driveService.batch();

                for (java.io.File folderToCreate : allFoldersToCreate) {
                    String relativePath = this.filesDir.toPath().relativize(folderToCreate.toPath()).toString();
                    java.io.File parentFile = folderToCreate.getParentFile();
                    boolean atRoot = parentFile.toPath().equals(this.filesDir.toPath());
                    String parentRelativeToFilesDir = this.filesDir.toPath().relativize(parentFile.toPath()).toString();
                    File remoteParent = this.remoteFilesMap.get(parentRelativeToFilesDir);
                    if (!atRoot && remoteParent == null) {
                        // parent is not created yet, skip this
                        continue;
                    }

                    File fileMetadata = new File();
                    fileMetadata.setName(folderToCreate.getName());
                    fileMetadata.setMimeType("application/vnd.google-apps.folder");
                    if (atRoot) {
                        fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    } else {
                        fileMetadata.setParents(Collections.singletonList(remoteParent.getId()));
                    }

                    JsonBatchCallback<File> callback = new JsonBatchCallback<File>() {
                        @Override
                        public void onFailure(GoogleJsonError e,
                                              HttpHeaders responseHeaders)
                                throws IOException {
                            Log.e(TAG, e.getMessage());
                        }

                        @Override
                        public void onSuccess(File result,
                                              HttpHeaders responseHeaders)
                                throws IOException {
                            Log.i(TAG, "Created folder");
                            Log.i(TAG, "File name: " + result.getName());
                            Log.i(TAG, "File ID: " + result.getId());
                            pathsToCreate.remove(relativePath);
                            remoteFilesMap.put(relativePath, result);
                        }
                    };

                    try {
                        driveService.files().create(fileMetadata)
                                .setFields(FILE_FIELDS)
                                .queue(batch, callback);
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
                if (batch.size() > 0) {
                    batch.execute();
                }
                Log.i(TAG, "batch finished");
            }
        }

        public void createFiles() {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            HashSet<String> toRemove = new HashSet<>();
            int i = 0;
            for (String pathToCreate : pathsToCreate) {
                java.io.File localFile = new java.io.File(this.filesDir, pathToCreate);
                java.io.File parentFile = localFile.getParentFile();
                boolean atRoot = parentFile.toPath().equals(this.filesDir.toPath());
                String parentRelativeToFilesDir = this.filesDir.toPath().relativize(parentFile.toPath()).toString();
                File remoteParent = this.remoteFilesMap.get(parentRelativeToFilesDir);

                if (!atRoot && !remoteParent.getMimeType().equals("application/vnd.google-apps.folder")) {
                    Log.e(TAG, "remoteParent is not a folder?");
                }

                File fileMetadata = new File();
                fileMetadata.setName(localFile.getName());
                fileMetadata.setModifiedTime(new DateTime(localFile.lastModified()));
                if (atRoot) {
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                } else {
                    fileMetadata.setParents(Collections.singletonList(remoteParent.getId()));
                }
                FileContent mediaContent = new FileContent("application/octet-stream", localFile);

                reportStatus("Uploading new files (" + i + "/" + pathsToCreate.size() + ")");
                try {
                    File result = driveService.files()
                            .create(fileMetadata, mediaContent)
                            .setFields(FILE_FIELDS)
                            .execute();
                    Log.i(TAG, "Created file");
                    Log.i(TAG, "File name: " + result.getName());
                    Log.i(TAG, "File ID: " + result.getId());
                    toRemove.add(pathToCreate);
                    remoteFilesMap.put(pathToCreate, result);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                i++;
            }
            pathsToCreate.removeAll(toRemove);
        }

        public void updateFiles() {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            int i = 0;
            for (String path : new HashSet<>(this.pathsToUpdate.keySet())) {
                String fileId = this.pathsToUpdate.get(path);
                java.io.File localFile = new java.io.File(this.filesDir, path);

                File fileMetadata = new File();
                fileMetadata.setName(localFile.getName());
                FileContent mediaContent = new FileContent("application/octet-stream", localFile);

                reportStatus("Updating files (" + i + "/" + pathsToUpdate.size() + ")");
                try {
                    File result = driveService.files()
                            .update(fileId, fileMetadata, mediaContent)
                            .setFields(FILE_FIELDS)
                            .execute();
                    Log.i(TAG, "Updated files");
                    Log.i(TAG, "File name: " + result.getName());
                    Log.i(TAG, "File ID: " + result.getId());
                    pathsToUpdate.remove(path);
                    remoteFilesMap.put(path, result);
                } catch (Exception e){}
                i++;
            }
        }

        public void deleteFiles() throws IOException {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            BatchRequest batch = driveService.batch();
            reportStatus("Deleting files...");
            for (String path : this.remoteFilesToDelete) {
                File remoteFile = this.remoteFilesMap.get(path);
                JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
                    @Override
                    public void onFailure(GoogleJsonError e,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onSuccess(Void v,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.i(TAG, "Deleted file");
                        Log.i(TAG, "fileId: " + remoteFile.getId());
                        remoteFilesToDelete.remove(path);
                        remoteFilesMap.remove(path);
                    }
                };

                driveService.files()
                        .delete(remoteFile.getId())
                        .queue(batch, callback);
            }
            if (batch.size() > 0) {
                batch.execute();
            }
        }

        public void downloadRemoteFiles() throws IOException {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            int i = 0;
            for (String path : this.pathsToDownload.keySet()) {
                String fileId = this.pathsToDownload.get(path);
                File remoteFile = this.remoteFilesMap.get(path);
                java.io.File localFile = new java.io.File(this.filesDir, path);
                java.io.File parent = localFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                if (remoteFile.getSize() != null && remoteFile.getSize() == 0) {
                    continue;
                }
                reportStatus("Downloading files (" + i + "/" + pathsToDownload.size() + ")");
                try {
                    FileOutputStream fos = new FileOutputStream(localFile);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    driveService.files().get(fileId)
                            .executeMediaAndDownloadTo(outputStream);
                    outputStream.writeTo(fos);
                    localFile.setLastModified(remoteFile.getModifiedTime().getValue());
                    reportUpdatedFile(localFile);
                } catch (Exception e){}
                i++;
            }
        }

        public void deleteLocalFiles() {
            int i = 0;
            for (String path : this.localFilesToDelete) {
                reportStatus("Deleting files (" + i + "/" + localFilesToDelete.size() + ")");
                java.io.File file = new java.io.File(this.filesDir, path);
                file.delete();
                reportUpdatedFile(file);
                i++;
            }
        }

        void createPatientFolder() throws IOException {
            if (remoteFilesMap.containsKey("patients")) return;
            reportStatus("Preparing patients");
            File fileMetadata = new File();
            fileMetadata.setName("patients");
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            fileMetadata.setParents(Collections.singletonList("appDataFolder"));
            try {
                File file = driveService.files().create(fileMetadata)
                        .setFields(FILE_FIELDS)
                        .execute();
                remoteFilesMap.put("patients", file);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        void createPatients(BatchRequest batch) {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            File patientFolder = remoteFilesMap.get("patients");
            if (patientFolder == null) {
                Log.e(TAG, "Cannot find patients folder");
                return;
            }
            ArrayList<Patient> patients = this.patientDB.getPatientsWithUids(this.patientsToCreate);
            for (Patient patient : patients) {
                File fileMetadata = new File();
                fileMetadata.setName(patient.uid);
                fileMetadata.setProperties(patient.toMap());
                fileMetadata.setMimeType("application/octet-stream");
                fileMetadata.setParents(Collections.singletonList(patientFolder.getId()));
                JsonBatchCallback<File> callback = new JsonBatchCallback<File>() {
                    @Override
                    public void onFailure(GoogleJsonError e,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onSuccess(File result,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.i(TAG, "Created patient " + patient.uid);
                        Log.i(TAG, "File ID: " + result.getId());
                        patientsToCreate.remove(patient.uid);
                        remotePatientsMap.put(patient.uid, result);
                    }
                };

                try {
                    driveService.files().create(fileMetadata)
                            .setFields(FILE_FIELDS)
                            .queue(batch, callback);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        void updatePatients(BatchRequest batch) throws IOException {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            ArrayList<Patient> patients = this.patientDB.getPatientsWithUids(this.patientsToUpdate.keySet());
            for (Patient patient : patients) {
                File file = this.patientsToUpdate.get(patient.uid);

                File fileMetadata = new File();
                fileMetadata.setName(patient.uid);
                fileMetadata.setProperties(patient.toMap());
                JsonBatchCallback<File> callback = new JsonBatchCallback<File>() {
                    @Override
                    public void onFailure(GoogleJsonError e,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onSuccess(File result,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.i(TAG, "Updated files");
                        Log.i(TAG, "File name: " + result.getName());
                        Log.i(TAG, "File ID: " + result.getId());
                        patientsToUpdate.remove(patient.uid);
                        remotePatientsMap.put(patient.uid, result);
                    }
                };

                driveService.files()
                        .update(file.getId(), fileMetadata)
                        .setFields(FILE_FIELDS)
                        .queue(batch, callback);
            }
        }

        void deletePatients(BatchRequest batch) throws IOException {
            Drive driveService = getDriveService();
            if (driveService == null) return;
            for (String uid : new HashSet<>(this.remotePatientsToDelete.keySet())) {
                File file = this.remotePatientsToDelete.get(uid);
                JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
                    @Override
                    public void onFailure(GoogleJsonError e,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onSuccess(Void v,
                                          HttpHeaders responseHeaders)
                            throws IOException {
                        Log.i(TAG, "Deleted remote patient");
                        Log.i(TAG, "fileId: " + file.getId());
                        remotePatientsToDelete.remove(uid);
                        remotePatientsMap.remove(uid);
                    }
                };
                driveService.files()
                        .delete(file.getId())
                        .queue(batch, callback);
            }
        }

        void downloadRemotePatients() {
            for (String uid : this.patientsToDownload.keySet()) {
                File file = this.patientsToDownload.get(uid);
                this.patientDB.upsertRecordByUid(new Patient(file.getProperties()));
                reportUpdatedPatient(uid);
            }
        }

        void deleteLocalPatients() {
            for (String uid : this.localPatientsToDelete) {
                this.patientDB.deletePatientWithUid(uid);
                this.patientVersions.remove(uid);
                reportUpdatedPatient(uid);
            }
        }

        private HashMap<String, Long> syncFiles() throws IOException {
            reportStatus("Preparing folders...");
            this.createFolders();
            Log.i(TAG, "new remote map after creating folder " + remoteFilesMap.toString());

            this.createFiles();
            this.updateFiles();
            this.deleteFiles();
            Log.i(TAG, "new remote map after uploading " + remoteFilesMap.toString());
            this.downloadRemoteFiles();
            this.deleteLocalFiles();
            HashMap<String, Long> newVersionMap = this.remoteFilesToVersionMap(this.remoteFilesMap);
            return newVersionMap;
        }

        private HashMap<String, Long> syncPatients() throws IOException {
            BatchRequest batch = driveService.batch();
            this.createPatientFolder();
            Log.i(TAG, "new remote map after creating patient folder " + remoteFilesMap.toString());

            reportStatus("Syncing patients");
            this.createPatients(batch);
            this.updatePatients(batch);
            this.deletePatients(batch);
            if (batch.size() > 0) {
                batch.execute();
            }
            Log.i(TAG, "new remote map after uploading patients " + remotePatientsMap.toString());
            this.downloadRemotePatients();
            this.deleteLocalPatients();
            HashMap<String, Long> newVersionMap = this.remoteFilesToVersionMap(this.remotePatientsMap);
            return newVersionMap;
        }

        public HashMap<String, Long> execute() {
            try {
                HashMap<String, Long> fileVersions = this.syncFiles();
                HashMap<String, Long> patientVersions = this.syncPatients();

                HashMap<String, Long> result = new HashMap<>(fileVersions);
                for (String uid : patientVersions.keySet()) {
                    result.put("patients/" + uid, patientVersions.get(uid));
                }
                return result;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        protected HashMap<String, Long> remoteFilesToVersionMap(Map<String, File> files) {
            HashMap<String, Long> map = new HashMap<>();
            for (String key : files.keySet()) {
                File file = files.get(key);
                map.put(key, file.getVersion());
            }
            return map;
        }
    }
}
