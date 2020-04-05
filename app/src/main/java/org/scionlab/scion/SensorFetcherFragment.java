package org.scionlab.scion;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.scionlab.scion.as.Config;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.scionlab.scion.as.Config;
import static org.scionlab.scion.as.Config.SensorFetcher.*;
import org.scionlab.scion.as.Process;
import org.scionlab.scion.as.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

public class SensorFetcherFragment extends Fragment {

    TextView resultText;
    TextInputEditText addressInput;
    String result;
    volatile InputStream executableInputStream;
    FragmentActivity activity;
    Thread inputStreamReaderThread;
    Thread backgroundThread;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.sensor_fetcher_fragment, container, false);

        TextInputLayout pingAddressTextInputLayout = layout.findViewById(R.id.sensorFetcherAddressLayout);
        pingAddressTextInputLayout.setEndIconOnClickListener(view -> {
            run();
        });

        resultText = layout.findViewById(R.id.sensorFetcherResult);
        addressInput = layout.findViewById(R.id.sensorFetcherAddressEdit);
        addressInput.setText("17-ffaa:0:1102,[192.33.93.177]:42003");
        activity = getActivity();

        return layout;
    }

    void startInputStreamReadThread() {
        if (inputStreamReaderThread != null) {
            inputStreamReaderThread.interrupt();
        }

        inputStreamReaderThread = new Thread(() -> {
            InputStreamReader isr = new InputStreamReader(executableInputStream);
            BufferedReader br = new BufferedReader(isr);

            while (true) {
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String line2 = line;
                        activity.runOnUiThread(() -> {
                            result += "\r\n" + line2;
                            resultText.setText(result);
                        });
                    }
                } catch (Exception e) {
                    return;
                }

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    return;
                }
            }
        });
        inputStreamReaderThread.start();
    }

    void run() {
        if (backgroundThread != null && backgroundThread.isAlive()) {
            Toast.makeText(getActivity(), "Already Running",
                Toast.LENGTH_LONG).show();
            return;
        }

        result = "";
        final Context context = getContext();
        final Storage storage = Storage.from(context);
        final String destinationAddress = addressInput.getText().toString();

        final Process.InputStreamHandler inputStreamHandler = new Process.InputStreamHandler() {
            @Override
            public void handle(InputStream stream) {
                executableInputStream = stream;
                startInputStreamReadThread();
            }
        };

        backgroundThread = new Thread(() -> {
            Process process = Process.from(
                    Config.Scion.SCIONLAB_BINARY_PATH,
                    "SensorFetcher",
                    Storage.from(context),
                    new UncaughtExceptionHandler(context));

            process.addEnvironmentVariable(DISPATCHER_SOCKET_ENV, storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH))
                    .addArgument(BINARY_FLAG)
                    .addArgument(SERVER_FLAG, destinationAddress)
                    .run(inputStreamHandler);
        });
        backgroundThread.start();
    }
}
