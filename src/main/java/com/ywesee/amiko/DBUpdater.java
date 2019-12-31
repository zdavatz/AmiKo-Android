package com.ywesee.amiko;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DBUpdater {

    private static String TAG = "DBUpdater";

    interface Callback {
        void onDownloadProgress(long done, long total);
        void onPreUnpack();
        void onUnpackProgress(String tag, long done, long total);
        void onFinish();
        void onError(Exception e);
    }

    Context context;
    Callback callback;

    int totalFileCount = 0;
    int downloadedFileCount = 0;

    boolean started = false;
    HashMap<String, Long> totalFileSizes = new HashMap<>();
    HashMap<String, Long> downloadedFileSizes = new HashMap<>();

    public DBUpdater(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void start() {
        if (started) return;
        started = true;
        String baseUrl = "http://pillbox.oddb.org/";
        String databaseUri = baseUrl + Constants.appZippedDatabase();
        String fullTextDatabaseUri = baseUrl + Constants.appZippedFullTextDatabase();
        String reportUri = baseUrl + Constants.appReportFile();
        String interactionsUri = baseUrl + Constants.appZippedInteractionsFile();

        String downloadDir =  this.downloadDir();
        addNewDownload(databaseUri, downloadDir, Constants.appZippedDatabase(), "databaseZip");
        addNewDownload(fullTextDatabaseUri, downloadDir, Constants.appZippedFullTextDatabase(), "fullTextDatabaseZip");
        addNewDownload(reportUri, downloadDir, Constants.appReportFile(), "report");
        addNewDownload(interactionsUri, downloadDir, Constants.appZippedInteractionsFile(), "interactionZip");
    }

    private void addNewDownload(String url, String dest, String filename, final String tag) {
        totalFileCount++;

        AndroidNetworking.download(url, dest, filename)
                .setTag(tag)
                .setPriority(Priority.MEDIUM)
                .build()
                .setDownloadProgressListener(new DownloadProgressListener() {
                    @Override
                    public void onProgress(long bytesDownloaded, long totalBytes) {
                        totalFileSizes.put(tag, totalBytes);
                        downloadedFileSizes.put(tag, bytesDownloaded);
                        triggerDownloadProgress();
                    }
                })
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        downloadedFileCount++;
                        checkDownloadProgress();
                    }
                    @Override
                    public void onError(ANError error) {
                        // TODO: handle error
                    }
                });
    }

    private void checkDownloadProgress() {
        if (downloadedFileCount == totalFileCount) {
            new Thread(this::startUnpackFiles).start();
        }
    }

    private void startUnpackFiles() {
        String downloadDir = this.downloadDir();
        String dbDir = context.getApplicationInfo().dataDir + "/databases/";

        String dbPath = dbDir + Constants.appDatabase();
        String fullTextDBPath = dbDir + Constants.appFullTextSearchDatabase();
        String interactionPath = dbDir + Constants.appInteractionsFile();
        String reportPath = dbDir + Constants.appReportFile();

        try {
            callback.onPreUnpack();
            unzipFile(downloadDir + "/" + Constants.appZippedDatabase(), dbPath, "databaseZip");
            unzipFile(downloadDir + "/" + Constants.appZippedFullTextDatabase(), fullTextDBPath, "fullTextDatabaseZip");
            unzipFile(downloadDir + "/" + Constants.appZippedInteractionsFile(), interactionPath, "interactionZip");
            copyFile(downloadDir + "/" + Constants.appReportFile(), reportPath, "report");
            callback.onFinish();
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    private void unzipFile(String srcFile, String dstFile, String tag) {
        byte[] buffer = new byte[2048];
        int bytesRead = -1;
        long totalSize;
        try {
            totalSize = totalFileSizes.getOrDefault(tag, 100L);
        } catch (Exception e) {}

        try {
            FileInputStream is = new FileInputStream(srcFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                FileOutputStream fout = new FileOutputStream(dstFile);
                long totalBytesRead = 0L;

                while ((bytesRead = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, bytesRead);
                    totalBytesRead = is.getChannel().position();
                    callback.onUnpackProgress(tag, totalBytesRead, totalSize);
                }

                Log.d(TAG, "Unzipped file " + ze.getName());

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyFile(String srcFile, String dstFile, String tag) throws IOException {
        long totalSize;
        try {
            totalSize = totalFileSizes.getOrDefault(tag, 100L);
        } catch (Exception e) {}
        InputStream mInput = new FileInputStream(srcFile);
        OutputStream mOutput = new FileOutputStream(dstFile);

        // Transfer bytes from input to output
        byte[] mBuffer = new byte[1024];
        int totalBytesRead = 0;
        int bytesRead;
        while ((bytesRead = mInput.read(mBuffer))>0) {
            mOutput.write(mBuffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            callback.onUnpackProgress(tag, totalBytesRead, totalSize);
        }

        // Close streams
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    private void triggerDownloadProgress() {
        long total = totalFileSizes.values().stream().mapToLong(a -> a).sum();
        long downloaded = downloadedFileSizes.values().stream().mapToLong(a -> a).sum();
        callback.onDownloadProgress(downloaded, total);
    }

    private String downloadDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
    }
}
