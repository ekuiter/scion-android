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

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class ScionBinary {
    private static final String TAG = "ScionBinary";
    private static final String BINARY_PATH = "libscion-android.so";
    private static final String DISPATCHER_FLAG = "godispatcher";
    private static final String DAEMON_FLAG = "sciond";
    private static final String CONFIG_FLAG = "-lib_env_config";

    private static Process startProcess(@NonNull Context context, @NonNull Map<String, String> env, @NonNull String... args) {
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        Log.i(TAG, "starting native SCION binary located in " + nativeLibraryDir);

        ArrayList<String> command = new ArrayList<>();
        command.add("./" + BINARY_PATH);
        command.addAll(Arrays.asList(args));
        //noinspection SimplifyStreamApiCallChains
        Log.i(TAG, command.stream().collect(Collectors.joining(" ")));

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

    private static int runProcess(@NonNull Context context, Utils.ConsumeOutputThread consumeOutputThread, @NonNull Map<String, String> env, @NonNull String... args) {
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
        return runProcess(context, consumeOutputThread, new HashMap<>(), DISPATCHER_FLAG, ScionBinary.CONFIG_FLAG, configPath);
    }
}
