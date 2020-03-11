package org.scionlab.scion;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.scionlab.scion.as.Config;
import org.scionlab.scion.as.Logger;

import timber.log.Timber;

public class LogActivity extends AppCompatActivity {
    private static Logger.Tree tree;
    private static StringBuffer buffer = new StringBuffer();
    private static Logger.LogLevel logLevel = Config.Logger.DEFAULT_LOG_LEVEL;
    private TextView logTextView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        Spinner logLevelSpinner = findViewById(R.id.logLevelSpinner);
        scrollView = findViewById(R.id.scrollView);
        logTextView = findViewById(R.id.logTextView);

        Logger.Tree newTree = new Logger.Tree((tag, message) -> runOnUiThread(() -> {
            logTextView.append(formatMessage(tag, message));
            scrollDown();
        }));
        logLevelSpinner.setSelection(logLevel.getValue());
        logLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                logLevel = Logger.LogLevel.valueOf((String) parent.getItemAtPosition(position));
                tree.setLogLevel(logLevel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        logTextView.append(buffer);
        scrollDown();
        buffer = new StringBuffer();
        plantTree(newTree);
    }

    @Override
    protected void onDestroy() {
        buffer = new StringBuffer(logTextView.getText());
        plantTree(new Logger.Tree((tag, message) -> runOnUiThread(() ->
                LogActivity.append(tag, message))));
        super.onDestroy();
    }

    static void plantTree(Logger.Tree tree) {
        LogActivity.tree = tree;
        Timber.uprootAll();
        Timber.plant(tree);
    }

    static void append(String tag, String message) {
        buffer.append(formatMessage(tag, message));
    }

    @NonNull
    private static String formatMessage(String tag, String message) {
        return String.format("%s: %s\n", tag, message);
    }

    private void scrollDown() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
