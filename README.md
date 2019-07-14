# How to make it work
## Prerequisites
- Android device with at least Android 9 Pie (henceforth referred to as “endhost”)
- Working scion AS that is reachable via IP from the endhost

## Import endhost configuration
1. From your AS' `gen` directory, transfer the `endhost` directory onto your endhost's user accessible storage.
2. From your AS' `gen` directory, transfer the `dispatcher.zlog.conf` file onto your endhost's user accessible storage.
3. In the endhost's `endhost` directory, find the `sciond.toml` file and make the following changes:
  - In the `[sd]` section's `Public` value, replace your AS' IP address with your endhost's IP address.
  - In the `[sd]` section, replace the `Reliable` value with <sample>"/data/data/org.scionlab.endhost/files/run/shm/sciond/default.sock"</sample>
  - In the `[sd]` section, replace the `Unix` value with <sample>"/data/data/org.scionlab.endhost/files/run/shm/sciond/default.unix"</sample>
  - In the `[general]` section, add the following key-value pair: `DispatcherPath = "/data/data/org.scionlab.endhost/files/run/shm/dispatcher/default.sock"`
  - In the `[general]` section, replace the `ConfigDir` value with an absolute path describing where the `endhost` directory now resides on your endhost, e.g. `"/sdcard/endhost"`
  - In the `[TrustDB]` section, prepend the `Connection` value with the root directory of your endhost's user accessible storage, resulting in e.g. `"/sdcard/gen-cache/sd***.trust.db"`
  - In the `[sd.PathDB]` section, prepend the `Connection` value with the root directory of your endhost's user accessible storage, resulting in e.g. `"/sdcard/gen-cache/sd***.path.db"`
  - In the `[logging.file]` section, prepend the `Path` value with the root directory of your endhost's user accessible storage, resulting in e.g. `"/sdcard/logs/sd***.log"`
4. In the endhost's `dispatcher.zlog.conf` file, make the following changes:
  - In the `[rules]` section, prepend the `dispatcher.DEBUG` value's string part with the root directory of your endhost's user accessible storage, resulting in e.g. `"/sdcard/logs/dispatcher.zlog.DEBUG"`
  - In the `[rules]` section, do the same with the `dispatcher.INFO`, `dispatcher.WARN`, `dispatcher.ERROR`, and `dispatcher.FATAL` values.
5. In your endhost's user accessible storage, ensure that every directory referenced in the config files actually exists, e.g. `/sdcard/gen-cache` and `/sdcard/logs`.

## Starting the dispatcher and sciond
Open the app and push the “Load dispatcher config” button. In the dialog that appears, navigate to your endhost's `dispatcher.zlog.conf` file. Your notification drawer should now have a new permanent entry called “Dispatcher service”.
Push the “Load sciond config” button. In the dialog that appears, navigate to your endhost's `endhost` directory and select the `sciond.toml` file. Your notification drawer should now have a new permanent entry called “Sciond service”.
The log files defined in the aforementioned config files should now get populated and not contain error messages.
In the text box, put in command line parameters for a call to the pingpong application. These should be newline-separated and _must_ include the `-sciond` and `-dispatcher` flags. Since the pingpong call sets `/data/data/org.scionlab.endhost` as its working directory, these can be relative paths. It is also advised to set the `-count` flag to a non-zero value since there is currently no way to gracefully interrupt the pingpong process, once started. An example configuration would look as follows (mind the newlines!):
```
-local
[endhost address]
-remote
19-ffaa:0:1303,[141.44.25.144]:40002
-count
1
-sciond
run/shm/sciond/default.sock
-dispatcher
run/shm/dispatcher/default.sock
```
Tap the “Start pingpong” button to test your endhost's connectivity. You should notice movement in your log files. With access to your endhost's logcat, you could even see pingpong's stdout and stderr outputs.