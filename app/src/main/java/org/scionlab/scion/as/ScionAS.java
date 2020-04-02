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

import android.app.Service;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import timber.log.Timber;

import static org.scionlab.scion.as.Config.Dispatcher.BINARY_FLAG;
import static org.scionlab.scion.as.Config.Logger.DELETE_PATTERN;
import static org.scionlab.scion.as.Config.Logger.UPDATE_INTERVAL;
import static org.scionlab.scion.as.Config.Scion.*;

/**
 * Runs a SCION AS (= autonomous system) by starting all its components.
 */
public class ScionAS {
    private final Service service;
    protected final Storage storage;
    private final ComponentRegistry componentRegistry;
    private Scmp scmp;

    public enum State {
        STOPPED, STARTING, HEALTHY, UNHEALTHY;

        public @NonNull String toString() {
            return this == STOPPED ? "Stopped" :
                    this == STARTING ? "Starting" :
                            this == HEALTHY ? "Healthy" :
                                    "Unhealthy";
        }
    }

    public enum Version {
        SCIONLAB(SCIONLAB_BINARY_PATH);

        private String binaryPath;

        Version(String binaryPath) {
            this.binaryPath = binaryPath;
        }

        public String getBinaryPath() {
            return binaryPath;
        }

        public String getScionVersion(Storage storage) {
            AtomicReference<String> version = new AtomicReference<>();
            Process.from(binaryPath, null, storage, new Logger.LogThread(
                    version::set, DELETE_PATTERN, UPDATE_INTERVAL, null))
                    .addArgument(BINARY_FLAG).addArgument(VERSION_FLAG).run();
            String _version = version.get();
            if (_version != null) {
                if (!_version.contains("Scion version:"))
                    _version = null;
                else
                    _version = _version
                            .replaceFirst("^.*Scion version: ", "")
                            .replaceFirst("-dirty", "");
            }
            return _version;
        }
    }

    ScionAS(Service service, BiConsumer<State, Map<String, State>> stateCallback) {
        this.service = service;
        Process.initialize(service);
        storage = Storage.from(service);
        componentRegistry = new ComponentRegistry(service, storage,
                (Map<String, State> componentState) ->
                        stateCallback.accept(getState(), componentState));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void start(Version version, String genDirectory, String vpnConfigFile, String pingAddress) {
        Timber.i("writing SCION configuration");
        if (storage.countFilesInDirectory(new File(genDirectory)) > GEN_DIRECTORY_FILE_LIMIT) {
            Timber.e("too many files in gen directory, did you choose the right directory?");
            return;
        }
        storage.deleteFileOrDirectory(GEN_DIRECTORY_PATH);
        storage.copyFileOrDirectory(new File(genDirectory), GEN_DIRECTORY_PATH);

        Optional<String> isdPath = storage.findInDirectory(GEN_DIRECTORY_PATH, ISD_DIRECTORY_PATH_REGEX);
        Optional<String> asPath = storage.findInDirectory(isdPath, AS_DIRECTORY_PATH_REGEX);
        Optional<String> componentPath = storage.findInDirectory(asPath, COMPONENT_DIRECTORY_PATH_REGEX);
        Optional<String> endhostPath = storage.findInDirectory(asPath, ENDHOST_DIRECTORY_PATH_REGEX);
        Optional<String> certsPath = storage.findInDirectory(componentPath, CERTS_DIRECTORY_PATH_REGEX);
        Optional<String> keysPath = storage.findInDirectory(componentPath, KEYS_DIRECTORY_PATH_REGEX);
        Optional<String> topologyPath = storage.findInDirectory(componentPath, TOPOLOGY_PATH_REGEX);

        if (!Stream.of(isdPath, asPath, componentPath, endhostPath, certsPath, keysPath, topologyPath)
                .allMatch(Optional::isPresent)) {
            Timber.e("unexpected gen directory structure");
            return;
        }

        storage.deleteFileOrDirectory(CONFIG_DIRECTORY_PATH);
        storage.createDirectory(CONFIG_DIRECTORY_PATH);
        storage.copyFileOrDirectory(certsPath.get(), CERTS_DIRECTORY_PATH);
        storage.copyFileOrDirectory(keysPath.get(), KEYS_DIRECTORY_PATH);
        if (!writeTopology(topologyPath.get()))
            return;
        storage.deleteFileOrDirectory(GEN_DIRECTORY_PATH);

        Timber.i("starting SCION AS");
        componentRegistry
                .setBinaryPath(version.getBinaryPath())
                .start(new VPNClient(service, vpnConfigFile == null
                        ? null
                        : storage.readFile(new File(vpnConfigFile))))
                .start(new BorderRouter())
                .start(new ControlServer())
                .start(new Dispatcher())
                .start(new Daemon())
                .start(scmp = new Scmp(pingAddress))
                //.start(new SensorFetcher())
                .notifyStateChange();
    }

    public void stop() {
        Timber.i("stopping SCION AS");
        componentRegistry.stopAll().notifyStateChange();
        scmp = null;
    }

    public State getState() {
        if (!componentRegistry.hasRegisteredComponents())
            return State.STOPPED;
        if (componentRegistry.hasComponentsWithState(Component.State.STARTING) &&
            !componentRegistry.hasComponentsWithState(Component.State.STOPPED))
            return State.STARTING;
        if (scmp != null && scmp.isHealthy())
            return State.HEALTHY;
        return State.UNHEALTHY;
    }

    public void setPingAddress(String pingAddress) {
        if (scmp != null) {
            componentRegistry.stop(scmp);
            componentRegistry.start(scmp = new Scmp(pingAddress));
        }
    }

    private boolean writeTopology(String topologyPath) {
        try {
            JSONObject root = new JSONObject(storage.readFile(topologyPath));
            JSONObject borderRouters = root.getJSONObject(BORDER_ROUTERS_JSON_PATH);
            JSONObject interfaces = borderRouters.getJSONObject(borderRouters.keys().next()).getJSONObject(INTERFACES_JSON_PATH);
            JSONObject iface = interfaces.getJSONObject(interfaces.keys().next());
            String ia = root.getString(IA_JSON_PATH);
            String overlayAddr = iface.getJSONObject(PUBLIC_OVERLAY_JSON_PATH).getString(OVERLAY_ADDR_JSON_PATH);
            int overlayPort = iface.getJSONObject(PUBLIC_OVERLAY_JSON_PATH).getInt(OVERLAY_PORT_JSON_PATH);
            String remoteIa = iface.getString(IA_JSON_PATH);
            String remoteOverlayAddr = iface.getJSONObject(REMOTE_OVERLAY_JSON_PATH).getString(OVERLAY_ADDR_JSON_PATH);
            int remoteOverlayPort = iface.getJSONObject(REMOTE_OVERLAY_JSON_PATH).getInt(OVERLAY_PORT_JSON_PATH);
            storage.writeFile(TOPOLOGY_PATH,
                    String.format(storage.readAssetFile(TOPOLOGY_TEMPLATE_PATH),
                        remoteIa, overlayAddr, overlayPort,
                        remoteOverlayAddr, remoteOverlayPort, ia));
        } catch (JSONException e) {
            Timber.e(e);
            return false;
        }
        return true;
    }
}
