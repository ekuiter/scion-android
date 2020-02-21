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

import android.content.Context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Acts as a central registry for all SCION components (daemon, dispatcher etc.)
 * For each component, exactly one instance may be registered (see MainService).
 */
public class ComponentRegistry {
    private Context context;
    private ConcurrentHashMap<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

    public ComponentRegistry(Context context) {
        this.context = context;
    }

    Context getContext() {
        return context;
    }

    private void register(Component component) {
        Class<? extends Component> cls = component.getClass();
        if (components.containsKey(cls))
            throw new RuntimeException("SCION component for " + cls + " already registered");
        components.put(cls, component);
    }

    private void unregister(Component component) {
        Class<? extends Component> cls = component.getClass();
        if (get(cls) != component)
            throw new RuntimeException("other SCION component registered for " + cls);
        components.remove(cls);
    }

    public ComponentRegistry start(Component component) {
        component.setComponentRegistry(this);
        component.start();
        register(component);
        return this;
    }

    private void stop(Component component) {
        unregister(component);
        component.stop();
        component.setComponentRegistry(null);
    }

    public void stopAll() {
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
