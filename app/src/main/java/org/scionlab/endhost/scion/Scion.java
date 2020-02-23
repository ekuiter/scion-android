package org.scionlab.endhost.scion;

import android.content.Context;
import android.util.Log;

import org.scionlab.endhost.Storage;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import static org.scionlab.endhost.scion.Config.Scion.*;

public class Scion {
    private static final String TAG = "Scion";
    private final Storage storage;
    private final ComponentRegistry componentRegistry;

    public Scion(Context context, Consumer<Map<Class<? extends Component>, Component.State>> componentStateCallback) {
        Process.initialize(context);
        storage = Storage.from(context);
        componentRegistry = new ComponentRegistry(storage, componentStateCallback);
    }

    public boolean start(String configDirectory) {
        // copy configuration folder provided by the user
        if (storage.countFilesInDirectory(new File(configDirectory)) > CONFIG_DIRECTORY_FILE_LIMIT) {
            Log.e(TAG, "too many files in configuration directory, did you choose the right directory?");
            return false;
        }
        storage.deleteFileOrDirectory(CONFIG_DIRECTORY_PATH);
        storage.copyFileOrDirectory(new File(configDirectory), CONFIG_DIRECTORY_PATH);

        Log.i(TAG, "starting SCION components");
        componentRegistry
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
        Log.i(TAG, "stopping SCION components");
        componentRegistry.stopAll();
    }
}
