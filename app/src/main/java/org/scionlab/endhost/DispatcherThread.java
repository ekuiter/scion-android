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

public class DispatcherThread extends Thread {
    private static final String TAG = "DispatcherThread";
    private Context context;

    DispatcherThread(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        final String configPath = ScionConfig.Dispatcher.CONFIG_PATH;
        final String logPath = ScionConfig.Dispatcher.LOG_PATH;
        final String socketPath = ScionConfig.Dispatcher.SOCKET_PATH;
        final Storage internalStorage = Storage.from(context),
                externalStorage = Storage.External.from(context);

        internalStorage.deleteFileOrDirectory(socketPath);
        externalStorage.deleteFileOrDirectory(logPath);
        externalStorage.createFile(logPath);
        externalStorage.writeFile(configPath, String.format(
                internalStorage.readAssetFile(ScionConfig.Dispatcher.CONFIG_TEMPLATE_PATH),
                internalStorage.getAbsolutePath(socketPath),
                externalStorage.getAbsolutePath(logPath)));

        Supplier<Utils.ConsumeOutputThread> consumeOutputThreadSupplier =
                () -> new Utils.ConsumeOutputThread(
                        line -> Log.i(TAG, line),
                        ScionConfig.Dispatcher.LOG_DELETER_PATTERN,
                        ScionConfig.Dispatcher.LOG_UPDATE_INTERVAL);

        consumeOutputThreadSupplier.get().setFile(externalStorage.getFile(logPath)).start();
        ScionBinary.runDispatcher(context,
                consumeOutputThreadSupplier.get(),
                externalStorage.getAbsolutePath(configPath));
    }
}
