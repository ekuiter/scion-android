package org.scionlab.endhost;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;


public class ScionProcess {
    private static final String BINARY = "libscion-android.so";
    private static final String TAG = "ScionProcess";

    public static int run(Context applicationContext, String... args) {
        Log.i(TAG, "running SCION process");
        String dataDir = applicationContext.getApplicationInfo().dataDir;
        String nativeLibraryDir = applicationContext.getApplicationInfo().nativeLibraryDir;
        Log.i(TAG, "data dir: " + dataDir);
        Log.i(TAG, "native library dir: " + nativeLibraryDir);

        ArrayList<String> command = new ArrayList<>();
        command.add("./" + BINARY);
        command.addAll(Arrays.asList(args));

        try {
            java.lang.Process process = new ProcessBuilder()
                    .directory(new File(nativeLibraryDir))
                    .command(command)
                    .redirectErrorStream(true)
                    .start();
            Log.i(TAG, "process is running");
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = input.readLine()) != null)
                Log.e(TAG, line);
            input.close();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Exception while running: " + e.toString());
            return 1;
        }
    }
}
