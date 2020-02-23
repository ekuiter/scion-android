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

package org.scionlab.endhost.scion;

import android.content.Context;
import android.util.Log;

import org.scionlab.endhost.Logger;
import org.scionlab.endhost.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.scionlab.endhost.scion.Config.Process.*;

/**
 * Runs a SCION component via the binary supplied in jniLibs.
 */
class Process {
    private static final String TAG = "Process";
    private static String nativeLibraryDir;
    private String tag;
    private Storage storage;
    private Logger.LogThread logThread;
    private Map<String, String> environment = new HashMap<>();
    private ArrayList<String> arguments = new ArrayList<>();

    private Process(String tag, Storage storage) {
        this.tag = tag;
        this.storage = storage;
        if (nativeLibraryDir == null)
            throw new RuntimeException("process class must be initialized first");
    }

    static Process from(String tag, Storage storage) {
        return new Process(tag, storage).setLogThread(Logger.createLogThread(tag));
    }

    static void initialize(Context context) {
        if (nativeLibraryDir == null) {
            nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
            Log.i(TAG, "native SCION binary is located in " + nativeLibraryDir);
        }
    }

    private Process setLogThread(Logger.LogThread logThread) {
        this.logThread = logThread;
        return this;
    }

    Process connectToDispatcher() {
        environment.put(DISPATCHER_SOCKET_ENV, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH));
        return this;
    }

    Process addArgument(String... args) {
        arguments.addAll(Arrays.asList(args));
        return this;
    }

    Process addConfigurationFile(String path) {
        return addArgument(CONFIG_FLAG, storage.getAbsolutePath(path));
    }

    private ProcessBuilder build() {
        ArrayList<String> command = new ArrayList<>();
        command.add("./" + BINARY_PATH);
        command.addAll(arguments);

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(new File(nativeLibraryDir))
                .command(command)
                .redirectErrorStream(true);
        processBuilder.environment().putAll(environment);
        return processBuilder;
    }

    private ProcessBuilder log(ProcessBuilder processBuilder) {
        //noinspection SimplifyStreamApiCallChains
        Log.i(tag, "starting SCION process");
        Log.i(tag, String.format("%s %s",
                environment.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                        .collect(Collectors.joining(" ")),
                processBuilder.command().stream().collect(Collectors.joining(" "))).trim());
        return processBuilder;
    }

    // Runs the SCION binary and blocks until the process exits or the thread is interrupted.
    // Thus, this should only be called from inside a (dedicated) thread.
    void run() {
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
            } catch (InterruptedException e) {
                Log.i(tag, "thread was interrupted, stopping SCION process");
                process.destroy();
                ret = -1;
            }
        }

        Log.i(tag, "SCION process exited with " + ret);
    }
}
