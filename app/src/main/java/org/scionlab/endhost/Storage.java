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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilities for handling files and directories.
 * All operations are scoped to the application's file directory (internal or external).
 * Internal storage is stored at /data and not accessible to other applications.
 * External storage, stored at /storage, is potentially available to other applications,
 * however, some security restrictions apply (e.g., Unix sockets must be stored internally).
 * In general, we try to store as much files externally as possible to facilitate debugging.
 */
public class Storage {
    private Context context;

    private Storage(Context context) {
        this.context = context;
    }

    static Storage from(Context context) {
        return new Storage(context);
    }

    private File getFilesDir(Context context, String path) {
        if (path.startsWith("EXTERNAL/"))
            return context.getExternalFilesDir(null);
        if (path.startsWith("INTERNAL/"))
            return context.getFilesDir();
        throw new RuntimeException("invalid path " + path + ", please specify storage");
    }

    private File getFile(String path) {
        return new File(getFilesDir(context, path), path
                .replaceFirst("^EXTERNAL/", "")
                .replaceFirst("^INTERNAL/", ""));
    }

    public InputStream getInputStream(String path) {
        try {
            return new FileInputStream(getFile(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public OutputStream getOutputStream(String path) {
        try {
            return new FileOutputStream(getFile(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRelativePath(String path, File file) {
        String storage = path.startsWith("INTERNAL/") ? "INTERNAL/" : "EXTERNAL/";
        return storage + getFilesDir(context, path).toURI().relativize(file.toURI()).getPath();
    }

    public String getAbsolutePath(String path) {
        return getFile(path).getAbsolutePath();
    }

    private String readFile(InputStream inputStream) {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
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

    public String readFile(String path) {
        return readFile(getInputStream(path));
    }

    public String readAssetFile(String path) {
        try {
            return readFile(context.getAssets().open(path));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void createApplicationDirectory(String path) {
        //noinspection ResultOfMethodCallIgnored
        getFile(path).mkdirs();
    }

    public int countFilesInDirectory(File file) {
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

    private int copyFileOrDirectory(File src, File dst) {
        int copied = 0;
        if (src.isDirectory()) {
            copied += Boolean.compare(dst.mkdirs(), false);
            for (File c : Objects.requireNonNull(src.listFiles()))
                copied += copyFileOrDirectory(c, new File(dst, c.getName()));
        } else {
            byte[] buffer = new byte[4096];
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                for (int len = in.read(buffer); len > 0; len = in.read(buffer))
                    out.write(buffer, 0, len);
                copied++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return copied;
    }

    public void deleteFileOrDirectory(String path) {
        File f = getFile(path);
        deleteFileOrDirectory(f);
    }

    public void copyFileOrDirectory(File src, String dstPath) {
        copyFileOrDirectory(src, getFile(dstPath));
    }

    public void createFile(String path) {
        File f = getFile(path);
        if (f.getParentFile() != null && !f.getParentFile().exists())
            createApplicationDirectory(Objects.requireNonNull(f.getParent()));

        try {
            //noinspection ResultOfMethodCallIgnored
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFile(String path, String content) {
        deleteFileOrDirectory(path);
        createFile(path);
        try {
            FileWriter writer = new FileWriter(getFile(path));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<String> findFirstMatchingFileInDirectory(String path, String regex) {
        final File dir = getFile(path);
        if (!dir.isDirectory())
            return Optional.empty();

        for (final File child : Objects.requireNonNull(dir.listFiles()))
            if (child.isFile() && child.getName().matches(regex))
                return Optional.of(getRelativePath(path, child));

        return Optional.empty();
    }
}
