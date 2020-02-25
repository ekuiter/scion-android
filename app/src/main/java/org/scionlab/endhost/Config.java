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

class Config {
    static class Logger {
        static final org.scionlab.endhost.Logger.LogLevel DEFAULT_LOG_LEVEL = org.scionlab.endhost.Logger.LogLevel.NONE; // default log level on startup
        static final org.scionlab.endhost.Logger.LogLevel DEFAULT_LINE_LOG_LEVEL = org.scionlab.endhost.Logger.LogLevel.INFO; // log level for lines that do not match
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
}
