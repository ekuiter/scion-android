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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ScionDaemon extends Thread {
    private static final String TAG = "ScionDaemon";
    private Context context;
    private String endhostImportPath;

    ScionDaemon(Context context, String endhostImportPath) {
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

        // copy configuration folder provided by the user and find daemon configuration file
        externalStorage.deleteFileOrDirectory(configDirectoryPath);
        externalStorage.copyFileOrDirectory(new File(endhostImportPath), configDirectoryPath);
        Optional<String> _configPath = externalStorage.findFirstMatchingFileInDirectory(
                configDirectoryPath, ScionConfig.Daemon.CONFIG_PATH_REGEX);
        if (!_configPath.isPresent()) {
            Log.e(TAG, "could not find SCION daemon configuration file in configuration directory");
            return;
        }
        String configPath = _configPath.get();

        // prepare files
        internalStorage.deleteFileOrDirectory(reliableSocketPath);
        internalStorage.deleteFileOrDirectory(unixSocketPath);
        externalStorage.deleteFileOrDirectory(logPath);
        externalStorage.createFile(logPath);

        // update configuration file
        {
            Map<String, Object> config = new Toml().read(externalStorage.getInputStream(configPath)).toMap();

            Map<String, Object> general = (Map<String, Object>) config.get("general");
            if (general == null) {
                general = new HashMap<>();
                config.put("general", general);
            }
            general.put("ConfigDir", externalStorage.getAbsolutePath(configDirectoryPath));

            Map<String, Object> sd = (Map<String, Object>) config.get("sd");
            if (sd == null) {
                sd = new HashMap<>();
                config.put("sd", sd);
            }
            sd.put("Reliable", internalStorage.getAbsolutePath(reliableSocketPath));
            sd.put("Unix", internalStorage.getAbsolutePath(unixSocketPath));

            Map<String, Object> pathDB = (Map<String, Object>) sd.get("PathDB");
            if (pathDB == null) {
                pathDB = new HashMap<>();
                sd.put("PathDB", pathDB);
            }
            pathDB.put("Connection", externalStorage.getAbsolutePath(ScionConfig.Daemon.PATH_DATABASE_PATH));

            Map<String, Object> trustDB = (Map<String, Object>) config.get("TrustDB");
            if (trustDB == null) {
                trustDB = new HashMap<>();
                config.put("TrustDB", trustDB);
            }
            trustDB.put("Connection", externalStorage.getAbsolutePath(ScionConfig.Daemon.TRUST_DATABASE_PATH));

            Map<String, Object> logging = (Map<String, Object>) config.get("logging");
            if (logging == null) {
                logging = new HashMap<>();
                config.put("logging", logging);
            }

            Map<String, Object> file = (Map<String, Object>) logging.get("file");
            if (file == null) {
                file = new HashMap<>();
                logging.put("file", file);
            }
            file.put("Path", externalStorage.getAbsolutePath(logPath));

            try {
                new TomlWriter().write(config, externalStorage.getOutputStream(configPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // prepare logger for stdout, stderr and the log file
        Supplier<Utils.ConsumeOutputThread> consumeOutputThreadSupplier =
                () -> new Utils.ConsumeOutputThread(
                        line -> Log.i(TAG, line),
                        ScionConfig.Daemon.LOG_DELETER_PATTERN,
                        ScionConfig.Daemon.LOG_UPDATE_INTERVAL);

        // tail log file and run daemon
        consumeOutputThreadSupplier.get().setInputStream(externalStorage.getInputStream(logPath)).start();
        ScionBinary.runDaemon(context,
                consumeOutputThreadSupplier.get(),
                externalStorage.getAbsolutePath(configPath),
                internalStorage.getAbsolutePath(ScionConfig.Dispatcher.SOCKET_PATH));
    }
}
