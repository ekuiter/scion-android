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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class Utils {
    static class ConsumeOutputThread extends Thread {
        private Consumer<String> outputConsumer;
        private Pattern deleterPattern;
        private long interval;
        InputStream inputStream;

        ConsumeOutputThread(Consumer<String> outputConsumer, Pattern deleterPattern, long interval) {
            this.outputConsumer = outputConsumer;
            this.deleterPattern = deleterPattern;
            this.interval = interval;
        }

        ConsumeOutputThread setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        ConsumeOutputThread setFile(File file) {
            try {
                this.inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return this;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    for (String line = br.readLine(); line != null; line = br.readLine())
                        outputConsumer.accept(deleterPattern.matcher(line).replaceAll(""));
                    Thread.sleep(interval);
                }
            } catch (InterruptedIOException | InterruptedException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
