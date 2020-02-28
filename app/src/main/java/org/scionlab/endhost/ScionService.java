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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import org.scionlab.endhost.scion.Scion;

import java.util.stream.Collectors;

import timber.log.Timber;

public class ScionService extends Service {
    private static final int NOTIFICATION_ID  = 1;
    private static final String NOTIFICATION_CHANNEL = ScionService.class.getCanonicalName() + ".NOTIFICATION_CHANNEL";
    public static final String VERSION = ScionService.class.getCanonicalName() + ".VERSION";
    public static final String SCIONLAB_ARCHIVE_FILE = ScionService.class.getCanonicalName() + ".SCIONLAB_ARCHIVE_FILE";
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Handler handler;
    private Scion scion;
    private static Scion.State state;

    public static Scion.State getState() {
        return state;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotification();
        HandlerThread handlerThread = new HandlerThread("ScionService");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);
        scion = new Scion(this, (state, componentState) -> {
            ScionService.state = state;
            updateUserInterface(state);
            notify(state, "SCION: " + state + "\n" +
                    componentState.entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("\n")));
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null || scion.getState() != Scion.State.STOPPED)
            return ret;

        final Scion.Version version = (Scion.Version) intent.getSerializableExtra(VERSION);
        if (version == null) {
            Timber.e("no SCION version given");
            return ret;
        }

        final String scionlabArchiveFile = intent.getStringExtra(SCIONLAB_ARCHIVE_FILE);
        if (scionlabArchiveFile == null) {
            Timber.e("no scionlab archive file given");
            return ret;
        }

        handler.post(() -> {
            // make this a foreground service, decreasing the probability that Android arbitrarily kills this service
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
            scion.start(version, scionlabArchiveFile);
        });

        return ret;
    }

    @Override
    public void onDestroy() {
        if (scion.getState() == Scion.State.STOPPED)
            return;

        handler.post(() -> {
            scion.stop();
            stopForeground(STOP_FOREGROUND_REMOVE);
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateUserInterface(Scion.State state) {
        sendBroadcast(new Intent(MainActivity.UPDATE_USER_INTERFACE)
                .putExtra(MainActivity.SCION_STATE, state));
    }

    private void setupNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL, getString(R.string.servicechannel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.servicechannel_description));
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_scion_logo)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
    }

    private void notify(Scion.State state, String text) {
        if (state != Scion.State.STOPPED) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
