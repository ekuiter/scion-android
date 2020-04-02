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

import static org.scionlab.scion.as.Config.SensorFetcher.*;

class SensorFetcher extends Component {
    @Override
    Class[] dependsOn() {
        return new Class[]{Scmp.class};
    }

    @Override
    boolean prepare() {
        return true;
    }

    @Override
    void run() {
        process.addEnvironmentVariable(DISPATCHER_SOCKET_ENV, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH))
                .addArgument(BINARY_FLAG)
                .addArgument(SERVER_FLAG, "17-ffaa:0:1102,[192.33.93.177]:42003") // TODO: make this configurable from user interface
                .run();

        /* example output in logcat:
            2020-03-26 13:24:53.203 23595-23937/org.scionlab.scion I/SensorFetcher: starting component
            2020-03-26 13:24:53.208 23595-24010/org.scionlab.scion I/SensorFetcher: waiting until component may run
            2020-03-26 13:25:06.480 23595-24010/org.scionlab.scion I/SensorFetcher: done waiting for component
            2020-03-26 13:25:06.486 23595-24010/org.scionlab.scion I/SensorFetcher: /data/app/org.scionlab.scion-1/lib/arm64/libscion-scionlab.so sensorfetcher -scion-android_dispatcher /data/user/0/org.scionlab.scion/files/dispatcher.sock -scion-android_sciond /data/user/0/org.scionlab.scion/files/daemon.reliable.sock -scion-android_c 19-ffaa:1:cf4,[127.0.0.1] -scion-android_s 17-ffaa:0:1102,[192.33.93.177]:42003
            2020-03-26 13:25:06.569 23595-24417/org.scionlab.scion I/SensorFetcher: t=2020-03-26T12:25:06+0000 lvl=dbug msg="Registered with dispatcher" addr="19-ffaa:1:cf4,[127.0.0.1]:1027 (UDP)"
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: 2020/03/26 12:25:03
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: Temperature: 21.5
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: Motion: 0
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: Illuminance: 1470.7
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: UV Light: 1
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: CO2: 398
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: Sound intensity: 2
            2020-03-26 13:25:06.697 23595-24417/org.scionlab.scion I/SensorFetcher: Humidity: 42.98
            2020-03-26 13:25:06.705 23595-24010/org.scionlab.scion I/SensorFetcher: SCION process exited with 0
            2020-03-26 13:25:06.705 23595-24010/org.scionlab.scion I/SensorFetcher: component has stopped */
    }
}
