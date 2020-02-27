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
import android.content.Context;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import timber.log.Timber;

import static org.scionlab.endhost.scion.Config.Scion.*;

public class Scion {
    private final Service service;
    private final Storage storage;
    private final ComponentRegistry componentRegistry;

    public enum Version {
        V0_4_0(V0_4_0_BINARY_PATH),
        SCIONLAB(SCIONLAB_BINARY_PATH);

        private String binaryPath;

        Version(String binaryPath) {
            this.binaryPath = binaryPath;
        }

        public String getBinaryPath() {
            return binaryPath;
        }
    }

    public Scion(Service service, Consumer<Map<Class<? extends Component>, Component.State>> componentStateCallback) {
        this.service = service;
        Process.initialize(service);
        storage = Storage.from(service);
        componentRegistry = new ComponentRegistry(storage, componentStateCallback);
    }

    public boolean start(Version version, String configDirectory) {
        // copy configuration folder provided by the user
        if (storage.countFilesInDirectory(new File(configDirectory)) > CONFIG_DIRECTORY_FILE_LIMIT) {
            Timber.e("too many files in configuration directory, did you choose the right directory?");
            return false;
        }
        storage.deleteFileOrDirectory(CONFIG_DIRECTORY_PATH);
        storage.copyFileOrDirectory(new File(configDirectory), CONFIG_DIRECTORY_PATH);

        Timber.i("starting SCION components");
        componentRegistry
                .setBinaryPath(version.getBinaryPath())
                .start(new VPNClient(service))
                .start(new BeaconServer())
                .start(new BorderRouter())
                .start(new CertificateServer())
                .start(new Dispatcher())
                .start(new Daemon())
                .start(new PathServer())
                .start(new Scmp());

        return true;
    }

    public void stop() {
        Timber.i("stopping SCION components");
        componentRegistry.stopAll();
    }
}
