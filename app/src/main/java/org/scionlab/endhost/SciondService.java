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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class SciondService extends BackgroundService {
    public static String DEFAULT_SCIOND_SOCKET_PATH;
    public static String DEFAULT_SCIOND_UNIX_PATH;
    public static String SCIOND_SOCKET_PATH = DEFAULT_SCIOND_SOCKET_PATH;
    public static String SCIOND_UNIX_PATH = DEFAULT_SCIOND_UNIX_PATH;
    public static final String PARAM_CONFIG_PATH = SciondService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 2;
    private static final String TAG = "sciond";
    private static final String LOG_PATH = "logs/sciond.log";
    private static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]+]\\s+");

    public SciondService() {
        super("SciondService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DEFAULT_SCIOND_SOCKET_PATH = getFilesDir() + "/sciond.sock";
        DEFAULT_SCIOND_UNIX_PATH = DEFAULT_SCIOND_SOCKET_PATH.replaceFirst("^(.*\\.)sock$", "\1unix");
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
        Optional<File> confFile = findSciondToml(this);
        if (!confFile.map(File::isFile).orElse(false)) {
            die(R.string.servicefilenotfound, confFile.map(File::getName).orElse("s*d.toml"), confPath);
            return;
        }

        String reliable = DEFAULT_SCIOND_SOCKET_PATH;
        String unix = DEFAULT_SCIOND_UNIX_PATH;
        String logFile = new File(getExternalFilesDir(null), LOG_PATH).getAbsolutePath();
        String trustDBConnection = "gen-cache/sciond.trust.db";
        String pathDBConnection = "gen-cache/sciond.path.db";
        try {
            Map<String, Object> conf = new Toml().read(new FileInputStream(confFile.get())).toMap();
            Map<String, Object> general = (Map<String, Object>) conf.get("general");
            if (general == null) {
                general = new HashMap<>();
                conf.put("general", general);
            }
            general.put("ConfigDir", confDir);
            general.put("DispatcherPath", DispatcherService.SOCKET_PATH);
            Map<String, Object> sd = (Map<String, Object>) conf.get("sd");
            if (sd == null) {
                sd = new HashMap<>();
                conf.put("sd", sd);
            }
            Map<String, Object> pathDB = (Map<String, Object>) sd.get("PathDB");
            if (pathDB == null) {
                pathDB = new HashMap<>();
                sd.put("PathDB", pathDB);
            }
            pathDBConnection = Optional.ofNullable((String) pathDB.get("Connection")).orElse(pathDBConnection);
            pathDB.put("Connection", pathDBConnection);
            reliable = Optional.ofNullable((String) sd.get("Reliable")).orElse(reliable);
            reliable = reliable.replaceFirst("^/+", "");
            sd.put("Reliable", reliable);
            SCIOND_SOCKET_PATH = reliable;
            unix = Optional.ofNullable((String) sd.get("Unix")).orElse(unix);
            unix = unix.replaceFirst("^/+", "");
            sd.put("Unix", unix);
            SCIOND_UNIX_PATH = unix;
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
                trustDB = (Map<String, Object>) conf.get("trustDB");
                if (trustDB == null) {
                    trustDB = new HashMap<>();
                    conf.put("TrustDB", trustDB);
                }
            }
            trustDBConnection = Optional.ofNullable((String) trustDB.get("Connection")).orElse(trustDBConnection);
            trustDB.put("Connection", trustDBConnection);
            new TomlWriter().write(conf, confFile.get());
        } catch (IOException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        }

        mkfile(trustDBConnection);
        mkfile(pathDBConnection);
        mkfile(reliable);
        delete(reliable);
        mkfile(unix);
        delete(unix);
        mkfile(logFile);
        delete(logFile);
        File log = mkfile(logFile);

        log(R.string.servicestart);
        setupLogUpdater(log).start();

        int ret = ScionBinary.run(getApplicationContext(), "sciond", "-lib_env_config", confFile.get().getAbsolutePath(), "", getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
    }

    static Optional<File> findSciondToml(@NonNull Context ctx) {
        final File dir = new File(ctx.getFilesDir(), "endhost");
        for (final File child : dir.listFiles()) {
            if (child.isFile() && child.getName().matches("^s.*d\\.toml$")) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
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
