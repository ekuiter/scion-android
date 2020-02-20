package org.scionlab.endhost;

import android.content.Context;

public abstract class ScionComponent {
    protected Context context;
    protected Storage storage;
    private Thread thread;

    public ScionComponent(Context context) {
        this.context = context;
        storage = Storage.from(context);
    }

    boolean isRunning() {
        return thread != null;
    }

    void start() {
        if (thread == null)
            return;

        prepare();
        thread = new Thread(this::run);
        thread.start();
    }

    void stop() {
        if (thread == null)
            return;

        thread.interrupt();
        thread = null;
    }

    // Override this to implement initialization procedures for a SCION component
    // (such as writing configuration files). This is run in the main thread and
    // as such, will not be interrupted. This will be called right before run().
    public abstract void prepare();

    // Rverride this to run the actual (long-running) SCION process - everything
    // implemented here should be interruptible (i.e., handles InterruptedException)
    // so we can stop the process any time. This will be called right after prepare().
    public abstract void run();
}
