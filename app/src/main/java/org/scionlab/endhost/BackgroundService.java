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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BackgroundService extends IntentService {
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private StringBuffer notificationLog;
    private Resources resources;
    private Intent activityResetIntent;

    private static final int MAX_NOTIFICATION_LENGTH = 5 * 1024;

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        activityResetIntent = new Intent(MainActivity.ACTION_SERVICE);
        sendBroadcast(activityResetIntent);
        String tag = getTag();
        String tagHead = tag.isEmpty() ? tag : tag.substring(0, 1);
        String tagTail = tag.isEmpty() ? tag : tag.substring(1);
        resources = getResources();
        notificationBuilder = new NotificationCompat.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setContentTitle(resources.getString(R.string.servicetitle, tagHead, tagTail))
                .setSmallIcon(R.drawable.ic_scion_logo);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationLog = new StringBuffer();
    }

    @Override
    @CallSuper
    protected void onHandleIntent(@Nullable Intent intent) {
        startForeground(getNotificationId(), notificationBuilder.build());
    }

    public static String commandLine(boolean addSockets, String... args) {
        List<String> additional = new LinkedList<>();
        if (addSockets && Arrays.stream(args).noneMatch("-sciond"::equals)) {
            additional.add("-sciond");
            additional.add(SciondService.SCIOND_SOCKET_PATH);
        }
        if (addSockets && Arrays.stream(args).noneMatch("-dispatcher"::equals)) {
            additional.add("-dispatcher");
            additional.add(ScionConfig.Dispatcher.SOCKET_PATH);
        }
        //noinspection deprecation
        return Stream.concat(Arrays.stream(args), additional.stream())
                .map(URLEncoder::encode)
                .collect(Collectors.joining("&"));
    }

    public BackgroundService(String name) { super(name); }

    public static boolean amIRunning(final Context ctx, final Class<?> cls) {
        final String serviceName = cls.getSimpleName();
        final ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null) {
            for (final RunningAppProcessInfo processInfo : procInfos) {
                Log.d(serviceName, processInfo.processName);
                if (processInfo.processName.endsWith(":" + serviceName)) {
                    return processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
                            || processInfo.importance == RunningAppProcessInfo.IMPORTANCE_SERVICE;
                }
            }
        }
        return false;
    }

    protected abstract int getNotificationId();

    protected abstract String getTag();

    protected void die(@StringRes int resID, Object... formatArgs) {
        log(resID, formatArgs);
        stopForeground(STOP_FOREGROUND_DETACH);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        die(R.string.servicestopped);
        super.onDestroy();
        activityResetIntent.putExtra(MainActivity.EXTRA_SERVICE_PID, Process.myPid());
        sendBroadcast(activityResetIntent);
    }

    protected void log(@StringRes int resId, Object... formatArgs) {
        String tag = getTag();
        String tagHead = tag.isEmpty() ? tag : tag.substring(0, 1);
        String tagTail = tag.isEmpty() ? tag : tag.substring(1);
        String line = resources.getString(
                resId,
                Stream.concat(
                        Stream.of(tagHead, tagTail),
                        Arrays.stream(formatArgs)
                ).toArray()
        );

        if (log(line) > 0) {
            updateNotification();
        }
    }

    private int log(String line) {
        if (line == null || line.length() == 0) return 0;

        // write to android log
        Log.d(getTag(), line);
        notificationLog.insert(0, line + '\n');
        return line.length();
    }

    private void updateNotification() {
        notificationLog.setLength(Math.min(MAX_NOTIFICATION_LENGTH, notificationLog.length()));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationLog));
        notificationManager.notify(getNotificationId(), notificationBuilder.build());
    }

    protected File mkdir(String path) {
        File dir = new File(path.startsWith("/") ? null : getFilesDir(), path);
        boolean existed = dir.exists();
        boolean success = dir.mkdirs();
        log(R.string.servicemkdir, dir.toString(), existed, success, dir.exists());
        return dir;
    }

    protected File mkfile(String filePath) {
        File f = new File(filePath.startsWith("/") ? null : getFilesDir(), filePath);
        boolean existed = f.exists();
        boolean success = false;
        if (!f.getParentFile().exists()) {
            mkdir(f.getParent());
        }
        try {
            success = f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            log(R.string.serviceexceptioninfo, e);
        }
        log(R.string.servicemkfile, f.toString(), existed, success, f.exists());
        return f;
    }

    protected int delete(String path) {
        File f = new File(path.startsWith("/") ? null : getFilesDir(), path);
        boolean existed = f.exists();
        int left = countRecursively(f) - deleteRecursively(f);
        log(R.string.servicedelete, f.toString(), existed, left, f.exists());
        return left;
    }

    protected int copy(String source, String target) {
        File src = new File(source.startsWith("/") ? null : getFilesDir(), source);
        File tgt = new File(target.startsWith("/") ? null : getFilesDir(), target);
        boolean existed = src.exists();
        int left = countRecursively(src) - copyRecursively(src, tgt);
        log(R.string.servicecopydir, src.getAbsolutePath(), tgt.getAbsolutePath(), existed, left, tgt.exists());
        return left;
    }

    private int countRecursively(File file) {
        int counted = Boolean.compare(file.exists(), false);
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                counted += countRecursively(c);
            }
        }
        return counted;
    }

    private int deleteRecursively(File file) {
        int deleted = 0;
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                deleted += deleteRecursively(c);
            }
        }
        deleted += Boolean.compare(file.delete(), false);
        return deleted;
    }

    private int copyRecursively(File src, File tgt) {
        int copied = 0;
        if (src.isDirectory()) {
            copied += Boolean.compare(tgt.mkdirs(), false);
            for (File c : src.listFiles()) {
                copied += copyRecursively(c, new File(tgt, c.getName()));
            }
        } else {
            byte[] buffer = new byte[4096];
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(tgt)) {
                for (int len = in.read(buffer); len > 0; len = in.read(buffer)) {
                    out.write(buffer, 0, len);
                }
                copied++;
            } catch (IOException e) {
                e.printStackTrace();
                log(R.string.serviceexceptioninfo, e);
            }
        }
        return copied;
    }

}
