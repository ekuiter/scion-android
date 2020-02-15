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

import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.File;

public class DispatcherService extends BackgroundService {
    private static final int NID = 1;
    private static final String TAG = "dispatcher";
    private static final String CONFIG_TEMPLATE_PATH = "dispatcher.toml";
    private static final String CONFIG_PATH = "dispatcher.toml";
    private static final String LOG_PATH = "dispatcher.log";
    public static final String SOCKET_PATH = "dispatcher.sock";

    public DispatcherService() {
        super("DispatcherService");
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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;
        super.onHandleIntent(intent);

        log(R.string.servicesetup);

        Storage internalStorage = Storage.from(this),
                externalStorage = Storage.External.from(this);

        internalStorage.deleteFileOrDirectory(SOCKET_PATH);
        externalStorage.deleteFileOrDirectory(LOG_PATH);
        File log = externalStorage.createFile(LOG_PATH);

        File configFile = externalStorage.writeFile(CONFIG_PATH, String.format(
                internalStorage.readAssetFile(CONFIG_TEMPLATE_PATH),
                internalStorage.getAbsolutePath(SOCKET_PATH),
                externalStorage.getAbsolutePath(LOG_PATH)));

        log(R.string.servicestart);
        setupLogUpdater(log).start();
        int ret = ScionBinary.runDispatcher(
                this, line -> log(R.string.servicestring, line), configFile.getAbsolutePath());
        die(R.string.servicereturn, ret);
    }
}
