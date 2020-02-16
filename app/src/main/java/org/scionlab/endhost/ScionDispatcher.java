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

import java.util.function.Supplier;

public class ScionDispatcher extends Thread {
    private static final String TAG = "ScionDispatcher";
    private Context context;

    ScionDispatcher(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        final Storage storage = Storage.from(context);
        final String configPath = ScionConfig.Dispatcher.CONFIG_PATH;
        final String logPath = ScionConfig.Dispatcher.LOG_PATH;
        final String socketPath = ScionConfig.Dispatcher.SOCKET_PATH;

        // prepare files
        storage.deleteFileOrDirectory(socketPath);
        storage.deleteFileOrDirectory(logPath);
        storage.createFile(logPath);
        storage.writeFile(configPath, String.format(
                storage.readAssetFile(ScionConfig.Dispatcher.CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(socketPath),
                storage.getAbsolutePath(logPath)));

        // prepare logger for stdout, stderr and the log file
        Supplier<Utils.ConsumeOutputThread> consumeOutputThreadSupplier =
                () -> new Utils.ConsumeOutputThread(
                        line -> Log.i(TAG, line),
                        ScionConfig.Dispatcher.LOG_DELETER_PATTERN,
                        ScionConfig.Dispatcher.LOG_UPDATE_INTERVAL);

        // tail log file and run dispatcher
        consumeOutputThreadSupplier.get().setInputStream(storage.getInputStream(logPath)).start();
        ScionBinary.runDispatcher(context,
                consumeOutputThreadSupplier.get(),
                storage.getAbsolutePath(configPath));
    }
}
