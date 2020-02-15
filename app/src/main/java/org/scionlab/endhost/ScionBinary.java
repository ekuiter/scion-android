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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScionBinary {
    private static final String TAG = "ScionBinary";
    private static final String BINARY_PATH = "libscion-android.so";
    private static final String DISPATCHER_FLAG = "godispatcher";
    private static final String SCIOND_FLAG = "sciond";
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

    public static int runProcess(@NonNull Context context, Consumer<String> outputConsumer, @NonNull Map<String, String> env, @NonNull String... args) {
        if (outputConsumer == null)
            outputConsumer = line -> Log.i(TAG, line);
        Process process = startProcess(context, env, args);
        int ret;

        if (process == null)
            ret = -1;
        else {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                for (String line = br.readLine(); line != null; line = br.readLine())
                    outputConsumer.accept(line);
                ret = process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                ret = -1;
            }
        }

        Log.i(TAG, "SCION exited with " + ret);
        return ret;
    }

    public static int runDispatcher(Context context, Consumer<String> outputConsumer, String configPath) {
        return runProcess(context, outputConsumer, new HashMap<>(), DISPATCHER_FLAG, ScionBinary.CONFIG_FLAG, configPath);
    }
}
