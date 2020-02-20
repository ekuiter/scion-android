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

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ScionBinary {
    private static final String TAG = "ScionBinary";
    private static boolean initialized = false;

    private static Process startProcess(Context context, Map<String, String> env, String... args) {
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        if (!initialized) {
            Log.i(TAG, "native SCION binary is located in " + nativeLibraryDir);
            initialized = true;
        }

        ArrayList<String> command = new ArrayList<>();
        command.add("./" + ScionConfig.Binary.PATH);
        command.addAll(Arrays.asList(args));
        //noinspection SimplifyStreamApiCallChains

        Log.i(TAG, String.format("%s %s",
                env.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                        .collect(Collectors.joining(" ")),
                command.stream().collect(Collectors.joining(" "))).trim());

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(new File(nativeLibraryDir))
                .command(command)
                .redirectErrorStream(true);
        Map<String, String> environment = processBuilder.environment();
        environment.putAll(env);

        try {
            return processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Runs the SCION binary and blocks until the process exits or the thread is interrupted.
    // Thus, this should only be called from inside a (dedicated) thread.
    private static int runProcess(Context context, Logger.LogThread logThread, Map<String, String> env, String... args) {
        Process process = startProcess(context, env, args);
        int ret;

        if (process == null)
            ret = -1;
        else {
            // this should create a separate thread that is only used to consume each line of the
            // process' stdout/stderr stream (see Logger.outputConsumerThread)
            logThread.setInputStream(process.getInputStream()).start();

            // block until the process dies or the current thread is interrupted, in which case we kill the process
            try {
                ret = process.waitFor();
            } catch (InterruptedException e) {
                Log.i(TAG, "thread was interrupted, stopping SCION process");
                process.destroy();
                ret = -1;
            }
        }

        Log.i(TAG, "SCION process exited with " + ret);
        return ret;
    }

    public static int runDispatcher(Context context, Logger.LogThread logThread, String configPath) {
        return runProcess(context, logThread, new HashMap<>(),
                ScionConfig.Binary.DISPATCHER_FLAG, ScionConfig.Binary.CONFIG_FLAG, configPath);
    }

    public static int runDaemon(Context context, Logger.LogThread logThread, String configPath, String dispatcherSocketPath) {
        HashMap<String, String> env = new HashMap<>();
        env.put(ScionConfig.Binary.DISPATCHER_SOCKET_ENV, dispatcherSocketPath);
        return runProcess(context, logThread, env,
                ScionConfig.Binary.DAEMON_FLAG, ScionConfig.Binary.CONFIG_FLAG, configPath);
    }

    public static int runScmp(Context context, Logger.LogThread logThread, String dispatcherSocketPath, String daemonSocketPath, String localAddress, String remoteAddress) {
        return runProcess(context, logThread, new HashMap<>(),
                ScionConfig.Binary.SCMP_FLAG,
                ScionConfig.Binary.SCMP_ECHO_FLAG,
                ScionConfig.Binary.SCMP_DISPATCHER_SOCKET_FLAG,
                dispatcherSocketPath,
                ScionConfig.Binary.SCMP_DAEMON_SOCKET_FLAG,
                daemonSocketPath,
                ScionConfig.Binary.SCMP_LOCAL_FLAG,
                localAddress,
                ScionConfig.Binary.SCMP_REMOTE_FLAG,
                remoteAddress);
    }
}
