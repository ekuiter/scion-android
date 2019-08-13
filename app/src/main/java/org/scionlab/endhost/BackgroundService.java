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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BackgroundService extends IntentService {
    private Notification.Builder notifB;
    private NotificationManager notifM;
    private StringBuffer notifLog;
    private Resources res;
    private boolean runLogUpdater;

    private static final int MAX_NOTIFICATION_LENGTH = 5 * 1024;
    private static final Pattern EMPTY_LOG_DELETER_PATTERN = Pattern.compile("");

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
    protected Thread setupLogUpdater(@NonNull Path log) {
        runLogUpdater = true;
        return new Thread(() -> {
            try(BufferedReader logReader = new BufferedReader(new FileReader(log.toFile()))) {
                String line;

                while(shouldLogUpdaterRun()) {
                    while ((line = logReader.readLine()) != null) {
                        log(getLogDeleter().matcher(line).replaceAll(""));
                    }

                    updateNotification();

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

        log(line);
        updateNotification();
    }

    private void log(String line) {
        if (line == null || line.length() == 0) return;

        // write to android log
        Log.d(getTag(), line);
        notifLog.insert(0, line + '\n');
    }

    private void updateNotification() {
        notifLog.setLength(Math.min(MAX_NOTIFICATION_LENGTH, notifLog.length()));
        notifB.setStyle(new Notification.BigTextStyle().bigText(notifLog));
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
            log(R.string.serviceexceptioninfo, e);
        }
        log(R.string.servicemkdir, dir.toString(), existed, success, Files.exists(dir));
        return dir;
    }

    @NonNull
    protected Path mkfile(@NonNull Path filePath) {
        Path f = getFilesDir().toPath().resolve(filePath);
        boolean existed = Files.exists(filePath);
        boolean success = false;
        if (!Files.exists(filePath.getParent())) {
            mkdir(filePath.getParent());
        }
        try {
            f = Files.createFile(f);
            success = true;
        } catch (FileAlreadyExistsException e) {
            // OK
        } catch (IOException e) {
            e.printStackTrace();
            log(R.string.serviceexceptioninfo, e);
        }
        log(R.string.servicemkfile, f.toString(), existed, success, Files.exists(f));
        return f;
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
            log(R.string.serviceexceptioninfo, e);
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
