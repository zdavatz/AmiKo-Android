package com.ywesee.amiko.hinclient;

import android.content.Context;
import android.util.Log;

import com.ywesee.amiko.Utilities;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public class HINSettingsStore {
    public static final String TAG = "HINSettingsStore";
    public static final String SDS_TOKEN_FILENAME = "sds-oauth-token.json";
    public static final String ADSWISS_TOKEN_FILENAME = "adswiss-oauth-token.json";
    public static final String ADSWISS_AUTH_HANDLE_FILENAME = "adswiss-auth-handle.json";

    public String dir;

    public HINSettingsStore(Context context) {
        dir = context.getFilesDir().toString();
    }

    public void saveSDSToken(HINToken token) {
        token.application = HINToken.Application.SDS;
        String targetPath = new File(dir, SDS_TOKEN_FILENAME).getPath();
        try {
            String content = token.toJSON().toString();
            FileOutputStream stream = new FileOutputStream(new File(targetPath));
            try {
                stream.write(content.getBytes());
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public HINToken getSDSToken() {
        String targetPath = new File(dir, SDS_TOKEN_FILENAME).getPath();
        String fileJSON = Utilities.readFromFile(targetPath);
        if (fileJSON == null) return null;
        try {
            JSONObject json = new JSONObject(fileJSON);
            return new HINToken(json);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }
    public void deleteSDSToken() {
        new File(dir, SDS_TOKEN_FILENAME).delete();
    }

    public void saveADSwissToken(HINToken token) {
        token.application = HINToken.Application.ADSwiss;
        String targetPath = new File(dir, ADSWISS_TOKEN_FILENAME).getPath();
        try {
            String content = token.toJSON().toString();
            FileOutputStream stream = new FileOutputStream(new File(targetPath));
            try {
                stream.write(content.getBytes());
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public HINToken getADSwissToken() {
        String targetPath = new File(dir, ADSWISS_TOKEN_FILENAME).getPath();
        String fileJSON = Utilities.readFromFile(targetPath);
        if (fileJSON == null) return null;
        try {
            JSONObject json = new JSONObject(fileJSON);
            return new HINToken(json);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }
    public void deleteADSwissToken() {
        new File(dir, ADSWISS_TOKEN_FILENAME).delete();
        new File(dir, ADSWISS_AUTH_HANDLE_FILENAME).delete();
    }

    public void saveADSwissAuthHandle(ADSwissAuthHandle authHandle) {
        String targetPath = new File(dir, ADSWISS_AUTH_HANDLE_FILENAME).getPath();
        try {
            String content = authHandle.toJSON().toString();
            FileOutputStream stream = new FileOutputStream(new File(targetPath));
            try {
                stream.write(content.getBytes());
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public ADSwissAuthHandle getADSwissAuthHandle() {
        String targetPath = new File(dir, ADSWISS_AUTH_HANDLE_FILENAME).getPath();
        String fileJSON = Utilities.readFromFile(targetPath);
        if (fileJSON == null) return null;
        try {
            JSONObject json = new JSONObject(fileJSON);
            ADSwissAuthHandle handle = new ADSwissAuthHandle(json);
            if (handle.isExpired()) {
                new File(targetPath).delete();
                return null;
            }
            return handle;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }
}
