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

import timber.log.Timber;

import static org.scionlab.endhost.scion.Config.Scmp.*;

class Scmp extends Component {
    private long lastPingReceived;

    @Override
    Class[] dependsOn() {
        return new Class[]{Dispatcher.class, VPNClient.class,
                BeaconServer.class, BorderRouter.class, CertificateServer.class,
                Daemon.class, PathServer.class};
    }

    @Override
    void run() {
        process.addArgument(BINARY_FLAG)
                .addArgument(ECHO_FLAG)
                .addArgument(DISPATCHER_SOCKET_FLAG, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH))
                .addArgument(DAEMON_SOCKET_FLAG, storage.getAbsolutePath(Config.Daemon.RELIABLE_SOCKET_PATH))
                .addArgument(LOCAL_FLAG, "19-ffaa:1:cf4,[192.168.0.123]")
                .addArgument(REMOTE_FLAG, "19-ffaa:0:1301,[0.0.0.0]")
                .watchFor(READY_PATTERN, () -> {
                    lastPingReceived = System.currentTimeMillis();
                    setReady();
                })
                .run();
    }

    @Override
    boolean isHealthy() {
        return getState() == State.READY && System.currentTimeMillis() - lastPingReceived <= HEALTH_TIMEOUT;
    }
}
