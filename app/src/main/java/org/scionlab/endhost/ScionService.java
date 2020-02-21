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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.scionlab.endhost.components.Daemon;
import org.scionlab.endhost.components.Dispatcher;
import org.scionlab.endhost.components.Scmp;

public class ScionService extends Service {
    private static final String TAG = "ScionService";
    private static final int NOTIFICATION_ID  = 1;
    private static final String NOTIFICATION_CHANNEL = ScionService.class.getCanonicalName() + ".NOTIFICATION_CHANNEL";
    public static final String DAEMON_CONFIG_DIRECTORY = ScionService.class.getCanonicalName() + ".DAEMON_CONFIG_DIRECTORY";
    private static boolean isRunning = false;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null || isRunning)
            return ret;

        final String daemonConfigDirectory = intent.getStringExtra(DAEMON_CONFIG_DIRECTORY);
        if (daemonConfigDirectory == null) {
            Log.e(TAG, "no daemon configuration directory given");
            return ret;
        }

        // make this a foreground service, decreasing the probability that Android arbitrarily kills this service
        setupNotification();
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        Log.i(TAG, "starting SCION components");
        ScionComponentRegistry.getInstance()
                .start(new Dispatcher(this))
                .start(new Daemon(this, daemonConfigDirectory))
                .start(new Scmp(this));

        isRunning = true;
        updateUserInterface();
        return ret;
    }

    @Override
    public void onDestroy() {
        if (!isRunning)
            return;

        Log.i(TAG, "stopping SCION components");
        ScionComponentRegistry.getInstance().stopAll();
        stopForeground(STOP_FOREGROUND_REMOVE);
        isRunning = false;
        updateUserInterface();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateUserInterface() {
        sendBroadcast(new Intent(MainActivity.UPDATE_USER_INTERFACE));
    }

    private void setupNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL, getString(R.string.servicechannel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.servicechannel_description));
            notificationManager.createNotificationChannel(channel);
        }
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_scion_logo);
    }

    private void notify(String text) {
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
