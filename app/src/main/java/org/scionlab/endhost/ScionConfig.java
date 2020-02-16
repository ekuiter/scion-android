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

class ScionConfig {
    static class Binary {
        static final String PATH = "libscion-android.so"; // file name of SCION binary as located in jniLabs subdirectories
        static final String DISPATCHER_FLAG = "godispatcher"; // value of binary's first argument to run the dispatcher
        static final String DAEMON_FLAG = "sciond"; // value of binary's first argument to run the daemon
        static final String CONFIG_FLAG = "-lib_env_config"; // flag that specifies a configuration file
        static final String DISPATCHER_SOCKET_ENV = "DISPATCHER_SOCKET"; // environment variable that specifies the dispatcher socket
    }

    static class Dispatcher {
        static final String CONFIG_TEMPLATE_PATH = "dispatcher.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/dispatcher.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/dispatcher.log"; // path to log file
        static final String SOCKET_PATH = "INTERNAL/dispatcher.sock"; // path to socket
        static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]{4}] "); // trims information from log output
        static final long LOG_UPDATE_INTERVAL = 1000; // how often to poll the log file for updates
    }

    static class Daemon {
        static final String CONFIG_TEMPLATE_PATH = "daemon.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_DIRECTORY_PATH = "EXTERNAL/endhost"; // path to configuration directory created in external storage
        static final String CONFIG_PATH_REGEX = "^s.*d\\.toml$"; // regex for configuration file located in configuration directory
        static final String LOG_PATH = "EXTERNAL/daemon.log"; // path to log file created in external storage
        static final String RELIABLE_SOCKET_PATH = "INTERNAL/daemon.reliable.sock"; // path to reliable socket created in internal storage
        static final String UNIX_SOCKET_PATH = "INTERNAL/daemon.unix.sock"; // path to UNIX socket created in internal storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/daemon.trust.db"; // path to trust SQLite database created in external storage
        static final String PATH_DATABASE_PATH = "EXTERNAL/daemon.path.db"; // path to path SQLite database created in external storage
        static final Pattern LOG_DELETER_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} \\[[A-Z]{4}] "); // trims information from log output
        static final long LOG_UPDATE_INTERVAL = 1000; // how often to poll the log file for updates
    }
}
