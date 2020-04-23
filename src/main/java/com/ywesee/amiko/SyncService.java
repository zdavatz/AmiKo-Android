package com.ywesee.amiko;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.StartPageToken;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyncService extends JobIntentService {
    final static String TAG = "SyncService";
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
        Credential cred = PersistenceManager.getShared().getGoogleCredential();
        if (cred == null) return null;
        driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();
        return driveService;
    }

    protected void synchronise() {
        List<File> remoteFiles = this.listRemoteFilesAndFolders();
        List<java.io.File> localFiles = this.listLocalFilesAndFolders();
        Map<String, File> remoteFilesMap = this.remoteFilesToMap(remoteFiles);
        Map<String, java.io.File> localFilesMap = this.localFilesToMap(localFiles);
        Map<String, Long> localVersions = this.localFileVersionMap();
        SyncPlan sp = new SyncPlan(this.getFilesDir(), localFilesMap, localVersions, remoteFilesMap);
        sp.execute();
    }

    protected void testUploadDoctorFile() {
        Drive drive = getDriveService();

        if (drive == null) return;
        List<File> fileList = listRemoteFilesAndFolders();

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
    }

    protected List<File> listRemoteFilesAndFolders() {
        ArrayList<File> files = new ArrayList<>();
        try {
            Drive drive = getDriveService();
            String pageToken = null;

            do {
                FileList fl = drive.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("nextPageToken, files(id, name, version, parents, mimeType)")
                        .execute();
                // application/vnd.google-apps.folder
                files.addAll(fl.getFiles());
                pageToken = fl.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return files;
    }

    protected List<java.io.File> listLocalFilesAndFolders() {
        ArrayList<java.io.File> allFiles = new ArrayList<>();
        java.io.File filesDir = this.getFilesDir();
        ArrayList<java.io.File> pendingFolders = new ArrayList<>();
        pendingFolders.add(filesDir);

        while (!pendingFolders.isEmpty()) {
            java.io.File currentFolder = pendingFolders.get(0);
            java.io.File[] children = currentFolder.listFiles();
            for (java.io.File child : children) {
                allFiles.add(child);
                if (child.isDirectory()) {
                    pendingFolders.add(child);
                }
                pendingFolders.remove(0);
            }
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
                    File thisFile = idMap.get(key);
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

    protected Map<String, java.io.File> localFilesToMap(List<java.io.File> files) {
        java.io.File filesDir = this.getFilesDir();
        Map<String, java.io.File> fileMap = new HashMap<>();
        for (java.io.File file : files) {
            String path = filesDir.toPath().relativize(file.toPath()).toString();
            fileMap.put(path, file);
        }
        return fileMap;
    }

    protected Map<String, Long> remoteFilesToVersionMap(Map<String, File> files) {
        HashMap<String, Long> map = new HashMap<>();
        for (String key : files.keySet()) {
            File file = files.get(key);
            map.put(key, file.getVersion());
        }
        return map;
    }

    protected Map<String, Long> localFileVersionMap() {
        // TODO
        return new HashMap<>();
    }

    protected void saveLocalFileVersionMap(Map<String, Long> map) {
        // TODO
    }

    // TODO: Make sure modified time for remote / local is in sync
    class SyncPlan {
        public java.io.File filesDir;

        private Map<String, java.io.File> localFilesMap;
        private Map<String, Long> localVersionMap;
        private Map<String, File> remoteFilesMap;

        public HashSet<String> pathsToCreate;
        // { path: remote file id }
        public HashMap<String, String> pathsToUpdate;
        // { path: remote file id }
        public HashMap<String, String> pathsToDownload;
        // set of path
        public HashSet<String> localFilesToDelete;
        // set of path
        public HashSet<String> remoteFilesToDelete;

        SyncPlan(java.io.File filesDir,
                 Map<String, java.io.File> localFilesMap,
                 Map<String, Long> localVersionMap,
                 Map<String, File> remoteFilesMap) {

            this.filesDir = filesDir;
            this.localFilesMap = localFilesMap;
            this.localVersionMap = localVersionMap;
            this.remoteFilesMap = remoteFilesMap;

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
                    File file = remoteFilesMap.get(path);
                    Long remoteVerion = file.getVersion();
                    if (remoteVerion == localVersion) {
                        java.io.File localFile = localFilesMap.get(path);
                        File remoteFile = remoteFilesMap.get(path);
                        long localModified = localFile.lastModified();
                        long remoteModified = remoteFile.getModifiedTime().getValue();
                        if (localModified > remoteModified) {
                            pathsToUpdate.put(path, file.getId());
                        }
                    } else if (remoteVerion > localVersion) {
                        pathsToDownload.put(path, file.getId());
                    }
                }
                if (!localHasFile && remoteHasFile) {
                    remoteFilesToDelete.add(path);
                }
                if (localHasFile && !remoteHasFile) {
                    localFilesToDelete.add(path);
                }
            }
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
                                .setFields("id, name, version")
                                .queue(batch, callback);
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
                batch.execute();
                Log.i(TAG, "batch finished");
            }
        }

        public void createFiles(BatchRequest batch) {
            for (String pathToCreate : pathsToCreate) {
                java.io.File localFile = new java.io.File(this.filesDir, pathToCreate);
                java.io.File parentFile = localFile.getParentFile();
                boolean atRoot = parentFile.toPath().equals(this.filesDir.toPath());
                String parentRelativeToFilesDir = this.filesDir.toPath().relativize(parentFile.toPath()).toString();
                File remoteParent = this.remoteFilesMap.get(parentRelativeToFilesDir);

                File fileMetadata = new File();
                fileMetadata.setName(localFile.getName());
                if (atRoot) {
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                } else {
                    fileMetadata.setParents(Collections.singletonList(remoteParent.getId()));
                }
                FileContent mediaContent = new FileContent("application/json", localFile);

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
                        Log.i(TAG, "Created file");
                        Log.i(TAG, "File name: " + result.getName());
                        Log.i(TAG, "File ID: " + result.getId());
                        pathsToCreate.remove(pathToCreate);
                        remoteFilesMap.put(pathToCreate, result);
                    }
                };

                try {
                    driveService.files()
                            .create(fileMetadata, mediaContent)
                            .setFields("id, name, version")
                            .queue(batch, callback);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        public void updateFiles(BatchRequest batch) throws IOException {
            for (String path : this.pathsToUpdate.keySet()) {
                String fileId = this.pathsToUpdate.get(path);
                java.io.File localFile = new java.io.File(this.filesDir, path);

                File fileMetadata = new File();
                fileMetadata.setName(localFile.getName());
                FileContent mediaContent = new FileContent("application/json", localFile);

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
                        pathsToUpdate.remove(path);
                        remoteFilesMap.put(path, result);
                    }
                };

                driveService.files()
                        .update(fileId, fileMetadata, mediaContent)
                        .setFields("id, name, version")
                        .queue(batch, callback);
            }
        }

        public void deleteFiles(BatchRequest batch) throws IOException {
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
        }

        public void downloadRemoteFiles() throws IOException {
            for (String path : this.pathsToDownload.keySet()) {
                String fileId = this.pathsToDownload.get(path);
                java.io.File localFile = new java.io.File(this.filesDir, path);
                java.io.File parent = localFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(localFile);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);
                outputStream.writeTo(outputStream);
            }

        }

        public void deleteLocalFiles() {
            for (String path : this.localFilesToDelete) {
                java.io.File file = new java.io.File(this.filesDir, path);
                file.delete();
            }
        }

        public void execute() {
            try {
                this.createFolders();

                BatchRequest batch = driveService.batch();
                this.createFiles(batch);
                this.updateFiles(batch);
                this.deleteFiles(batch);
                if (batch.size() > 0) {
                    batch.execute();
                }
                this.downloadRemoteFiles();
                this.deleteLocalFiles();
                // TODO: construct new version map
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
