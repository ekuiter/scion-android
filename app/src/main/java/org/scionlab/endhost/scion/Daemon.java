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

import android.util.Log;

import com.moandjiezana.toml.Toml;

import org.scionlab.endhost.Logger;

import java.util.Optional;
import static org.scionlab.endhost.scion.Config.Daemon.*;

/**
 * Performs requests to the SCION network and acts as an endhost.
 */
public class Daemon extends Component {
    private String configDirectoryPath;

    public Daemon(String configDirectoryPath) {
        this.configDirectoryPath = configDirectoryPath;
    }

    @Override
    protected String getTag() {
        return "Daemon";
    }

    @Override
    boolean prepare() {
        Optional<String> _configPath = storage.findFirstMatchingFileInDirectory(
                configDirectoryPath, CONFIG_PATH_REGEX);
        if (!_configPath.isPresent()) {
            Log.e(getTag(), "could not find SCION daemon configuration file sciond.toml or sd.toml");
            return false;
        }
        String publicAddress = new Toml().read(storage.getInputStream(_configPath.get())).getString(CONFIG_PUBLIC_TOML_PATH);
        // TODO: for now, we assume the topology file is present at the correct location and has the right values
        // TODO: import certs and keys directories

        storage.prepareFiles(RELIABLE_SOCKET_PATH, UNIX_SOCKET_PATH, TRUST_DATABASE_PATH, PATH_DATABASE_PATH);
        storage.writeFile(CONFIG_PATH, String.format(
                storage.readAssetFile(CONFIG_TEMPLATE_PATH),
                storage.getAbsolutePath(configDirectoryPath),
                storage.getAbsolutePath(LOG_PATH),
                LOG_LEVEL,
                storage.getAbsolutePath(TRUST_DATABASE_PATH),
                storage.getAbsolutePath(PATH_DATABASE_PATH),
                publicAddress,
                storage.getAbsolutePath(RELIABLE_SOCKET_PATH),
                storage.getAbsolutePath(UNIX_SOCKET_PATH)));
        setupLogThread(LOG_PATH, WATCH_PATTERN);

        return true;
    }

    @Override
    void run() {
        Binary.runDaemon(getContext(),
                Logger.createLogThread(getTag()),
                storage.getAbsolutePath(CONFIG_PATH),
                storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH));
    }
}
