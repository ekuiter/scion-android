package com.example.myapplication;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import sciondgobind.Sciondgobind;

public class SciondService extends IntentService {

    public static final String PARAM_CONFIG_PATH = SciondService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final String TAG = "sciond";

    public SciondService() {
        super("SciondService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) return;
        Log.d(TAG, "Setting up sciond service");

        boolean existed;
        boolean success;

        File shm = new File(getFilesDir(), "run/shm");
        existed = shm.exists();
        success = shm.mkdirs();
        Log.d(TAG, String.format("Creating shm dir %s: %b %b %b", shm.getAbsolutePath(), existed, success, shm.exists()));

        File socket = new File(shm, "sciond/default.sock");
        existed = socket.exists();
        success = socket.delete();
        Log.d(TAG, String.format("Deleting sciond reliable socket %s: %b %b %b", socket.getAbsolutePath(), existed, success, socket.exists()));

        socket = new File(shm, "sciond/default.unix");
        existed = socket.exists();
        success = socket.delete();
        Log.d(TAG, String.format("Deleting sciond unix socket %s: %b %b %b", socket.getAbsolutePath(), existed, success, socket.exists()));

        Log.d(TAG, "Starting sciond service");
        Sciondgobind.realMain(confPath);
    }

}
