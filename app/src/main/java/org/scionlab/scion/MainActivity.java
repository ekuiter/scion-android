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

package org.scionlab.scion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.scionlab.scion.as.Config;
import org.scionlab.scion.as.Logger;
import org.scionlab.scion.as.ScionAS;

import java.util.function.Consumer;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final String SCIONLAB_CONFIGURATION = MainActivity.class.getCanonicalName() + ".SCIONLAB_CONFIGURATION";
    private static final String UPDATE_USER_INTERFACE = MainActivity.class.getCanonicalName() + ".UPDATE_USER_INTERFACE";
    private static final String SCION_STATE = MainActivity.class.getCanonicalName() + ".SCION_STATE";

    private SharedPreferences getPreferences;
    private BroadcastReceiver updateUserInterfaceReceiver;
    private AppCompatButton scionButton;
    private EditText pingAddressEditText;
    private ScrollView scrollView;
    private TextView logTextView;
    private String scionLabConfiguration;

    static void updateUserInterface(Context context, ScionAS.State state) {
        context.sendBroadcast(new Intent(UPDATE_USER_INTERFACE)
                .putExtra(SCION_STATE, state));
    }

    static Intent bringToForeground(Context context) {
        return new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (savedInstanceState != null)
            scionLabConfiguration = savedInstanceState.getString(SCIONLAB_CONFIGURATION);
        getPreferences = getPreferences(MODE_PRIVATE);
        scionButton = findViewById(R.id.scionbutton);
        pingAddressEditText = findViewById(R.id.pingAddressEditText);
        Spinner logLevelSpinner = findViewById(R.id.logLevelSpinner);
        scrollView = findViewById(R.id.scrollView);
        logTextView = findViewById(R.id.logTextView);

        Logger.Tree tree = new Logger.Tree((tag, message) -> runOnUiThread(() -> {
            logTextView.append(String.format("%s: %s\n", tag, message));
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }));
        logLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tree.setLogLevel(Logger.LogLevel.valueOf((String) parent.getItemAtPosition(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Timber.plant(tree);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInterfaceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUserInterface((ScionAS.State) intent.getSerializableExtra(SCION_STATE));
            }
        };
        registerReceiver(updateUserInterfaceReceiver, new IntentFilter(UPDATE_USER_INTERFACE));
        updateUserInterface(ScionService.getState());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateUserInterfaceReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (scionLabConfiguration != null)
            outState.putString(SCIONLAB_CONFIGURATION, scionLabConfiguration);
    }

    private void updateUserInterface(ScionAS.State state) {
        if (state == null)
            state = ScionAS.State.STOPPED;

        if (state == ScionAS.State.STOPPED) {
            scionButton.setText(R.string.scionButtonStart);
            scionButton.setOnClickListener(view -> {
                logTextView.setText("");
                VPNPermissionFragment.askPermission(this, (String errorMessage) -> {
                    if (errorMessage != null) {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    chooseScionLabConfiguration(scionLabConfiguration ->
                            ScionService.start(this,
                                    scionLabConfiguration,
                                    pingAddressEditText.getText().toString()));
                });
            });
        } else {
            scionButton.setText(R.string.scionButtonStop);
            scionButton.setOnClickListener(view -> ScionService.stop(this));
        }
    }

    private void chooseScionLabConfiguration(Consumer<String> callback) {
        new ChooserDialog(this)
                .withResources(R.string.chooseScionLabConfiguration, R.string.ok, R.string.cancel)
                .withStartFile(getPreferences.getString(SCIONLAB_CONFIGURATION, null))
                .withFilterRegex(false, false, Config.Scion.SCIONLAB_CONFIGURATION_REGEX)
                .withChosenListener((path, pathFile) -> {
                    if (path != null) {
                        scionLabConfiguration = path;
                        getPreferences.edit().putString(SCIONLAB_CONFIGURATION, scionLabConfiguration).apply();
                        callback.accept(path);
                    }
                }).build().show();
    }
}
