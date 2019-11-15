# How to make it work
## Prerequisites
- Android device with at least Android 9 Pie (henceforth referred to as “endhost”)
- Working scion AS with services set up to be reachable via IP from the endhost

## Import endhost configuration
1. From your AS' `gen` directory, transfer the `endhost` directory onto your endhost's user accessible storage.
3. In the endhost's `endhost` directory, find the `sciond.toml` file and make the following changes:
  - In the `[sd]` section's `Public` value, replace your AS' IP address with your endhost's IP address.
  - If you want to be able to access the sciond's log output: Replace the `[logging.file]` section's `Path` value with an absolute path that is accessible to you, e.g. `"/sdcard/Android/data/org.scionlab.endhost/files/logs/sd***.log"`.
4. In the endhost's `endhost` directory, find the `topology.json` file and make sure that the endhost can reach the AS' services.

## Starting the dispatcher and sciond
Open the app and push the “Start dispatcher” button. Your notification drawer should now have a new permanent entry called “Dispatcher service”.
Push the “Set endhost directory” button. In the dialog that appears, navigate to your endhost's `endhost` directory and press “OK”. Your notification drawer should now have a new permanent entry called “Sciond service”.
The log files (usually) located in `/sdcard/Android/data/org.scionlab.endhost/files/logs/` should now get populated and not contain error messages.
Apart from the `/sdcard/` prefix, the dispatcher logs are hardcoded to be in the aforementioned directory, while sciond's logs should be where they've been configured to be in the previous step.

## Testing connectivity with scmp
In the text box, put in command line parameters for a call to the scmp application. These should be newline-separated and _must_ include the `-sciond` and `-dispatcher` flags.
Since all calls set [the app's files dir](https://developer.android.com/reference/android/content/Context.html#getFilesDir()) as the working directory and the dispatcher and sciond place their sockets relative to that, these should be relative paths.
It is also advised to set the `-c` flag to a non-zero value since there is currently no way to gracefully interrupt the scmp process, once started.
An example configuration would look as follows (mind the newlines!):
```
echo
-local
[endhost address]
-remote
19-ffaa:0:1303,[0.0.0.0]
-c
1
-sciond
run/shm/sciond/default.sock
-dispatcher
run/shm/dispatcher/default.sock
```
Tap the “Start scmp” button to test your endhost's connectivity.
You should notice movement in your log files.
With access to your endhost's logcat, you could even see scmp's stdout and stderr outputs.
If after some seconds, there is a notification titled “Scmp service” mentioning “Scmp returned with value 0”, you have SCION connectivity.
