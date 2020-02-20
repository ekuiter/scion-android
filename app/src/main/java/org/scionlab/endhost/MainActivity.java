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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity {
    private static String getClassName() { return (new Object(){}).getClass().getEnclosingClass().getCanonicalName(); }
    private static final String DAEMON_CONFIG_DIRECTORY = getClassName() + ".DAEMON_CONFIG_DIRECTORY";
    static final String SERVICE_CHANNEL = getClassName() + ".SERVICES";
    static final String UPDATE_USER_INTERFACE = getClassName() + ".UPDATE_USER_INTERFACE";

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), UPDATE_USER_INTERFACE))
                updateUserInterface();
        }
    };

    Optional<Bundle> optionalState;
    private Optional<String> daemonConfigDirectory;
    private AppCompatButton scionButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        optionalState = Optional.ofNullable(savedInstanceState);
        daemonConfigDirectory = optionalState.map(i->i.getString(DAEMON_CONFIG_DIRECTORY));
        createNotificationChannel();
        prefs = getPreferences(MODE_PRIVATE);
        scionButton = findViewById(R.id.scionbutton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter resetFilter = new IntentFilter();
        resetFilter.addAction(UPDATE_USER_INTERFACE);
        registerReceiver(serviceReceiver, resetFilter);
        updateUserInterface();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_help) {
            startActivity(new Intent(this, MarkdownActivity.Help.class));
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, MarkdownActivity.About.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Function<String,Consumer<CharSequence>> putter = key->cs->outState.putString(key, cs.toString());
        daemonConfigDirectory.ifPresent(putter.apply(DAEMON_CONFIG_DIRECTORY));
    }

    private void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.servicechannel_name);
            String description = getString(R.string.servicechannel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateUserInterface() {
        if (!ScionService.isRunning()) {
            scionButton.setText(R.string.scionbuttonstart);
            scionButton.setOnClickListener(view ->
                    new ChooserDialog(view.getContext())
                            .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                            .withFilter(true, true)
                            .withStartFile(prefs.getString(DAEMON_CONFIG_DIRECTORY, null))
                            .withChosenListener((path, pathFile) ->
                                    (daemonConfigDirectory = Optional.ofNullable(path)).ifPresent(p -> {
                                        ensureWritePermissions();
                                        startService(new Intent(this, ScionService.class)
                                                .putExtra(ScionService.DAEMON_CONFIG_DIRECTORY, p));
                                        putString(DAEMON_CONFIG_DIRECTORY, p);
                                        updateUserInterface();
                                    })
                            ).build().show());
        } else {
            scionButton.setText(R.string.scionbuttonstop);
            scionButton.setOnClickListener(view -> {
                stopService(new Intent(this, ScionService.class));
                updateUserInterface();
            });
        }
    }

    private void ensureWritePermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(serviceReceiver);
    }
}
