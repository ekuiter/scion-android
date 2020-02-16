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

public class ScionService extends Service {
    private static final String TAG = "ScionService";
    private static final int NOTIFICATION_ID  = 1;
    private Thread dispatcherThread, daemonThread;

    public ScionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "starting SCION service");

        // make this a foreground service, decreasing the probability that Android arbitrarily kills this service
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_scion_logo);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        dispatcherThread = new DispatcherThread(this);
        daemonThread = new DaemonThread(this);
        dispatcherThread.start();
        daemonThread.start();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "stopping SCION service");
        dispatcherThread.interrupt();
        daemonThread.interrupt();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
