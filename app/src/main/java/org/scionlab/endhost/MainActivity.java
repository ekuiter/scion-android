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
import android.os.Process;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity {

    private static String getClassName() { return (new Object(){}).getClass().getEnclosingClass().getCanonicalName(); }
    private static final String SCIOND_CFG_PATH = getClassName() + ".SCIOND";
    static final String SERVICE_CHANNEL = getClassName() + ".SERVICES";
    static final String ACTION_SERVICE = getClassName() + ".SERVICE";
    static final String EXTRA_SERVICE_PID = getClassName() + ".SERVICE_PID";

    Optional<Bundle> optionalState;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                int pid = intent.getIntExtra(EXTRA_SERVICE_PID, -1);
                if (pid != -1) {
                    Process.killProcess(pid);
                }
                activateButtons();
            }
        }
    };
    private Optional<String> sciondCfgPath;
    private AppCompatButton[] buttons;
    private Class<?>[] classes;
    private View.OnClickListener[][] buttonClicks;
    private int[][] buttonTextResIds;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        optionalState = Optional.ofNullable(savedInstanceState);

        sciondCfgPath = optionalState.map(i->i.getString(SCIOND_CFG_PATH));

        createNotificationChannel();

        prefs = getPreferences(MODE_PRIVATE);

        buttons = new AppCompatButton[] {
                findViewById(R.id.scionbutton),
                findViewById(R.id.scion2button)
        };

        buttonTextResIds = new int[][] {
                { R.string.scionbuttonstart, R.string.scionbuttonstart },
                { R.string.scion2buttonstart, R.string.scion2buttonstart }
        };

        classes = new Class[] {
                ScionService.class,
                ScionService.class
        };

        buttonClicks = new View.OnClickListener[][]{
                {
                        view ->
                                new ChooserDialog(view.getContext())
                                        .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                                        .withFilter(true, true)
                                        .withStartFile(prefs.getString(SCIOND_CFG_PATH, null))
                                        .withChosenListener((path, pathFile) ->
                                                (sciondCfgPath = Optional.ofNullable(path)).ifPresent(p -> {
                                                    ensureWritePermissions();
                                                    startService(new Intent(this, ScionService.class)
                                                            .putExtra(ScionService.CONFIG_DIRECTORY_SOURCE_PATH, p));
                                                    putString(SCIOND_CFG_PATH, p);
                                                    activateButtons();
                                                })
                                        ).build().show(),
                        null
                },
                {
                        view -> {
                            stopService(new Intent(this, ScionService.class));
                            activateButtons();
                        },
                        null
                }
        };

        for (int i = 0; i < classes.length; i++) {
            final Intent intent = new Intent(this, classes[i]);
            buttonClicks[i][1] = view -> { stopService(intent); activateButtons(); };
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter resetFilter = new IntentFilter();
        resetFilter.addAction(ACTION_SERVICE);
        registerReceiver(serviceReceiver, resetFilter);
        activateButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Function<String,Consumer<CharSequence>> putter = key->cs->outState.putString(key, cs.toString());
        sciondCfgPath.ifPresent(putter.apply(SCIOND_CFG_PATH));
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

    private void activateButtons() {
        boolean[] running = new boolean[classes.length];
        for (int i = 0; i < running.length; i++) {
            running[i] = BackgroundService.amIRunning(this, classes[i]);
            buttons[i].setOnClickListener(buttonClicks[i][running[i] ? 1 : 0]);
            buttons[i].setText(buttonTextResIds[i][running[i] ? 1 : 0]);
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
