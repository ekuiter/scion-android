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

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.scionlab.endhost.Config.Logger.*;

public class Logger {
    public static class LogThread extends Thread {
        private Consumer<String> outputConsumer;
        private HashMap<Pattern, Runnable> watchPatterns = new HashMap<>();
        private Pattern deletePattern;
        private long interval;
        InputStream inputStream;

        LogThread(Consumer<String> outputConsumer, Pattern deletePattern, long interval) {
            this.outputConsumer = outputConsumer;
            this.deletePattern = deletePattern;
            this.interval = interval;
        }

        public LogThread setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public LogThread watchFor(Pattern watchPattern, Runnable watchCallback) {
            this.watchPatterns.put(watchPattern, watchCallback);
            return this;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        String _line = deletePattern.matcher(line).replaceAll("");
                        watchPatterns.entrySet().forEach(e -> {
                            if (e.getKey().matcher(_line).matches())
                                e.getValue().run();
                        });
                        outputConsumer.accept(_line);
                    }
                    Thread.sleep(interval);
                }
            } catch (InterruptedIOException | InterruptedException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static LogThread createLogThread(String tag) {
        return new Logger.LogThread(line -> Log.i(tag, line), DELETE_PATTERN, UPDATE_INTERVAL);
    }

    public static LogThread createLogThread(String tag, InputStream inputStream) {
        return createLogThread(tag).setInputStream(inputStream);
    }
}
