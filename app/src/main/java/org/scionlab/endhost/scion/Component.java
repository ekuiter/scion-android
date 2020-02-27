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

import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Think of SCION components as "Docker containers": They can be started, stopped,
 * and have a state that usually transitions from STOPPED to STARTING and then READY.
 * This serves the same purpose as the SCION services in /lib/systemd/system on Linux.
 */
abstract class Component {
    private ComponentRegistry componentRegistry;
    Storage storage;
    Process process;
    private Thread thread;
    private boolean doneWaiting = false, isReady = false;

    enum State {
        STOPPED, STARTING, READY
    }

    void setComponentRegistry(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    boolean isRunning() {
        return thread != null;
    }

    private Timber.Tree timber() {
        return Timber.tag(getTag());
    }

    boolean isHealthy() {
        return getState() == State.READY;
    }

    // Is called when the component transitions from STARTING to READY. Should only
    // be called from within run(). Note that a crash of the component should cause
    // run() to exit instead of setting isReady = false;
    synchronized void setReady() {
        if (!isReady) {
            timber().i("component is ready");
            isReady = true;
        }
        notifyStateChange();
    }

    synchronized void notifyStateChange() {
        if (componentRegistry != null)
            componentRegistry.notifyStateChange();
    }

    State getState() {
        if (!isRunning())
            return State.STOPPED;
        return isReady ? State.READY : State.STARTING;
    }

    Scion.State getScionState() {
        switch (getState()) {
            case STOPPED:
                return Scion.State.STOPPED;
            case STARTING:
                return Scion.State.STARTING;
            default:
                return isHealthy() ? Scion.State.HEALTHY : Scion.State.UNHEALTHY;
        }
    }

    Logger.LogThread createLogThread(String logPath, Pattern readyPattern) {
        storage.prepareFile(logPath);
        return Logger.createLogThread(getTag(), storage.getEmptyInputStream(logPath))
                .watchFor(readyPattern, this::setReady);
    }

    synchronized void stateHasChanged() {
        if (doneWaiting && !mayRun())
            stop();
    }

    synchronized void start() {
        if (thread != null)
            return;

        if (componentRegistry == null) {
            timber().i("not registered with any component registry");
            return;
        }

        timber().i("starting component");
        storage = componentRegistry.getStorage();
        String binaryPath = componentRegistry.getBinaryPath();
        if (binaryPath == null)
            throw new RuntimeException("no binary path given");
        process = Process.from(binaryPath, getTag(), storage);

        if (!prepare()) {
            timber().e("failed to prepare component");
            return;
        }

        thread = new Thread(() -> {
            try {
                int retries = 0;
                for (; retries < Config.Component.READY_RETRIES && !mayRun(); retries++) {
                    if (retries == 0)
                        timber().i("waiting until component may run");
                    Thread.sleep(Config.Component.READY_INTERVAL);
                }
                if (retries > 0)
                    timber().i("done waiting for component");
                doneWaiting = true;
                if (mayRun())
                    run();
            } catch (InterruptedException ignored) {
            } finally {
                timber().i("component has stopped");
                thread = null;
                if (componentRegistry != null)
                    componentRegistry.notifyStateChange();
            }
        });
        thread.start();
        componentRegistry.notifyStateChange();
    }

    synchronized void stop() {
        if (thread == null)
            return;

        timber().i("stopping component");
        thread.interrupt();
    }

    private String getTag() {
        return getClass().getSimpleName();
    }

    // Override this to implement initialization procedures for a SCION component
    // (such as writing configuration files). This is run in the main thread and
    // as such, will not be interrupted. This will be called right before mayRun().
    boolean prepare() {
        return true;
    }

    // Called before/while run() to make sure this component may actually be running.
    private boolean mayRun() {
        if (componentRegistry == null)
            return false;
        return componentRegistry.isReady(dependsOn());
    }

    // Override this to check to define  which other components are required.
    Class[] dependsOn() {
        return new Class[]{};
    }

    // Rverride this to run the actual (long-running) SCION process - everything
    // implemented here should be interruptible (i.e., handles InterruptedException)
    // so we can stop the process any time. This will be called right after mayRun().
    abstract void run();
}
