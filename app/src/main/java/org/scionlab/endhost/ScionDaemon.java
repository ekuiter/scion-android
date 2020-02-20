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

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

public class ScionDaemon extends Thread {
    private static final String TAG = "ScionDaemon";
    private Context context;
    private String configDirectorySourcePath;

    ScionDaemon(Context context, String configDirectorySourcePath) {
        this.context = context;
        this.configDirectorySourcePath = configDirectorySourcePath;
    }

    @Override
    public void run() {
        final Storage storage = Storage.from(context);
        final String configDirectoryPath = ScionConfig.Daemon.CONFIG_DIRECTORY_PATH;
        final String reliableSocketPath = ScionConfig.Daemon.RELIABLE_SOCKET_PATH;
        final String unixSocketPath = ScionConfig.Daemon.UNIX_SOCKET_PATH;
        final String logPath = ScionConfig.Daemon.LOG_PATH;

        // copy configuration folder provided by the user and find daemon configuration file
        storage.deleteFileOrDirectory(configDirectoryPath);
        storage.copyFileOrDirectory(new File(configDirectorySourcePath), configDirectoryPath);
        Optional<String> _configPath = storage.findFirstMatchingFileInDirectory(
                configDirectoryPath, ScionConfig.Daemon.CONFIG_PATH_REGEX);
        if (!_configPath.isPresent()) {
            Log.e(TAG, "could not find SCION daemon configuration file sciond.toml or sd.toml");
            return;
        }
        String configPath = _configPath.get();
        Toml config = new Toml().read(storage.getInputStream(configPath));
        String publicAddress = config.getString(ScionConfig.Daemon.CONFIG_PUBLIC_TOML_PATH);
        // TODO: for now, we assume the topology file is present at the correct location and has the right values
        // TODO: import certs

        // prepare files
        storage.deleteFileOrDirectory(reliableSocketPath);
        storage.deleteFileOrDirectory(unixSocketPath);
        storage.deleteFileOrDirectory(logPath);
        storage.createFile(logPath);

        // instantiate configuration file template
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(ScionConfig.Daemon.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(configDirectoryPath),
                storage.getAbsolutePath(logPath),
                ScionConfig.Daemon.LOG_LEVEL,
                publicAddress,
                storage.getAbsolutePath(reliableSocketPath),
                storage.getAbsolutePath(unixSocketPath),
                storage.getAbsolutePath(ScionConfig.Daemon.PATH_DATABASE_PATH),
                storage.getAbsolutePath(ScionConfig.Daemon.TRUST_DATABASE_PATH)));

        // prepare logger for stdout, stderr and the log file
        Supplier<Logger.ConsumeOutputThread> consumeOutputThreadSupplier =
                () -> new Logger.ConsumeOutputThread(
                        line -> Log.i(TAG, line),
                        ScionConfig.Log.DELETER_PATTERN,
                        ScionConfig.Log.UPDATE_INTERVAL);

        // tail log file and run daemon
        consumeOutputThreadSupplier.get().setInputStream(storage.getInputStream(logPath)).start();
        ScionBinary.runDaemon(context,
                consumeOutputThreadSupplier.get(),
                storage.getAbsolutePath(configPath),
                storage.getAbsolutePath(ScionConfig.Dispatcher.SOCKET_PATH));
    }
}
