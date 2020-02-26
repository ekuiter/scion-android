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

import static org.scionlab.endhost.scion.Logger.*;

class Config {
    static class Process {
        static final String CONFIG_FLAG = "-lib_env_config"; // flag that specifies a configuration file
        static final String DISPATCHER_SOCKET_ENV = "DISPATCHER_SOCKET"; // environment variable that specifies the dispatcher socket
    }

    static class Component {
        static final int READY_INTERVAL = 250; // how frequently (in ms) to check whether required components are ready
        static final int READY_RETRIES = 60; // when to give up and stop the component
    }

    static class Logger {
        static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.NONE; // default log level on startup
        static final LogLevel DEFAULT_LINE_LOG_LEVEL = LogLevel.INFO; // log level for lines that do not match
        static final String TRACE_PREFIX = "[TRACE] [DBUG] "; // prefix for lines with the trace log level
        static final String DEBUG_PREFIX = "[DBUG] "; // prefix for lines with the debug log level
        static final String INFO_PREFIX = "[INFO] "; // prefix for lines with the info log level
        static final String WARN_PREFIX = "[WARN] "; // prefix for lines with the warn log level
        static final String ERROR_PREFIX = "[EROR] "; // prefix for lines with the error log level
        static final String CRIT_PREFIX = "[CRIT] "; // prefix for lines with the crit log level
        static final String SKIP_LINE_PREFIX = "> "; // skip setting the message log level for lines starting with this prefix
        static final Pattern DELETE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}\\+\\d{4} "); // trims information from log output
        static final long UPDATE_INTERVAL = 1000; // how often (in ms) to poll the log file for updates
    }

    static class BeaconServer {
        static final String BINARY_FLAG = "beacon_srv"; // value of binary's first argument to run the beacon server
        static final String CONFIG_TEMPLATE_PATH = "beacon_server.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/beacon_server.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/logs/beacon_server.log"; // path to log file created in external storage
        static final String BEACON_DATABASE_PATH = "EXTERNAL/databases/beacon_server.beacon.db"; // path to beacon SQLite database created in external storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/databases/beacon_server.trust.db"; // path to trust SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*Started listening UDP.*$"); // when encountered, consider component ready
    }

    static class BorderRouter {
        static final String BINARY_FLAG = "border"; // value of binary's first argument to run the border router
        static final String CONFIG_TEMPLATE_PATH = "border_router.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/border_router.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/logs/border_router.log"; // path to log file created in external storage
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*_TODO_.*$"); // TODO: when encountered, consider component ready
        static final Pattern VPN_NOT_READY_PATTERN = Pattern.compile("^.*bind: cannot assign requested address.*$"); // occurs when VPN connection is not ready
    }

    static class CertificateServer {
        static final String BINARY_FLAG = "cert_srv"; // value of binary's first argument to run the certificate server
        static final String CONFIG_TEMPLATE_PATH = "certificate_server.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/certificate_server.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/logs/certificate_server.log"; // path to log file created in external storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/databases/certificate_server.trust.db"; // path to trust SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*Started listening UDP.*$"); // when encountered, consider component ready
    }

    static class Daemon {
        static final String BINARY_FLAG = "sciond"; // value of binary's first argument to run the daemon
        static final String CONFIG_TEMPLATE_PATH = "daemon.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/daemon.toml"; // path to configuration file
        static final String CONFIG_PATH_REGEX = "^s.*d\\.toml$"; // regex for configuration file located in configuration directory
        static final String CONFIG_PUBLIC_TOML_PATH = "sd.Public"; // TOML path for public address read from configuration file
        static final String LOG_PATH = "EXTERNAL/logs/daemon.log"; // path to log file created in external storage
        static final String RELIABLE_SOCKET_PATH = "INTERNAL/daemon.reliable.sock"; // path to reliable socket created in internal storage
        static final String UNIX_SOCKET_PATH = "INTERNAL/daemon.unix.sock"; // path to UNIX socket created in internal storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/databases/daemon.trust.db"; // path to trust SQLite database created in external storage
        static final String PATH_DATABASE_PATH = "EXTERNAL/databases/daemon.path.db"; // path to path SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*Registered with dispatcher.*$"); // when encountered, consider component ready
    }

    static class Dispatcher {
        static final String BINARY_FLAG = "godispatcher"; // value of binary's first argument to run the dispatcher
        static final String CONFIG_TEMPLATE_PATH = "dispatcher.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/dispatcher.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/logs/dispatcher.log"; // path to log file
        static final String SOCKET_PATH = "INTERNAL/dispatcher.sock"; // path to socket
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*Accepted new client.*$"); // when encountered, consider component ready
    }

    static class PathServer {
        static final String BINARY_FLAG = "path_srv"; // value of binary's first argument to run the path server
        static final String CONFIG_TEMPLATE_PATH = "path_server.toml"; // path to configuration file template, located in assets folder
        static final String CONFIG_PATH = "EXTERNAL/config/path_server.toml"; // path to configuration file
        static final String LOG_PATH = "EXTERNAL/logs/path_server.log"; // path to log file created in external storage
        static final String PATH_DATABASE_PATH = "EXTERNAL/databases/path_server.path.db"; // path to path SQLite database created in external storage
        static final String TRUST_DATABASE_PATH = "EXTERNAL/databases/path_server.trust.db"; // path to trust SQLite database created in external storage
        static final String LOG_LEVEL = "trace"; // log level passed to process (log messages are later filtered by the Logger.Tree class)
        static final Pattern READY_PATTERN = Pattern.compile("^.*Started listening UDP.*$"); // when encountered, consider component ready
    }

    static class Scmp {
        static final String BINARY_FLAG = "scmp"; // value of binary's first argument to run the scmp tool
        static final String ECHO_FLAG = "echo"; // value of scmp's first argument to run an echo request
        static final String LOCAL_FLAG = "-tools_scmp_cmn_local"; // flag that specifies the local address
        static final String REMOTE_FLAG = "-tools_scmp_cmn_remote"; // flag that specifies the remote address
        static final String DISPATCHER_SOCKET_FLAG = "-tools_scmp_dispatcher"; // flag that specifies the dispatcher socket
        static final String DAEMON_SOCKET_FLAG = "-tools_scmp_sciond"; // flag that specifies the daemon socket
    }

    static class Scion {
        static final String V0_4_0_BINARY_PATH = "libscion-v0.4.0.so"; // file name of SCION v0.4.0 binary located in jniLabs subdirectories
        static final String SCIONLAB_BINARY_PATH = "libscion-scionlab.so"; // same for the scionlab fork of SCION
        static final String CONFIG_DIRECTORY_PATH = "EXTERNAL/config/imported"; // path to configuration directory created in external storage
        static final int CONFIG_DIRECTORY_FILE_LIMIT = 50; // number of files allowed in imported directory (failsafe if the user chooses wrong)
    }
}
