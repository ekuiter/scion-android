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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

/**
 * Utilities for handling files and directories.
 * All operations are scoped to the application's file directory (internal or external).
 * Internal storage is stored at /data and not accessible to other applications.
 * External storage, stored at /storage, is potentially available to other applications,
 * however, some security restrictions apply (e.g., Unix sockets must be stored internally).
 * In general, we try to store as much files externally as possible to facilitate debugging.
 */
class Storage {
    private Context context;
    boolean useExternalFilesDir = false;

    private Storage(Context context) {
        this.context = context;
    }

    static Storage from(Context context) {
        return new Storage(context);
    }

    static class External extends Storage {
        private External(Context context) {
            super(context);
            this.useExternalFilesDir = true;
        }

        static Storage from(Context context) {
            return new External(context);
        }
    }

    String getAbsolutePath(String path) {
        return new File(getFilesDir(context), path).getAbsolutePath();
    }

    private File getFilesDir(Context context) {
        return useExternalFilesDir
            ? context.getExternalFilesDir(null)
            : context.getFilesDir();
    }

    String readAssetFile(String path) {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(path)))) {
            StringBuilder sb = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void createApplicationDirectory(String path) {
        //noinspection ResultOfMethodCallIgnored
        new File(getFilesDir(context), path).mkdirs();
    }

    private int countFilesInDirectory(File file) {
        int counted = Boolean.compare(file.exists(), false);
        if (file.isDirectory())
            for (File c : Objects.requireNonNull(file.listFiles()))
                counted += countFilesInDirectory(c);
        return counted;
    }

    private int deleteFileOrDirectory(File file) {
        int deleted = 0;
        if (file.isDirectory())
            for (File c : Objects.requireNonNull(file.listFiles()))
                deleted += deleteFileOrDirectory(c);
        deleted += Boolean.compare(file.delete(), false);
        return deleted;
    }

    int deleteFileOrDirectory(String path) {
        File f = new File(getFilesDir(context), path);
        return countFilesInDirectory(f) - deleteFileOrDirectory(f);
    }

    File createFile(String path) {
        File f = new File(getFilesDir(context), path);
        if (f.getParentFile() != null && !f.getParentFile().exists())
            createApplicationDirectory(Objects.requireNonNull(f.getParent()));

        try {
            //noinspection ResultOfMethodCallIgnored
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    File writeFile(String path, String content) {
        deleteFileOrDirectory(path);
        File f = createFile(path);
        try {
            FileWriter writer = new FileWriter(f);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }
}
