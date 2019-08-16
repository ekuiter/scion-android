/*
 * Copyright (C) 2019  Vera Clemens, Tom Kranz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.scionlab.endhost;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherService extends BackgroundService {
    // Depends on DISPATCHER_DIR and DEFAULT_DISPATCHER_ID from CMakeLists.txt
    public static final String DEFAULT_DISP_SOCKET_PATH = "run/shm/dispatcher/default.sock";
    public static final String PARAM_CONFIG_PATH = DispatcherService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 1;
    private static final String TAG = "dispatcher";
    private static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]+] \\(\\d+:dispatcher:\\.\\./\\.\\./\\.\\./\\.\\./src/main/cpp/gobind-scion/c/dispatcher/dispatcher\\.c:\\d+\\)\\s+");
    private static final String DEFAULT_LOG_PATH = "logs/dispatcher.log";

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

    @NonNull
    @Override
    protected Pattern getLogDeleter() {
        return LOG_DELETER_PATTERN;
    }

    @Override
    protected void onHandleIntent (Intent intent) {
        if (intent == null) return;
        super.onHandleIntent(intent);
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) {
            die(R.string.servicenoconf);
            return;
        }
        String logPath = getLogPath(confPath);

        log(R.string.servicesetup);

        mkfile(DEFAULT_DISP_SOCKET_PATH);
        delete(DEFAULT_DISP_SOCKET_PATH);

        delete(logPath);
        File log = mkfile(logPath);

        log(R.string.servicestart);
        setupLogUpdater(log).start();

        int ret = main(confPath, getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
    }

    @NonNull
    private String getLogPath (@NonNull String confPath) {
        try (BufferedReader confReader = new BufferedReader(new FileReader(confPath))) {
            Pattern pattern = Pattern.compile("dispatcher.DEBUG \"(.*)\".*");
            for (String line = confReader.readLine(); line != null; line = confReader.readLine()) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            log(R.string.serviceexceptioninfo, e);
        }
        return DEFAULT_LOG_PATH;
    }

    public native int main(String confFileName, String workingDir);
}
