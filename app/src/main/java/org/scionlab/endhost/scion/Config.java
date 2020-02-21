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

package org.scionlab.endhost.scion;

import java.util.regex.Pattern;

class Config {
    static class Binary {
        static final String PATH = "libscion-android.so"; // file name of SCION binary as located in jniLabs subdirectories
        static final String BEACON_SERVER_FLAG = "beacon_srv"; // value of binary's first argument to run the beacon server
        static final String DAEMON_FLAG = "sciond"; // value of binary's first argument to run the daemon
        static final String DISPATCHER_FLAG = "godispatcher"; // value of binary's first argument to run the dispatcher
        static final String SCMP_FLAG = "scmp"; // value of binary's first argument to run the scmp tool
        static final String CONFIG_FLAG = "-lib_env_config"; // flag that specifies a configuration file
        static final String DISPATCHER_SOCKET_ENV = "DISPATCHER_SOCKET"; // environment variable that specifies the dispatcher socket
        static final String SCMP_ECHO_FLAG = "echo"; // value of scmp's first argument to run an echo request
        static final String SCMP_LOCAL_FLAG = "-tools_scmp_cmn_local"; // flag that specifies the local address
        static final String SCMP_REMOTE_FLAG = "-tools_scmp_cmn_remote"; // flag that specifies the remote address
        static final String SCMP_DISPATCHER_SOCKET_FLAG = "-tools_scmp_dispatcher"; // flag that specifies the dispatcher socket
        static final String SCMP_DAEMON_SOCKET_FLAG = "-tools_scmp_sciond"; // flag that specifies the daemon socket
    }

    static class Component {
        static final int READY_INTERVAL = 250; // how frequently (in ms) to check whether required components are ready
        static final int READY_RETRIES = 60; // when to give up and stop the component
    }

    static class BeaconServer {
        static final String CONFIG_TEMPLATE_PATH = "beacon_server.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/beacon_server.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/beacon_server.log"; // path to log file created in external storage
        static final String BEACON_DATABASE_PATH = "EXTERNAL/beacon_server.beacon.db"; // path to path SQLite database created in external storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/beacon_server.trust.db"; // path to trust SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // dispatcher log level (one of trace, debug, info, warn, error, crit), at least info!
        static final Pattern WATCH_PATTERN = Pattern.compile(".*Started periodic tasks.*$"); // when encountered, consider component ready
    }

    static class Daemon {
        static final String CONFIG_TEMPLATE_PATH = "daemon.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_DIRECTORY_PATH = "EXTERNAL/endhost"; // path to configuration directory created in external storage
        static final int CONFIG_DIRECTORY_FILE_LIMIT = 50; // number of files allowed in imported directory (failsafe if the user chooses wrong)
        static final String CONFIG_PATH_REGEX = "^s.*d\\.toml$"; // regex for configuration file located in configuration directory
        static final String CONFIG_PUBLIC_TOML_PATH = "sd.Public"; // TOML path for public address read from configuration file
        static final String LOG_PATH = "EXTERNAL/daemon.log"; // path to log file created in external storage
        static final String RELIABLE_SOCKET_PATH = "INTERNAL/daemon.reliable.sock"; // path to reliable socket created in internal storage
        static final String UNIX_SOCKET_PATH = "INTERNAL/daemon.unix.sock"; // path to UNIX socket created in internal storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/daemon.trust.db"; // path to trust SQLite database created in external storage
        static final String PATH_DATABASE_PATH = "EXTERNAL/daemon.path.db"; // path to path SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // dispatcher log level (one of trace, debug, info, warn, error, crit), at least info!
        static final Pattern WATCH_PATTERN = Pattern.compile(".*Registered with dispatcher.*$"); // when encountered, consider component ready
    }

    static class Dispatcher {
        static final String CONFIG_TEMPLATE_PATH = "dispatcher.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/dispatcher.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/dispatcher.log"; // path to log file
        static final String SOCKET_PATH = "INTERNAL/dispatcher.sock"; // path to socket
        static final String LOG_LEVEL = "trace"; // dispatcher log level (one of trace, debug, info, warn, error, crit), at least info!
        static final Pattern WATCH_PATTERN = Pattern.compile(".*Accepted new client.*$"); // when encountered, consider component ready
    }
}
