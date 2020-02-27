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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Acts as a central registry for all SCION components (daemon, dispatcher etc.)
 * For each component, exactly one instance may be registered (see Scion).
 */
class ComponentRegistry {
    private String binaryPath;
    private Storage storage;
    private Consumer<Map<Class<? extends Component>, Component.State>> componentStateCallback;
    private ConcurrentHashMap<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

    ComponentRegistry(Storage storage, Consumer<Map<Class<? extends Component>, Component.State>> componentStateCallback) {
        this.storage = storage;
        this.componentStateCallback = componentStateCallback;
    }

    Storage getStorage() {
        return storage;
    }

    ComponentRegistry setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
        return this;
    }

    String getBinaryPath() {
        return binaryPath;
    }

    void notifyStateChange() {
        components.values().forEach(Component::notifyStateChange);
        componentStateCallback.accept(components.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getState())));
    }

    private void register(Component component) {
        Class<? extends Component> cls = component.getClass();
        if (components.containsKey(cls))
            throw new RuntimeException("SCION component for " + cls + " already registered");
        components.put(cls, component);
        component.setComponentRegistry(this);
    }

    private void unregister(Component component) {
        Class<? extends Component> cls = component.getClass();
        if (get(cls) != component)
            throw new RuntimeException("other SCION component registered for " + cls);
        components.remove(cls);
        component.setComponentRegistry(null);
    }

    ComponentRegistry start(Component component) {
        register(component);
        component.start();
        return this;
    }

    private void stop(Component component) {
        component.stop();
        unregister(component);
    }

    void stopAll() {
        components.values().forEach(this::stop);
    }

    private Component get(Class<? extends Component> cls) {
        return components.get(cls);
    }

    private boolean isReady(Class<? extends Component> cls) {
        return get(cls) != null && get(cls).getState() == Component.State.READY;
    }

    boolean isReady(Class... classes) {
        return Stream.of(classes).allMatch(this::isReady);
    }
}
