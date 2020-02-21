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

package org.scionlab.endhost.components;

import android.content.Context;
import android.util.Log;

import com.moandjiezana.toml.Toml;

import org.scionlab.endhost.Logger;
import org.scionlab.endhost.ScionBinary;
import org.scionlab.endhost.ScionComponent;
import org.scionlab.endhost.Config;

import java.io.File;
import java.util.Optional;

/**
 * Performs requests to the SCION network and acts as an endhost.
 */
public class Daemon extends ScionComponent {
    private static final String TAG = "Daemon";
    private String configDirectorySourcePath;
    private String configPath;

    public Daemon(Context context, String configDirectorySourcePath) {
        super(context);
        this.configDirectorySourcePath = configDirectorySourcePath;
    }

    @Override
    public boolean prepare() {
        final String configDirectoryPath = Config.Daemon.CONFIG_DIRECTORY_PATH;
        final String reliableSocketPath = Config.Daemon.RELIABLE_SOCKET_PATH;
        final String unixSocketPath = Config.Daemon.UNIX_SOCKET_PATH;
        final String logPath = Config.Daemon.LOG_PATH;

        // copy configuration folder provided by the user and find daemon configuration file
        if (storage.countFilesInDirectory(new File(configDirectorySourcePath)) > Config.Daemon.CONFIG_DIRECTORY_FILE_LIMIT) {
            Log.e(TAG, "too many files in configuration directory, did you choose the right directory?");
            return false;
        }
        storage.deleteFileOrDirectory(configDirectoryPath);
        storage.copyFileOrDirectory(new File(configDirectorySourcePath), configDirectoryPath);
        Optional<String> _configPath = storage.findFirstMatchingFileInDirectory(
                configDirectoryPath, Config.Daemon.CONFIG_PATH_REGEX);
        if (!_configPath.isPresent()) {
            Log.e(TAG, "could not find SCION daemon configuration file sciond.toml or sd.toml");
            return false;
        }
        configPath = _configPath.get();
        Toml config = new Toml().read(storage.getInputStream(configPath));
        String publicAddress = config.getString(Config.Daemon.CONFIG_PUBLIC_TOML_PATH);
        // TODO: for now, we assume the topology file is present at the correct location and has the right values
        // TODO: import certs

        // prepare files
        storage.deleteFileOrDirectory(reliableSocketPath);
        storage.deleteFileOrDirectory(unixSocketPath);
        storage.deleteFileOrDirectory(logPath);
        storage.createFile(logPath);

        // instantiate configuration file template
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(Config.Daemon.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(configDirectoryPath),
                storage.getAbsolutePath(logPath),
                Config.Daemon.LOG_LEVEL,
                publicAddress,
                storage.getAbsolutePath(reliableSocketPath),
                storage.getAbsolutePath(unixSocketPath),
                storage.getAbsolutePath(Config.Daemon.PATH_DATABASE_PATH),
                storage.getAbsolutePath(Config.Daemon.TRUST_DATABASE_PATH)));

        // tail log file
        Logger.createLogThread(TAG, storage.getInputStream(logPath))
                .watchFor(Config.Daemon.WATCH_PATTERN, this::setReady)
                .start();
        return true;
    }

    @Override
    public void run() {
        ScionBinary.runDaemon(context,
                Logger.createLogThread(TAG),
                storage.getAbsolutePath(configPath),
                storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH));
    }
}
