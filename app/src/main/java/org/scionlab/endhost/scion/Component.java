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
public abstract class Component {
    ComponentRegistry componentRegistry;
    Storage storage;
    Process process;
    private Thread thread;
    private boolean doneWaiting = false, isReady = false;

    public enum State {
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

    boolean isReady() {
        return isReady;
    }

    // Is called when the component transitions from STARTING to READY. Should only
    // be called from within run(). Note that a crash of the component should cause
    // run() to exit instead of setting isReady = false;
    void setReady() {
        if (!isReady) {
            timber().i("component is ready");
            isReady = true;
            if (componentRegistry != null)
                componentRegistry.notifyStateChange();
        }
    }

    State getState() {
        if (!isRunning())
            return State.STOPPED;
        return isReady ? State.READY : State.STARTING;
    }

    Logger.LogThread createLogThread(String logPath, Pattern readyPattern) {
        storage.prepareFile(logPath);
        return Logger.createLogThread(getTag(), storage.getEmptyInputStream(logPath))
                .watchFor(readyPattern, this::setReady);
    }

    void notifyStateChange() {
        if (doneWaiting && !mayRun())
            stop();
    }

    void start() {
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

    void stop() {
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
    // Override this to check to check whether other required components are ready.
    boolean mayRun() {
        return true;
    }

    // Rverride this to run the actual (long-running) SCION process - everything
    // implemented here should be interruptible (i.e., handles InterruptedException)
    // so we can stop the process any time. This will be called right after mayRun().
    abstract void run();
}
