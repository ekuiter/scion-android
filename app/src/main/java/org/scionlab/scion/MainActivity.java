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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.obsez.android.lib.filechooser.ChooserDialog;

import org.scionlab.scion.as.Config;
import org.scionlab.scion.as.Logger;
import org.scionlab.scion.as.ScionAS;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String SCIONLAB_CONFIGURATION = MainActivity.class.getCanonicalName() + ".SCIONLAB_CONFIGURATION";
    private static final String PING_ADDRESS = MainActivity.class.getCanonicalName() + ".PING_ADDRESS";
    private static final String UPDATE_USER_INTERFACE = MainActivity.class.getCanonicalName() + ".UPDATE_USER_INTERFACE";
    private static final String SCION_STATE = MainActivity.class.getCanonicalName() + ".SCION_STATE";
    private static final String COMPONENT_STATE = MainActivity.class.getCanonicalName() + ".COMPONENT_STATE";

    private SharedPreferences getPreferences;
    private BroadcastReceiver updateUserInterfaceReceiver;
    private MaterialButton scionButton;
    private EditText pingAddressEditText;
    private String scionLabConfiguration;
    private String pingAddress;

    static void updateUserInterface(Context context, ScionAS.State state, Map<String, ScionAS.State> componentState) {
        context.sendBroadcast(new Intent(UPDATE_USER_INTERFACE)
                .putExtra(SCION_STATE, state)
                .putExtra(COMPONENT_STATE, (Serializable) componentState));
    }

    static Intent bringToForeground(Context context) {
        return new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
        setContentView(R.layout.activity_main);
        getPreferences = getPreferences(MODE_PRIVATE);
        scionButton = findViewById(R.id.scionbutton);
        pingAddressEditText = findViewById(R.id.pingAddressEditText);
        if (savedInstanceState != null) {
            scionLabConfiguration = savedInstanceState.getString(SCIONLAB_CONFIGURATION);
            pingAddress = savedInstanceState.getString(PING_ADDRESS);
        } else
            pingAddress = getResources().getString(R.string.pingAddress);
        pingAddressEditText.setText(getPreferences.getString(PING_ADDRESS, getResources().getString(R.string.pingAddress)));
        LogActivity.plantTree(new Logger.Tree((tag, message) -> runOnUiThread(() ->
                LogActivity.append(tag, message))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInterfaceReceiver = new BroadcastReceiver() {
            @SuppressWarnings("unchecked")
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUserInterface((ScionAS.State) intent.getSerializableExtra(SCION_STATE),
                        (Map<String, ScionAS.State>) intent.getSerializableExtra(COMPONENT_STATE));
            }
        };
        registerReceiver(updateUserInterfaceReceiver, new IntentFilter(UPDATE_USER_INTERFACE));
        updateUserInterface(ScionService.getState(), ScionService.getComponentState());
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
        if (pingAddress != null)
            outState.putString(PING_ADDRESS, pingAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItem = item.getItemId();

        switch (menuItem) {
            case R.id.log:
                startActivity(new Intent(this, LogActivity.class));
                break;

            case R.id.how:
                startActivity(new Intent(this, WebActivity.class)
                        .putExtra(WebActivity.ASSET, "file:///android_asset/how.html"));
                break;

            case R.id.about:
                startActivity(new Intent(this, WebActivity.class)
                        .putExtra(WebActivity.ASSET, "file:///android_asset/about.html"));
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateUserInterface(ScionAS.State state, Map<String, ScionAS.State> componentState) {
        if (state == null)
            state = ScionAS.State.STOPPED;

        if (state == ScionAS.State.STOPPED) {
            scionButton.setText(R.string.start);
            scionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            scionButton.setOnClickListener(view ->
                    VPNPermissionFragment.askPermission(this, (String errorMessage) -> {
                        if (errorMessage != null) {
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        chooseScionLabConfiguration(scionLabConfiguration -> {
                            pingAddress = pingAddressEditText.getText().toString();
                            getPreferences.edit().putString(PING_ADDRESS, pingAddress).apply();
                            ScionService.start(this, scionLabConfiguration, pingAddress);
                        });
                    }));
        } else {
            scionButton.setText(R.string.stop);
            scionButton.setBackgroundColor(ContextCompat.getColor(this,
                    state == ScionAS.State.STARTING ? R.color.colorStarting :
                            state == ScionAS.State.HEALTHY ? R.color.colorHealthy : R.color.colorUnhealthy));
            scionButton.setOnClickListener(view -> ScionService.stop(this));
        }

        Chip[] chips = new Chip[] {
                findViewById(R.id.beaconServer), findViewById(R.id.borderRouter),
                findViewById(R.id.certificateServer), findViewById(R.id.daemon),
                findViewById(R.id.dispatcher), findViewById(R.id.pathServer),
                findViewById(R.id.scmp), findViewById(R.id.vpnClient)};
        for (Chip chip : chips)
            chip.setChipIconTintResource(R.color.colorPrimary);

        componentState.forEach((k, v) -> {
            int color = v == ScionAS.State.STOPPED ? R.color.colorPrimary :
                    v == ScionAS.State.STARTING ? R.color.colorStarting :
                    v == ScionAS.State.HEALTHY ? R.color.colorHealthy : R.color.colorUnhealthy;
            if (k.equals("BeaconServer"))
                ((Chip) findViewById(R.id.beaconServer)).setChipIconTintResource(color);
            if (k.equals("BorderRouter"))
                ((Chip) findViewById(R.id.borderRouter)).setChipIconTintResource(color);
            if (k.equals("CertificateServer"))
                ((Chip) findViewById(R.id.certificateServer)).setChipIconTintResource(color);
            if (k.equals("Daemon"))
                ((Chip) findViewById(R.id.daemon)).setChipIconTintResource(color);
            if (k.equals("Dispatcher"))
                ((Chip) findViewById(R.id.dispatcher)).setChipIconTintResource(color);
            if (k.equals("PathServer"))
                ((Chip) findViewById(R.id.pathServer)).setChipIconTintResource(color);
            if (k.equals("Scmp"))
                ((Chip) findViewById(R.id.scmp)).setChipIconTintResource(color);
            if (k.equals("VPNClient"))
                ((Chip) findViewById(R.id.vpnClient)).setChipIconTintResource(color);
        });
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
