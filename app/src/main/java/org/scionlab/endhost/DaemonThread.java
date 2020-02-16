/*
 * Copyright (C) 2019-2020 Vera Clemens, Tom Kranz, Tom Heimbrodt, Elias Kuiter
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
import android.util.Log;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class DaemonThread extends Thread {
    private static final String TAG = "DaemonThread";
    private Context context;
    private String endhostImportPath;

    DaemonThread(Context context, String endhostImportPath) {
        this.context = context;
        this.endhostImportPath = endhostImportPath;
    }

    @Override
    public void run() {
        final String configDirectoryPath = ScionConfig.Daemon.CONFIG_DIRECTORY_PATH;
        final String reliableSocketPath = ScionConfig.Daemon.RELIABLE_SOCKET_PATH;
        final String unixSocketPath = ScionConfig.Daemon.UNIX_SOCKET_PATH;
        final String logPath = ScionConfig.Daemon.LOG_PATH;
        final Storage internalStorage = Storage.from(context),
                externalStorage = Storage.External.from(context);

        externalStorage.deleteFileOrDirectory(configDirectoryPath);
        externalStorage.copyFileOrDirectory(new File(endhostImportPath), configDirectoryPath);

        Optional<String> _configPath = externalStorage.findFirstMatchingFileInDirectory(
                configDirectoryPath, ScionConfig.Daemon.CONFIG_PATH_REGEX);
        if (!_configPath.isPresent()) {
            Log.e(TAG, "could not find SCION daemon configuration file in configuration directory");
            return;
        }
        String configPath = _configPath.get();

        //String logFile = new File(getExternalFilesDir(null), LOG_PATH).getAbsolutePath();
        // externalStorage.getAbsolutePath(logPath)
        // internalStorage.getAbsolutePath(reliableSocketPath);
        // internalStorage.getAbsolutePath(unixSocketPath);

        String trustDBConnection = "gen-cache/sciond.trust.db";
        String pathDBConnection = "gen-cache/sciond.path.db";

        /*try {
            Map<String, Object> conf = new Toml().read(new FileInputStream(confFile.get())).toMap();
            Map<String, Object> general = (Map<String, Object>) conf.get("general");
            if (general == null) {
                general = new HashMap<>();
                conf.put("general", general);
            }
            general.put("ConfigDir", confDir);
            general.put("DispatcherPath", ScionConfig.Dispatcher.SOCKET_PATH);
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
        }*/

        internalStorage.deleteFileOrDirectory(reliableSocketPath);
        internalStorage.deleteFileOrDirectory(unixSocketPath);
        externalStorage.deleteFileOrDirectory(logPath);
        externalStorage.createFile(logPath);
        //mkfile(trustDBConnection);
        //mkfile(pathDBConnection);

        Supplier<Utils.ConsumeOutputThread> consumeOutputThreadSupplier =
                () -> new Utils.ConsumeOutputThread(
                        line -> Log.i(TAG, line),
                        ScionConfig.Daemon.LOG_DELETER_PATTERN,
                        ScionConfig.Daemon.LOG_UPDATE_INTERVAL);

        consumeOutputThreadSupplier.get().setFile(externalStorage.getFile(logPath)).start();
        ScionBinary.runDaemon(context,
                consumeOutputThreadSupplier.get(),
                externalStorage.getAbsolutePath(configPath),
                internalStorage.getAbsolutePath(ScionConfig.Dispatcher.SOCKET_PATH));
    }
}
