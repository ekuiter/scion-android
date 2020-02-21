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

package org.scionlab.endhost.scion;

import org.scionlab.endhost.Logger;

public class BeaconServer extends Component {
    private static final String TAG = "BeaconServer";
    private final String configPath = Config.BeaconServer.CONFIG_PATH;

    @Override
    boolean prepare() {
        final String logPath = Config.BeaconServer.LOG_PATH;

        // prepare files
        storage.deleteFileOrDirectory(logPath);
        storage.createFile(logPath);

        // instantiate configuration file template
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(Config.BeaconServer.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(Config.Daemon.CONFIG_DIRECTORY_PATH),
                storage.getAbsolutePath(logPath),
                Config.BeaconServer.LOG_LEVEL,
                storage.getAbsolutePath(Config.BeaconServer.BEACON_DATABASE_PATH),
                storage.getAbsolutePath(Config.BeaconServer.TRUST_DATABASE_PATH)));

        // tail log file
        Logger.createLogThread(TAG, storage.getInputStream(logPath))
                .watchFor(Config.BeaconServer.WATCH_PATTERN, this::setReady)
                .start();
        return true;
    }

    @Override
    boolean mayRun() {
        return componentRegistry.isReady(Daemon.class);
    }

    @Override
    void run() {
        Binary.runBeaconServer(getContext(),
                Logger.createLogThread(TAG),
                storage.getAbsolutePath(configPath),
                storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH));
    }
}
