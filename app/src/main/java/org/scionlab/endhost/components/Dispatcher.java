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

import org.scionlab.endhost.Logger;
import org.scionlab.endhost.ScionBinary;
import org.scionlab.endhost.ScionComponent;
import org.scionlab.endhost.ScionConfig;

/**
 * Dispatches requests/responses from other SCION components to the outside world and vice-versa.
 */
public class Dispatcher extends ScionComponent {
    private static final String TAG = "Dispatcher";
    private final String configPath = ScionConfig.Dispatcher.CONFIG_PATH;

    public Dispatcher(Context context) {
        super(context);
    }

    @Override
    public void prepare() {
        final String socketPath = ScionConfig.Dispatcher.SOCKET_PATH;
        final String logPath = ScionConfig.Dispatcher.LOG_PATH;

        // prepare files
        storage.deleteFileOrDirectory(socketPath);
        storage.deleteFileOrDirectory(logPath);
        storage.createFile(logPath);

        // instantiate configuration file template
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(ScionConfig.Dispatcher.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(socketPath),
                storage.getAbsolutePath(logPath),
                ScionConfig.Dispatcher.LOG_LEVEL));

        // tail log file
        Logger.createLogThread(TAG, storage.getInputStream(logPath))
                .watchFor(ScionConfig.Dispatcher.WATCH_PATTERN, this::setReady)
                .start();
    }

    @Override
    public void run() {
        ScionBinary.runDispatcher(context,
                Logger.createLogThread(TAG),
                storage.getAbsolutePath(configPath));
    }
}
