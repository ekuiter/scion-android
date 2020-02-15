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

import java.util.regex.Pattern;

public class ScionConfig {
    static class Dispatcher {
        static final String CONFIG_TEMPLATE_PATH = "dispatcher.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "dispatcher.toml"; // path to configuration file created in external storage
        static final String LOG_PATH = "dispatcher.log"; // path to log file created in external storage
        static final String SOCKET_PATH = "dispatcher.sock"; // path to socket created in internal storage
        static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]{4}] ");
        static final long LOG_UPDATE_INTERVAL = 1000; // how often to poll the log file for updates
    }
}
