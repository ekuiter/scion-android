package org.scionlab.endhost;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import sciond.Sciond;

public class SciondService extends BackgroundService {

    public static final String PARAM_CONFIG_PATH = SciondService.class.getCanonicalName() + ".CONFIG_PATH";
    private static final int NID = 2;
    private static final String TAG = "sciond";
    private static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]+]\\s+");
    private static final String CONF_FILE_NAME = "sciond.toml";

    public SciondService() {
        super("SciondService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        super.onHandleIntent(intent);
        String confPath = intent.getStringExtra(PARAM_CONFIG_PATH);
        if (confPath == null) {
            die(R.string.servicenoconf);
            return;
        }

        log(R.string.servicesetup);

        Path confDir;
        try {
            confDir = deleteRecursively(Paths.get("endhost"));
        } catch (IOException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        }
        if (Files.exists(confDir)) {
            die(R.string.servicenocleanup, confDir.toString());
            return;
        }
        try {
            confDir = copyRecursively(Paths.get(confPath), confDir);
        } catch (IOException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        }
        Path confFile = confDir.resolve(CONF_FILE_NAME);
        if (!Files.exists(confFile)) {
            die(R.string.servicefilenotfound, confFile.getFileName().toString(), confPath);
            return;
        }

        Path reliable = Paths.get("run/shm/sciond/default.sock");
        Path unix = Paths.get("run/shm/sciond/default.unix");
        Path logFile = Paths.get("logs/sciond.log");
        Path trustDBConnection = Paths.get("gen-cache/sciond.trust.db");
        try {
            Map<String, Object> conf = new Toml().read(new FileInputStream(confFile.toFile())).toMap();
            Map<String, Object> general = (Map<String, Object>) conf.get("general");
            if (general == null) {
                general = new HashMap<>();
                conf.put("general", general);
            }
            general.put("ConfigDir", confDir.toString());
            // Depends on DISPATCHER_DIR and DEFAULT_DISPATCHER_ID from CMakeLists.txt
            general.put("DispatcherPath", "run/shm/dispatcher/default.sock");
            Map<String, Object> sd = (Map<String, Object>) conf.get("sd");
            if (sd == null) {
                sd = new HashMap<>();
                conf.put("sd", sd);
            }
            reliable = Optional.ofNullable((String) sd.get("Reliable"))
                    .map(Paths::get).orElse(reliable);
            if (reliable.isAbsolute()) {
                reliable = reliable.getRoot().relativize(reliable);
            }
            sd.put("Reliable", reliable.toString());
            unix = Optional.ofNullable((String) sd.get("Unix"))
                    .map(Paths::get).orElse(unix);
            if (unix.isAbsolute()) {
                unix = unix.getRoot().relativize(unix);
            }
            sd.put("Unix", unix.toString());
            Map<String, Object> logging = (Map<String, Object>) conf.get("logging");
            if (logging == null) {
                logging = new HashMap<>();
                conf.put("logging", logging);
            }
            Map<String, Object> file = (Map<String, Object>) logging.get("file");
            if (file == null) {
                file = new HashMap<>();
                logging.put("file", file);
            }
            logFile = Optional.ofNullable((String) file.get("Path"))
                    .map(Paths::get).orElse(logFile);
            file.put("Path", logFile.toString());
            Map<String, Object> trustDB = (Map<String, Object>) conf.get("TrustDB");
            if (trustDB == null) {
                trustDB = new HashMap<>();
                conf.put("TrustDB", trustDB);
            }
            trustDBConnection = Optional.ofNullable((String) trustDB.get("Connection"))
                    .map(Paths::get).orElse(trustDBConnection);
            trustDB.put("Connection", trustDBConnection.toString());
            new TomlWriter().write(conf, confFile.toFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        } catch (IOException e) {
            e.printStackTrace();
            die(R.string.serviceexception, e.getLocalizedMessage());
            return;
        }

        mkdir(reliable.getParent());
        delete(reliable);
        mkdir(unix.getParent());
        delete(unix);
        mkdir(logFile.getParent());
        logFile = mkfile(delete(logFile));
        mkdir(trustDBConnection.getParent());

        log(R.string.servicestart);
        setupLogUpdater(logFile).start();

        long ret = Sciond.main(commandLine("-config", confFile.toString()), "", getFilesDir().getAbsolutePath());
        die(R.string.servicereturn, ret);
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

    @NonNull
    @Override
    protected Pattern getLogDeleter() {
        return LOG_DELETER_PATTERN;
    }
}
