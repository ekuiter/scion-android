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

        return layout;
    }

    void run() {
        result = "";
        final Context context = getContext();
        final Storage storage = Storage.from(context);
        final FragmentActivity activity = getActivity();
        final String destinationAddress = addressInput.getText().toString();

        final Process.InputStreamHandler inputStreamHandler = new Process.InputStreamHandler() {
            @Override
            public void handle(InputStream stream) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStreamReader isr = new InputStreamReader(stream);
                            BufferedReader br = new BufferedReader(isr);

                            while (true) {
                                final String line = br.readLine();
                                if (line != null) {
                                    activity.runOnUiThread(() -> {
                                        result += "\r\n" + line;
                                        resultText.setText(result);
                                    });
                                }
                            }
                        }
                        catch (Exception e) { }
                    }
                });
                thread.start();
            }
        };

        Thread backgroundThread = new Thread(() -> {
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
