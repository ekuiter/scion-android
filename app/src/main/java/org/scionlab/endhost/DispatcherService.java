package org.scionlab.endhost;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherService extends BackgroundService {
    public static final String PARAM_CONFIG_PATH = DispatcherService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 1;
    private static final String TAG = "dispatcher";
    private static final String DEFAULT_LOG_PATH = Paths.get("logs/dispatcher.log").toString();

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
        String logPath = getLogPath(confPath);
        if (logPath == null) {
            logPath = DEFAULT_LOG_PATH;
        }
        intent.putExtra(BackgroundService.PARAM_LOG_PATH, logPath);

        log(R.string.servicesetup);

        // Depends on DISPATCHER_DIR and DEFAULT_DISPATCHER_ID from CMakeLists.txt
        Path dispSocket = Paths.get("run/shm/dispatcher/default.sock");
        mkdir(dispSocket.getParent());
        delete(dispSocket);

        delete(Paths.get(logPath));
        mkfile(Paths.get(logPath));

        log(R.string.servicestart);
        super.onHandleIntent(intent);

        int ret = main(confPath, getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
    }

    private String getLogPath (String confPath) {
        try (FileReader confFile = new FileReader(confPath)) {
            BufferedReader confReader = new BufferedReader(confFile);
            Pattern pattern = Pattern.compile("dispatcher.DEBUG \"(.*)\".*");
            String line;
            while ((line = confReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public native int main(String confFileName, String workingDir);
}
