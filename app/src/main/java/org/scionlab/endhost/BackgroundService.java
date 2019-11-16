/*
 * Copyright (C) 2019  Vera Clemens, Tom Kranz
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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BackgroundService extends IntentService {
    private NotificationCompat.Builder notifB;
    private NotificationManager notifM;
    private StringBuffer notifLog;
    private Resources res;
    private boolean runLogUpdater;
    private Intent activityResetIntent;

    private static final int MAX_NOTIFICATION_LENGTH = 5 * 1024;
    private static final Pattern EMPTY_LOG_DELETER_PATTERN = Pattern.compile("");

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        activityResetIntent = new Intent(MainActivity.ACTION_SERVICE);
        sendBroadcast(activityResetIntent);
        String tag = getTag();
        String tagHead = tag.isEmpty() ? tag : tag.substring(0, 1);
        String tagTail = tag.isEmpty() ? tag : tag.substring(1);
        res = getResources();
        notifB = new NotificationCompat.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setContentTitle(res.getString(R.string.servicetitle, tagHead, tagTail))
                .setSmallIcon(R.drawable.ic_scion_logo);
        notifM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifLog = new StringBuffer();
    }

    @Override
    @CallSuper
    protected void onHandleIntent(@Nullable Intent intent) {
        startForeground(getNotificationId(), notifB.build());
    }

    @NonNull
    protected Thread setupLogUpdater(@NonNull File log) {
        runLogUpdater = true;
        return new Thread(() -> {
            try(BufferedReader logReader = new BufferedReader(new FileReader(log))) {
                while(shouldLogUpdaterRun()) {
                    boolean logChanged = false;
                    for (String line = logReader.readLine(); line != null; line = logReader.readLine()) {
                        // Mind the order; don't short-circuit the logging!
                        logChanged = log(getLogDeleter().matcher(line).replaceAll("")) > 0 || logChanged;
                    }
                    if (logChanged) {
                        updateNotification();
                    }

                    Thread.sleep(getLogUpdateWaitTime());
                }
            } catch (IOException e) {
                e.printStackTrace();
                log(R.string.serviceexceptioninfo, e);
            } catch (InterruptedException e) {
                // Why not…
            }
        });
    }

    @NonNull
    public static String commandLine(@NonNull String... args) {
        //noinspection deprecation
        return Arrays.stream(args)
                .map(URLEncoder::encode)
                .collect(Collectors.joining("&"));
    }

    @NonNull
    public static String env(@NonNull String[]... env) {
        //noinspection deprecation
        return Arrays.stream(env)
                .filter(kv -> kv.length >= 2)
                .map(kv -> Arrays.stream(kv)
                        .map(URLEncoder::encode)
                        .collect(Collectors.joining("=")))
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

    @NonNull
    protected abstract String getTag();

    @NonNull
    protected Pattern getLogDeleter() {
        return EMPTY_LOG_DELETER_PATTERN;
    }

    protected long getLogUpdateWaitTime() {
        return 1000;
    }

    protected boolean shouldLogUpdaterRun() {
        return runLogUpdater;
    }

    protected void die(@StringRes int resID, Object... formatArgs) {
        runLogUpdater = false;
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
        String line = res.getString(
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
        notifLog.insert(0, line + '\n');
        return line.length();
    }

    private void updateNotification() {
        notifLog.setLength(Math.min(MAX_NOTIFICATION_LENGTH, notifLog.length()));
        notifB.setStyle(new NotificationCompat.BigTextStyle().bigText(notifLog));
        notifM.notify(getNotificationId(), notifB.build());
    }

    @NonNull
    protected File mkdir(@NonNull String path) {
        File dir = new File(path.startsWith("/") ? null : getFilesDir(), path);
        boolean existed = dir.exists();
        boolean success = dir.mkdirs();
        log(R.string.servicemkdir, dir.toString(), existed, success, dir.exists());
        return dir;
    }

    @NonNull
    protected File mkfile(@NonNull String filePath) {
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

    protected int delete(@NonNull String path) {
        File f = new File(path.startsWith("/") ? null : getFilesDir(), path);
        boolean existed = f.exists();
        int left = countRecursively(f) - deleteRecursively(f);
        log(R.string.servicedelete, f.toString(), existed, left, f.exists());
        return left;
    }

    protected int copy(@NonNull String source, @NonNull String target) {
        File src = new File(source.startsWith("/") ? null : getFilesDir(), source);
        File tgt = new File(target.startsWith("/") ? null : getFilesDir(), target);
        boolean existed = src.exists();
        int left = countRecursively(src) - copyRecursively(src, tgt);
        log(R.string.servicecopydir, src.getAbsolutePath(), tgt.getAbsolutePath(), existed, left, tgt.exists());
        return left;
    }

    private int countRecursively(@NonNull File file) {
        int counted = Boolean.compare(file.exists(), false);
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                counted += countRecursively(c);
            }
        }
        return counted;
    }

    private int deleteRecursively(@NonNull File file) {
        int deleted = 0;
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                deleted += deleteRecursively(c);
            }
        }
        deleted += Boolean.compare(file.delete(), false);
        return deleted;
    }

    private int copyRecursively(@NonNull File src, @NonNull File tgt) {
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
