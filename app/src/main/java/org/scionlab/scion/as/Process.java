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

package org.scionlab.scion.as;

import android.content.Context;

import org.scionlab.scion.UncaughtExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import timber.log.Timber;

import static org.scionlab.scion.as.Config.Process.*;

/**
 * Runs a SCION component via the binary supplied in jniLibs.
 */
public class Process {
    private static String nativeLibraryDir;
    private String binaryPath;
    private String tag;
    private Storage storage;
    private Logger.LogThread logThread;
    private Map<String, String> environment = new HashMap<>();
    private ArrayList<String> arguments = new ArrayList<>();

    private Process(String binaryPath, String tag, Storage storage) {
        this.binaryPath = binaryPath;
        this.tag = tag;
        this.storage = storage;
        if (nativeLibraryDir == null)
            throw new RuntimeException("process class must be initialized first");
    }

    static Process from(String binaryPath, String tag, Storage storage, Logger.LogThread logThread) {
        return new Process(binaryPath, tag, storage).setLogThread(logThread);
    }

    public static Process from(String binaryPath, String tag, Storage storage, UncaughtExceptionHandler uncaughtExceptionHandler) {
        return from(binaryPath, tag, storage, Logger.createLogThread(tag, uncaughtExceptionHandler));
    }

    static void initialize(Context context) {
        if (nativeLibraryDir == null) {
            nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
            Timber.i("native SCION binary is located in %s", nativeLibraryDir);
        }
    }

    private Timber.Tree timber() {
        return Timber.tag(tag);
    }

    private Process setLogThread(Logger.LogThread logThread) {
        this.logThread = logThread;
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    Process watchFor(Pattern watchPattern, Runnable watchCallback) {
        if (logThread == null)
            throw new RuntimeException("no log thread given");
        logThread.watchFor(watchPattern, watchCallback);
        return this;
    }

    public Process addEnvironmentVariable(String key, String value) {
        environment.put(key, value);
        return this;
    }

    public Process connectToDispatcher() {
        addEnvironmentVariable(DISPATCHER_SOCKET_ENV, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH));
        return this;
    }

    public Process addArgument(String... args) {
        arguments.addAll(Arrays.asList(args));
        return this;
    }

    public Process addConfigurationFile(String path) {
        return addArgument(CONFIG_FLAG, storage.getAbsolutePath(path));
    }

    private ProcessBuilder build() {
        ArrayList<String> command = new ArrayList<>();
        command.add(nativeLibraryDir + "/" + binaryPath);
        command.addAll(arguments);

        storage.createDirectory(WORKING_DIRECTORY_PATH);
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(new File(storage.getAbsolutePath(WORKING_DIRECTORY_PATH)))
                .command(command)
                .redirectErrorStream(true);
        processBuilder.environment().putAll(environment);
        return processBuilder;
    }

    private ProcessBuilder log(ProcessBuilder processBuilder) {
        String env = environment.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
        //noinspection SimplifyStreamApiCallChains
        String invocation = String.format("%s %s", env,
                processBuilder.command().stream().collect(Collectors.joining(" "))).trim();
        timber().i(invocation);
        return processBuilder;
    }

    // Runs the SCION binary and blocks until the process exits or the thread is interrupted.
    // Thus, this should only be called from inside a (dedicated) thread.
    public void run() {
        java.lang.Process process;
        try {
            process = log(build()).start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int ret;

        if (process == null)
            ret = -1;
        else {
            // this should create a separate thread that is only used to consume each line of the
            // process' stdout/stderr stream (see Logger.LogThread)
            if (logThread != null)
                logThread.setInputStream(process.getInputStream()).start();

            // block until the process dies or the current thread is interrupted, in which case we kill the process
            try {
                ret = process.waitFor();
            } catch (InterruptedException ignored) {
                timber().i("thread was interrupted, stopping SCION process");
                process.destroy();
                ret = -1;
            }
        }

        timber().i("SCION process exited with %s", ret);
    }

    public interface InputStreamHandler {
        void handle(InputStream stream);
    }

    public void run(InputStreamHandler streamHandler) {
        java.lang.Process process;
        try {
            process = log(build()).start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int ret;

        if (process == null)
            ret = -1;
        else {
            // this should create a separate thread that is only used to consume each line of the
            // process' stdout/stderr stream (see Logger.LogThread)
            streamHandler.handle(process.getInputStream());
            //if (logThread != null)
                //logThread.setInputStream(process.getInputStream()).start();

            // block until the process dies or the current thread is interrupted, in which case we kill the process
            try {
                ret = process.waitFor();
            } catch (InterruptedException ignored) {
                timber().i("thread was interrupted, stopping SCION process");
                process.destroy();
                ret = -1;
            }
        }

        timber().i("SCION process exited with %s", ret);
    }
}
