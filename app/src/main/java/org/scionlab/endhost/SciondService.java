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

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import sciond.Sciond;

public class SciondService extends BackgroundService {
    public static final String DEFAULT_SCIOND_SOCKET_PATH = "run/shm/sciond/default.sock";
    public static final String PARAM_CONFIG_PATH = SciondService.class.getCanonicalName() + ".CONFIG_PATH";
    public static final String CONF_FILE_NAME = "sciond.toml";
    private static final int NID = 2;
    private static final String TAG = "sciond";
    private static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]+]\\s+");

    public SciondService() {
        super("SciondService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        super.onHandleIntent(intent);
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) {
            die(R.string.servicenoconf);
            return;
        }

        log(R.string.servicesetup);

        String confDir = new File(getFilesDir(), "endhost").getAbsolutePath();
        if (delete(confDir) != 0 || copy(confPath, confDir) != 0) {
            die(R.string.servicenosetup, confDir);
            return;
        }
        File confFile = new File(confDir, CONF_FILE_NAME);
        if (!confFile.exists()) {
            die(R.string.servicefilenotfound, confFile.getName(), confPath);
            return;
        }

        String reliable = DEFAULT_SCIOND_SOCKET_PATH;
        String unix = "run/shm/sciond/default.unix";
        String logFile = "logs/sciond.log";
        String trustDBConnection = "gen-cache/sciond.trust.db";
        try {
            Map<String, Object> conf = new Toml().read(new FileInputStream(confFile)).toMap();
            Map<String, Object> general = (Map<String, Object>) conf.get("general");
            if (general == null) {
                general = new HashMap<>();
                conf.put("general", general);
            }
            general.put("ConfigDir", confDir);
            general.put("DispatcherPath", DispatcherService.DEFAULT_DISP_SOCKET_PATH);
            Map<String, Object> sd = (Map<String, Object>) conf.get("sd");
            if (sd == null) {
                sd = new HashMap<>();
                conf.put("sd", sd);
            }
            reliable = Optional.ofNullable((String) sd.get("Reliable")).orElse(reliable);
            if (reliable.startsWith("/")) {
                reliable = reliable.replaceFirst("/+", "");
            }
            sd.put("Reliable", reliable);
            unix = Optional.ofNullable((String) sd.get("Unix")).orElse(unix);
            if (unix.startsWith("/")) {
                unix = unix.replaceFirst("/+", "");
            }
            sd.put("Unix", unix);
            Map<String, Object> logging = (Map<String, Object>) conf.get("logging");
            if (logging == null) {
                logging = new HashMap<>();
                conf.put("logging", logging);
            }
            Map<String, Object> file = (Map<String, Object>) logging.get("file");
            if (file == null) {
                file = new HashMap<>();
                logging.put("file", file);
            }
            logFile = Optional.ofNullable((String) file.get("Path")).orElse(logFile);
            file.put("Path", logFile);
            Map<String, Object> trustDB = (Map<String, Object>) conf.get("TrustDB");
            if (trustDB == null) {
                trustDB = new HashMap<>();
                conf.put("TrustDB", trustDB);
            }
            trustDBConnection = Optional.ofNullable((String) trustDB.get("Connection")).orElse(trustDBConnection);
            trustDB.put("Connection", trustDBConnection);
            new TomlWriter().write(conf, confFile);
        } catch (IOException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        }

        mkfile(reliable);
        delete(reliable);
        mkfile(unix);
        delete(unix);
        mkfile(logFile);
        delete(logFile);
        File log = mkfile(logFile);
        mkfile(trustDBConnection);

        log(R.string.servicestart);
        setupLogUpdater(log).start();

        long ret = Sciond.main(commandLine("-config", confFile.toString()), "", getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
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
}
