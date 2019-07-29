package org.scionlab.endhost;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DispatcherService extends BackgroundService {
    public static final String PARAM_CONFIG_PATH = DispatcherService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 1;
    private static final String TAG = "dispatcher";

    static {
        System.loadLibrary("dispatcher-wrapper");
    }

    public DispatcherService() {
        super("DispatcherService");
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

    @Override
    protected void onHandleIntent (Intent intent) {
        if (intent == null) return;
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) {
            die(R.string.servicenoconf);
            return;
        }
        super.onHandleIntent(intent);

        log(R.string.servicesetup);

        // Depends on DISPATCHER_DIR and DEFAULT_DISPATCHER_ID from CMakeLists.txt
        Path dispSocket = Paths.get("run/shm/dispatcher/default.sock");
        mkdir(dispSocket.getParent());
        delete(dispSocket);

        log(R.string.servicestart);

        int ret = main(confPath, getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
    }

    public native int main(String confFileName, String workingDir);
}
