/*
 * Copyright (C) 2019  Vera Clemens, Tom Kranz
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity {

    private static final String SCIOND_CFG_PATH = MainActivity.class.getCanonicalName() + ".SCIOND";
    private static final String DISP_CFG_PATH = MainActivity.class.getCanonicalName() + ".DISPATCHER";
    private static final String SCMP_CMD_LINE = MainActivity.class.getCanonicalName() + ".SCMPCMDLINE";
    static final String SERVICE_CHANNEL = MainActivity.class.getCanonicalName() + ".SERVICES";

    private Optional<String> sciondCfgPath;
    private Optional<String> dispCfgPath;
    private AppCompatButton sciondButton;
    private AppCompatButton dispButton;
    private AppCompatButton scmpButton;
    private TextInputEditText scmpCmdLine;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        Optional<Bundle> sIS = Optional.ofNullable(savedInstanceState);

        sciondCfgPath = sIS.map(i->i.getString(SCIOND_CFG_PATH));
        dispCfgPath = sIS.map(i->i.getString(DISP_CFG_PATH));

        createNotificationChannel();

        sciondButton = findViewById(R.id.sciondbutton);
        dispButton = findViewById(R.id.dispbutton);
        scmpButton = findViewById(R.id.scmpbutton);
        scmpCmdLine = findViewById(R.id.scmpcmdline);
        prefs = getPreferences(MODE_PRIVATE);

        activateButtons();
        scmpCmdLine.setText(sIS.map(i->i.getString(SCMP_CMD_LINE)).orElse(prefs.getString(SCMP_CMD_LINE, "")));

        sciondButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                    .withFilter(true, true)
                    .withStartFile(prefs.getString(SCIOND_CFG_PATH, null))
                    .withChosenListener((path, pathFile) ->
                            (sciondCfgPath = Optional.ofNullable(path)).ifPresent(p -> {
                                ensureWritePermissions();
                                startService(new Intent(this, SciondService.class)
                                        .putExtra(SciondService.PARAM_CONFIG_PATH, p));
                                putString(SCIOND_CFG_PATH, p);
                                activateButtons();
                            })
                    ).build().show()
        );
        dispButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosedispcfg, R.string.ok, R.string.cancel)
                    .withStartFile(prefs.getString(DISP_CFG_PATH, null))
                    .withChosenListener((path, pathFile) ->
                            (dispCfgPath = Optional.ofNullable(path)).ifPresent(p->{
                                ensureWritePermissions();
                                startService(new Intent(this, DispatcherService.class)
                                        .putExtra(DispatcherService.PARAM_CONFIG_PATH, p));
                                putString(DISP_CFG_PATH, p);
                                activateButtons();
                            })
                    ).build().show()
        );

        scmpButton.setOnClickListener(view -> {
            String cmdLine = Optional.ofNullable(scmpCmdLine.getText()).map(CharSequence::toString).orElse("");
            startService(
                    new Intent(this, ScmpService.class)
                            .putExtra(
                                    ScmpService.PARAM_ARGS_QUERY,
                                    BackgroundService.commandLine(cmdLine.split("\n"))
                            )
            );
            putString(SCMP_CMD_LINE, cmdLine);
        });
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
        dispCfgPath.ifPresent(putter.apply(DISP_CFG_PATH));
        Optional.ofNullable(scmpCmdLine.getText()).ifPresent(putter.apply(SCMP_CMD_LINE));
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
        sciondButton.setEnabled(!sciondCfgPath.isPresent());
        dispButton.setEnabled(!dispCfgPath.isPresent());
        scmpButton.setEnabled(sciondCfgPath.isPresent() && dispCfgPath.isPresent());
    }

    private void ensureWritePermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0
        );
    }
}
