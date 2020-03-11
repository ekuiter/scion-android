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

import static org.scionlab.scion.as.Config.Scmp.*;

class Scmp extends Component {
    private long lastPingReceived;
    private String localAddress, remoteAddress;

    Scmp(String localAddress, String remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    Class[] dependsOn() {
        return new Class[]{Dispatcher.class, VPNClient.class,
                BeaconServer.class, BorderRouter.class, CertificateServer.class,
                Daemon.class, PathServer.class};
    }

    @Override
    boolean isHealthy() {
        return getState() == State.READY && System.currentTimeMillis() - lastPingReceived <= HEALTH_TIMEOUT;
    }

    @Override
    void run() {
        Thread notifyStateChangeThread = new Thread(() -> {
            try {
                while (true) {
                    notifyStateChange();
                    Thread.sleep(HEALTH_TIMEOUT);
                }
            } catch (InterruptedException ignored) {
            }
        });
        notifyStateChangeThread.setUncaughtExceptionHandler(componentRegistry.getUncaughtExceptionHandler());
        notifyStateChangeThread.start();

        process.addArgument(BINARY_FLAG)
                .addArgument(ECHO_FLAG)
                .addArgument(DISPATCHER_SOCKET_FLAG, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH))
                .addArgument(DAEMON_SOCKET_FLAG, storage.getAbsolutePath(Config.Daemon.RELIABLE_SOCKET_PATH))
                .addArgument(LOCAL_FLAG, localAddress)
                .addArgument(REMOTE_FLAG, remoteAddress)
                .watchFor(READY_PATTERN, () -> {
                    lastPingReceived = System.currentTimeMillis();
                    setReady();
                })
                .run();

        notifyStateChangeThread.interrupt();
    }
}
