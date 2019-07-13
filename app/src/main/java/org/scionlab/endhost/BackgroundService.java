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

import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Path;
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
        notifB = new Notification.Builder(this, MainActivity.SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notifM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        res = getResources();
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
        notifB.setStyle(
                new Notification.BigTextStyle()
                        .bigText(notifLog.toString())
                        .setBigContentTitle(res.getString(R.string.servicetitle, tagHead, tagTail))
        );
        notifM.notify(getNotificationId(), notifB.build());
    }

    @NonNull
    protected File mkdir(@NonNull Path directory) {
        File dir = getFilesDir().toPath().resolve(directory).toFile();
        boolean existed = dir.exists();
        boolean success = dir.mkdirs();
        log(R.string.servicemkdir, dir.getAbsolutePath(), existed, success, dir.exists());
        return dir;
    }

    @NonNull
    protected File delete(@NonNull Path file) {
        File f = getFilesDir().toPath().resolve(file).toFile();
        boolean existed = f.exists();
        boolean success = f.delete();
        log(R.string.servicedelete, f.getAbsolutePath(), existed, success, f.exists());
        return f;
    }
}
