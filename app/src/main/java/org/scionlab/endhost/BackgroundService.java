package org.scionlab.endhost;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BackgroundService extends IntentService {

    private Notification.Builder notifB;
    private NotificationManager notifM;
    private StringBuilder notifLog;
    private Resources res;

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        String tag = getTag();
        String tagHead = tag.isEmpty() ? tag : tag.substring(0, 1);
        String tagTail = tag.isEmpty() ? tag : tag.substring(1);
        res = getResources();
        notifB = new Notification.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setContentTitle(res.getString(R.string.servicetitle, tagHead, tagTail))
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notifM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    @CallSuper
    protected void onHandleIntent(@Nullable Intent intent) {
        startForeground(getNotificationId(), notifB.build());
        notifLog = new StringBuilder();
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

    protected abstract int getNotificationId();

    @NonNull
    protected abstract String getTag();

    protected void die(@StringRes int resID, Object... formatArgs) {
        log(resID, formatArgs);
        stopForeground(STOP_FOREGROUND_DETACH);
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
        Log.d(tag, line);
        if (notifLog.length() > 0) notifLog.append('\n');
        notifLog.append(line);
        if (notifLog.length() > 5 * 1024) {
            notifLog.replace(0, notifLog.length() - 5 * 1024, "");
        }
        notifB.setStyle(
                new Notification.BigTextStyle()
                        .bigText(notifLog.toString())
                        .setBigContentTitle(res.getString(R.string.servicetitle, tagHead, tagTail))
        );
        notifM.notify(getNotificationId(), notifB.build());
    }

    @NonNull
    protected Path mkdir(@NonNull Path directory) {
        Path dir = getFilesDir().toPath().resolve(directory);
        boolean existed = Files.exists(dir);
        boolean success = false;
        try {
            dir = Files.createDirectories(dir);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        log(R.string.servicemkdir, dir.toString(), existed, success, Files.exists(dir));
        return dir;
    }

    @NonNull
    protected Path delete(@NonNull Path file) {
        Path f = getFilesDir().toPath().resolve(file);
        boolean existed = Files.exists(f);
        boolean success = false;
        try {
            success = Files.deleteIfExists(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log(R.string.servicedelete, f.toString(), existed, success, Files.exists(f));
        return f;
    }

    @NonNull
    protected Path deleteRecursively(@NonNull Path directory) throws IOException {
        Path d = getFilesDir().toPath().resolve(directory);
        final int[] counters = {0, 0};
        boolean isDir = Files.isDirectory(d);
        if (isDir) {
            Files.walkFileTree(d, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException
                {
                    Files.delete(file);
                    counters[0]++;
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException
                {
                    if (e == null) {
                        Files.delete(dir);
                        counters[1]++;
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }
            });
        }
        log(R.string.servicedeletedir, d.toString(), isDir, counters[0], counters[1], Files.exists(d));
        return d;
    }

    @NonNull
    protected Path copyRecursively(@NonNull Path directory, Path target) throws IOException {
        Path d = getFilesDir().toPath().resolve(directory);
        Path t = getFilesDir().toPath().resolve(target);
        boolean isDir = Files.isDirectory(d);
        final int[] counters = {0, 0};
        if (isDir) {
            Files.walkFileTree(d, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    Path targetdir = t.resolve(d.relativize(dir));
                    try {
                        Files.copy(dir, targetdir);
                        counters[1]++;
                    } catch (FileAlreadyExistsException e) {
                        if (!Files.isDirectory(targetdir)) throw e;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.copy(file, t.resolve(d.relativize(file)));
                    counters[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        log(R.string.servicecopydir, d.toString(), t.toString(), isDir, counters[0], counters[1], Files.exists(t));
        return t;
    }
}
