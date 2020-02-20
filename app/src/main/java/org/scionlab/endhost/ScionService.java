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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.scionlab.endhost.components.Daemon;
import org.scionlab.endhost.components.Dispatcher;

public class ScionService extends Service {
    private static final String TAG = "ScionService";
    private static final int NOTIFICATION_ID  = 1;
    public static final String CONFIG_DIRECTORY_SOURCE_PATH = ScionService.class.getCanonicalName() + ".CONFIG_DIRECTORY_SOURCE_PATH";
    private boolean running = false;
    private ScionComponent dispatcher, daemon;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null || running)
            return ret;
        final String configDirectorySourcePath = intent.getStringExtra(CONFIG_DIRECTORY_SOURCE_PATH);
        if (configDirectorySourcePath == null) {
            Log.e(TAG, "no config directory source path given");
            return ret;
        }

        // TODO: make this a foreground service, decreasing the probability that Android arbitrarily kills this service
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_scion_logo);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        // start SCION components
        Log.i(TAG, "starting SCION components");
        dispatcher = new Dispatcher(this);
        daemon = new Daemon(this, configDirectorySourcePath);
        dispatcher.start();
        daemon.start();
        running = true;

        new ScionScmp(this).start();

        return ret;
    }

    @Override
    public void onDestroy() {
        if (!running)
            return;

        Log.i(TAG, "stopping SCION components");
        dispatcher.stop();
        daemon.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
