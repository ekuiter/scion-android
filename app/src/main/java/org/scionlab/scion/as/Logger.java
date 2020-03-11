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

import android.util.Log;

import androidx.annotation.NonNull;

import org.scionlab.scion.UncaughtExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import timber.log.Timber;

import static org.scionlab.scion.as.Config.Logger.*;

public class Logger {
    static class LogThread extends Thread {
        private Consumer<String> outputConsumer;
        private HashMap<Pattern, Runnable> watchPatterns = new HashMap<>();
        private Pattern deletePattern;
        private long interval;
        InputStream inputStream;

        LogThread(Consumer<String> outputConsumer, Pattern deletePattern, long interval,
                  org.scionlab.scion.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.outputConsumer = outputConsumer;
            this.deletePattern = deletePattern;
            this.interval = interval;
            setUncaughtExceptionHandler(uncaughtExceptionHandler);
        }

        LogThread setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        LogThread watchFor(Pattern watchPattern, Runnable watchCallback) {
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

    public enum LogLevel {
        TRACE(0, TRACE_PREFIX),
        DEBUG(1, DEBUG_PREFIX),
        INFO(2, INFO_PREFIX),
        WARN(3, WARN_PREFIX),
        ERROR(4, ERROR_PREFIX),
        CRIT(5, CRIT_PREFIX);

        private int value;
        private String prefix;

        LogLevel(int value, String prefix) {
            this.value = value;
            this.prefix = prefix;
        }

        public int getValue() {
            return value;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public static class Tree extends Timber.DebugTree {
        private BiConsumer<String, String> outputConsumer;
        private LogLevel logLevel = DEFAULT_LOG_LEVEL;
        private int messageLogLevel = DEFAULT_LINE_LOG_LEVEL.getValue();

        public Tree(BiConsumer<String, String> outputConsumer) {
            this.outputConsumer = outputConsumer;
        }

        public LogLevel getLogLevel(LogLevel logLevel) {
            return logLevel;
        }

        public void setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
        }

        @Override
        protected void log(int priority, String tag, @NonNull String message, Throwable t) {
            // assuming Log.DEBUG corresponds exactly to the SCION output (see below)
            if (priority == Log.DEBUG && !message.startsWith(SKIP_LINE_PREFIX))
                messageLogLevel = Stream.of(LogLevel.values())
                        .filter(e -> message.startsWith(e.getPrefix()))
                        .findFirst().map(LogLevel::getValue).orElse(DEFAULT_LINE_LOG_LEVEL.getValue());

            // all SCION output is logged as Log.DEBUG, this output is filtered
            // according to the log level. All other messages (i.e., from the app),
            // are logged ignoring the log level.
            if (priority > Log.DEBUG || logLevel.getValue() <= messageLogLevel) {
                // log with at least Log.INFO because Logcat tends to ignore DEBUG messages
                super.log(Math.max(Log.INFO, priority), tag, message, t);
                outputConsumer.accept(tag, message);
            }
        }
    }

    static LogThread createLogThread(String tag, UncaughtExceptionHandler uncaughtExceptionHandler) {
        // log all tailed files and processes as Log.DEBUG
        return new Logger.LogThread(line -> Timber.tag(tag).d(line),
                DELETE_PATTERN, UPDATE_INTERVAL, uncaughtExceptionHandler);
    }

    static LogThread createLogThread(String tag, UncaughtExceptionHandler uncaughtExceptionHandler, InputStream inputStream) {
        return createLogThread(tag, uncaughtExceptionHandler).setInputStream(inputStream);
    }
}
