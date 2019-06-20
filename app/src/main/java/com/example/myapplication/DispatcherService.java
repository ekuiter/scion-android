package com.example.myapplication;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DispatcherService extends IntentService {
    public static final String PARAM_CONFIG_PATH = DispatcherService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final String TAG = "dispatcher";

    static {
        System.loadLibrary("dispatcher-wrapper");
    }

    public DispatcherService() {
        super("DispatcherService");
    }

    @Override
    protected void onHandleIntent (Intent intent) {
        if (intent == null) return;
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) return;
        Log.d(TAG, "Setting up dispatcher service");

        boolean existed;
        boolean success;

        File shm = new File(getFilesDir(), "run/shm");
        existed = shm.exists();
        success = shm.mkdirs();
        Log.d(TAG, String.format("Creating shm dir %s: %b %b %b", shm.getAbsolutePath(), existed, success, shm.exists()));

        File logs = new File(getFilesDir(), "logs");
        existed = logs.exists();
        success = logs.mkdirs();
        Log.d(TAG, String.format("Creating logs dir %s: %b %b %b", logs.getAbsolutePath(), existed, success, logs.exists()));

        File socket = new File(shm, "dispatcher/default.sock");
        existed = socket.exists();
        success = socket.delete();
        Log.d(TAG, String.format("Deleting dispatcher socket %s: %b %b %b", socket.getAbsolutePath(), existed, success, socket.exists()));

        Log.d(TAG, "Starting dispatcher service");

        main(confPath);
    }

    public native int main(String confFileName);
}
