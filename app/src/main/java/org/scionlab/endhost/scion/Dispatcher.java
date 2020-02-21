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

/**
 * Dispatches requests/responses from other SCION components to the outside world and vice-versa.
 */
public class Dispatcher extends Component {
    private final String configPath = Config.Dispatcher.CONFIG_PATH;

    @Override
    protected String getTag() {
        return "Dispatcher";
    }

    @Override
    boolean prepare() {
        final String socketPath = Config.Dispatcher.SOCKET_PATH;
        final String logPath = Config.Dispatcher.LOG_PATH;

        storage.prepareFile(socketPath);
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(Config.Dispatcher.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(socketPath),
                storage.getAbsolutePath(logPath),
                Config.Dispatcher.LOG_LEVEL));
        setupLogThread(logPath, Config.Dispatcher.WATCH_PATTERN);

        return true;
    }

    @Override
    void run() {
        Binary.runDispatcher(getContext(),
                Logger.createLogThread(getTag()),
                storage.getAbsolutePath(configPath));
    }
}
