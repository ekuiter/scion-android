package com.example.myapplication;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import sciond.Sciond;

public class SciondService extends BackgroundService {

    public static final String PARAM_CONFIG_PATH = SciondService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 2;
    private static final String TAG = "sciond";

    public SciondService() {
        super("SciondService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) return;
        super.onHandleIntent(intent);
        log(R.string.servicesetup);

        Path shm = Paths.get("run", "shm");
        mkdir(shm);

        delete(shm.resolve("sciond/default.sock"));

        delete(shm.resolve("sciond/default.unix"));

        log(R.string.servicestart);
        long ret = Sciond.main(commandLine("-config", confPath), "", "");
        log(R.string.servicereturn, ret);
    }

    @Override
    protected int getNotificationId() {
        return NID;
    }

    @NonNull
    @Override
    protected String getTag() {
        return TAG;
    }
}
