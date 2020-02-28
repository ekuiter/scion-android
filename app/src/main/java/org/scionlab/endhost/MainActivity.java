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

package org.scionlab.endhost;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.scionlab.endhost.scion.Logger;
import org.scionlab.endhost.scion.Scion;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final String SCIONLAB_ARCHIVE_FILE = MainActivity.class.getCanonicalName() + ".SCIONLAB_ARCHIVE_FILE";
    public static final String UPDATE_USER_INTERFACE = MainActivity.class.getCanonicalName() + ".UPDATE_USER_INTERFACE";
    public static final String SCION_STATE = MainActivity.class.getCanonicalName() + ".SCION_STATE";

    private SharedPreferences getPreferences;
    private BroadcastReceiver updateUserInterfaceReceiver;
    private AppCompatButton scionButton;
    private ScrollView scrollView;
    private TextView logTextView;
    private String scionlabArchiveFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (savedInstanceState != null)
            scionlabArchiveFile = savedInstanceState.getString(SCIONLAB_ARCHIVE_FILE);
        getPreferences = getPreferences(MODE_PRIVATE);
        scionButton = findViewById(R.id.scionbutton);
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
                updateUserInterface((Scion.State) intent.getSerializableExtra(SCION_STATE));
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
        if (scionlabArchiveFile != null)
            outState.putString(SCIONLAB_ARCHIVE_FILE, scionlabArchiveFile);
    }

    private void updateUserInterface(Scion.State state) {
        if (state == null)
            state = Scion.State.STOPPED;

        if (state == Scion.State.STOPPED) {
            scionButton.setText(R.string.scionbuttonstart);
            scionButton.setOnClickListener(view ->
                    new ChooserDialog(view.getContext())
                            .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                            .withFilter(false, true)
                            .withStartFile(getPreferences.getString(SCIONLAB_ARCHIVE_FILE, null))
                            .withChosenListener((path, pathFile) -> {
                                if (path != null) {
                                    logTextView.setText("");
                                    scionlabArchiveFile = path;
                                    ActivityCompat.requestPermissions(
                                            this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            0
                                    );
                                    getPreferences.edit().putString(SCIONLAB_ARCHIVE_FILE, path).apply();
                                    VPNPermissionFragment.askPermission(this, (String errorMessage) -> {
                                        if (errorMessage == null)
                                            startScionService(scionlabArchiveFile);
                                        else
                                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                    });
                                }
                            }).build().show());
        } else {
            scionButton.setText(R.string.scionbuttonstop);
            scionButton.setOnClickListener(view -> stopScionService());
        }
    }

    private void startScionService(String scionlabArchiveFile) {
        startService(new Intent(this, ScionService.class)
                .putExtra(ScionService.VERSION, Scion.Version.SCIONLAB)
                .putExtra(ScionService.SCIONLAB_ARCHIVE_FILE, scionlabArchiveFile));
    }

    private void stopScionService() {
        stopService(new Intent(this, ScionService.class));
    }
}
