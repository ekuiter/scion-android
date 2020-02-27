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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;
import timber.log.Timber;

import static org.scionlab.endhost.scion.Config.VPNClient.*;

class VPNClient extends Component {
    private Service service;
    private IOpenVPNAPIService openVPNAPIService;
    private boolean shouldCrash, restarted;

    VPNClient(Service service) {
        this.service = service;
    }

    @Override
    void run() {
        String config = storage.readFile(Config.Scion.CONFIG_DIRECTORY_PATH + "/client.conf");
        Intent intent = new Intent(IOpenVPNAPIService.class.getName()).setPackage(PACKAGE_NAME);

        IOpenVPNStatusCallback openVPNStatusCallback = new IOpenVPNStatusCallback.Stub() {
            @Override
            public void newStatus(String uuid, String state, String message, String level) {
                if (message.isEmpty())
                    Timber.i("%s", state);
                else
                    Timber.i("%s: %s", state, message);

                switch (state) {
                    case NOPROCESS_STATE:
                        if (getState() == State.READY) {
                            Timber.e("VPN client stopped by user");
                            shouldCrash = true;
                        }
                        break;
                    case VPN_GENERATE_CONFIG:
                        if (restarted) {
                            Timber.e("VPN client restarted by user");
                            shouldCrash = true;
                        }
                        restarted = true;
                        break;
                    case CONNECTED_STATE:
                        // only when we have seen VPN_GENERATE_CONFIG, the connection has been (re-)started
                        if (restarted) {
                            String[] parts = message.split(",");
                            Timber.i("local IP = %s, remote IP = %s", parts[1], parts[2]);
                            setReady();
                        }
                        break;
                }
            }
        };

        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Timber.i("established connection to VPN service");
                openVPNAPIService = IOpenVPNAPIService.Stub.asInterface(service);

                try {
                    openVPNAPIService.registerStatusCallback(openVPNStatusCallback);
                    Timber.i("starting VPN client");
                    openVPNAPIService.startVPN(config);
                } catch (RemoteException e) {
                    Timber.e(e);
                    shouldCrash = true;
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                Timber.e("lost connection to VPN service");
                openVPNAPIService = null;
                shouldCrash = true;
            }
        };

        try {
            service.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            while (!shouldCrash)
                Thread.sleep(CRASH_INTERVAL);
        } catch (InterruptedException ignored) {
        }

        if (shouldCrash)
            Timber.e("VPN service crashed");

        if (openVPNAPIService != null) {
            try {
                Timber.i("stopping VPN client");
                openVPNAPIService.disconnect();
            } catch (RemoteException e) {
                Timber.e(e);
            }
        }

        try {
            service.unbindService(serviceConnection);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
