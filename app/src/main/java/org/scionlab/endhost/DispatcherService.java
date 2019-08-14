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
    private static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]+] \\(\\d+:dispatcher:\\.\\./\\.\\./\\.\\./\\.\\./src/main/cpp/gobind-scion/c/dispatcher/dispatcher\\.c:\\d+\\)\\s+");
    // Depends on DISPATCHER_DIR and DEFAULT_DISPATCHER_ID from CMakeLists.txt
    private static final Path DEFAULT_DISP_SOCKET_PATH = Paths.get("run/shm/dispatcher/default.sock");
    private static final Path DEFAULT_LOG_PATH = Paths.get("logs/dispatcher.log");

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
        Path logPath = getLogPath(confPath);
        if (logPath == null) {
            logPath = DEFAULT_LOG_PATH;
        }

        log(R.string.servicesetup);

        Path dispSocket = DEFAULT_DISP_SOCKET_PATH;
        mkdir(dispSocket.getParent());
        delete(dispSocket);

        logPath = mkfile(delete(logPath));

        log(R.string.servicestart);
        setupLogUpdater(logPath).start();

        int ret = main(confPath, getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
    }

    private Path getLogPath (String confPath) {
        try (FileReader confFile = new FileReader(confPath)) {
            BufferedReader confReader = new BufferedReader(confFile);
            Pattern pattern = Pattern.compile("dispatcher.DEBUG \"(.*)\".*");
            String line;
            while ((line = confReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    return Paths.get(matcher.group(1));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public native int main(String confFileName, String workingDir);
}
