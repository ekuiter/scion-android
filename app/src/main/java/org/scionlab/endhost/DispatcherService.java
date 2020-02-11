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
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

public class DispatcherService extends BackgroundService {
    private static final int NID = 1;
    private static final String TAG = "dispatcher";
    private static final String CONFIG_PATH = "disp.toml";
    private static final String LOG_PATH = "disp.log";
    public static final String SOCKET_PATH = "disp.sock";

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
        super.onHandleIntent(intent);

        log(R.string.servicesetup);

        try {
            File logRoot = getExternalFilesDir(null);
            File conf = createConfigFile(logRoot);
            String socketPath = new File(getFilesDir(), SOCKET_PATH).getAbsolutePath();
            mkfile(socketPath);
            delete(socketPath);

            String logPath = new File(logRoot, LOG_PATH).getAbsolutePath();
            delete(logPath);
            File log = mkfile(logPath);

            log(R.string.servicestart);
            setupLogUpdater(log).start();
            int ret = ScionProcess.run(getApplicationContext(), "godispatcher", "-lib_env_config", conf.getAbsolutePath());
            die(R.string.servicereturn, ret);
        } catch (Exception e) {
            die(R.string.serviceexception, e.getLocalizedMessage());
        }
    }

    private File createConfigFile(File logRoot) throws IOException {
        delete(CONFIG_PATH);
        File conf = mkfile(CONFIG_PATH);
        FileWriter w = new FileWriter(conf);
        w.write(
            "[dispatcher]\n" +
                "ID = \"dispatcher\"\n" +
                "SocketFileMode = \"0777\"\n" +
                "ApplicationSocket = \"" + new File(getFilesDir(), SOCKET_PATH).getAbsolutePath() + "\"\n" +
                "\n" +
                "[metrics]\n" +
                "Prometheus = \"[127.0.0.1]:30441\"\n" +
                "\n" +
                "[logging.file]\n" +
                "Level = \"debug\"\n" +
                "MaxAge = 3\n" +
                "MaxBackups = 1\n" +
                "Path = \"" + new File(logRoot, LOG_PATH).getAbsolutePath() + "\"\n"
        );
        w.close();
        return conf;
    }
}
