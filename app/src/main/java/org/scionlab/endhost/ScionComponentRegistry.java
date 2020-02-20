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

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Acts as a central registry for all SCION components (daemon, dispatcher etc.)
 * For each component, exactly one instance may be registered (see ScionService).
 */
public class ScionComponentRegistry {
    private static ScionComponentRegistry instance = new ScionComponentRegistry();
    private ConcurrentHashMap<Class<? extends ScionComponent>, ScionComponent> scionComponents = new ConcurrentHashMap<>();

    public static ScionComponentRegistry getInstance() {
        return instance;
    }

    ScionComponentRegistry register(ScionComponent scionComponent) {
        Class<? extends ScionComponent> cls = scionComponent.getClass();
        if (scionComponents.containsKey(cls))
            throw new RuntimeException("SCION component for " + cls + " already registered");
        scionComponents.put(cls, scionComponent);
        return this;
    }

    ScionComponentRegistry unregister(ScionComponent scionComponent) {
        Class<? extends ScionComponent> cls = scionComponent.getClass();
        if (get(cls) != scionComponent)
            throw new RuntimeException("other SCION component registered for " + cls);
        scionComponents.remove(cls);
        return this;
    }

    ScionComponentRegistry start(ScionComponent scionComponent) {
        scionComponent.start();
        register(scionComponent);
        return this;
    }

    ScionComponentRegistry stop(ScionComponent scionComponent) {
        unregister(scionComponent);
        scionComponent.stop();
        return this;
    }

    void stopAll() {
        scionComponents.values().forEach(this::stop);
    }

    ScionComponent get(Class<? extends ScionComponent> cls) {
        return scionComponents.get(cls);
    }

    private boolean isReady(Class<? extends ScionComponent> cls) {
        return get(cls) != null && get(cls).getState() == ScionComponent.State.READY;
    }

    public boolean isReady(Class... classes) {
        return Stream.of(classes).allMatch(this::isReady);
    }
}
