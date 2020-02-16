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

class ScionBinary {
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

    private static int runProcess(Context context, Utils.ConsumeOutputThread consumeOutputThread, Map<String, String> env, String... args) {
        Process process = startProcess(context, env, args);
        int ret;

        if (process == null)
            ret = -1;
        else {
            // this should create a separate thread that is only used to consume each line of the
            // process' stdout/stderr stream (see Utils.outputConsumerThread)
            consumeOutputThread.setInputStream(process.getInputStream()).start();

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

    static int runDispatcher(Context context, Utils.ConsumeOutputThread consumeOutputThread, String configPath) {
        return runProcess(context, consumeOutputThread, new HashMap<>(),
                ScionConfig.Binary.DISPATCHER_FLAG, ScionConfig.Binary.CONFIG_FLAG, configPath);
    }

    static int runDaemon(Context context, Utils.ConsumeOutputThread consumeOutputThread, String configPath, String dispatcherSocketPath) {
        HashMap<String, String> env = new HashMap<>();
        env.put(ScionConfig.Binary.DISPATCHER_SOCKET_ENV, dispatcherSocketPath);
        return runProcess(context, consumeOutputThread, env,
                ScionConfig.Binary.DAEMON_FLAG, ScionConfig.Binary.CONFIG_FLAG, configPath);
    }
}
