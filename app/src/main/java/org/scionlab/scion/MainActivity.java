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
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

import org.scionlab.scion.as.ScionAS;

import java.io.Serializable;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String SCIONLAB_CONFIGURATION_URI = MainActivity.class.getCanonicalName() + ".SCIONLAB_CONFIGURATION_URI";
    private static final String PING_ADDRESS = MainActivity.class.getCanonicalName() + ".PING_ADDRESS";
    private static final String UPDATE_USER_INTERFACE = MainActivity.class.getCanonicalName() + ".UPDATE_USER_INTERFACE";
    private static final String SCION_STATE = MainActivity.class.getCanonicalName() + ".SCION_STATE";
    private static final String COMPONENT_STATE = MainActivity.class.getCanonicalName() + ".COMPONENT_STATE";

    private SharedPreferences preferences;
    private BroadcastReceiver updateUserInterfaceReceiver;
    private String scionLabConfigurationUri;
    private String pingAddress;

    private ScionControlFragment controlFragment;
    private SensorFetcherFragment sensorFetcherFragment;
    private LogActivity logFragment;
    private WebActivity howtoFragment;
    private WebActivity aboutFragment;

    private NavigationView navigationView;
    private DrawerLayout drawer;

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
        //Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle abdt = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawerOpen, R.string.drawerClosed);
        drawer.addDrawerListener(abdt);

        getSupportActionBar().setHomeButtonEnabled(true);
        abdt.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                showFragment(item.getItemId());
                return false;
            }
        });

        showFragment(R.id.nav_scion_control);

        preferences = getPreferences(MODE_PRIVATE);
        pingAddress = preferences.getString(PING_ADDRESS, getResources().getString(R.string.pingAddress));
        scionLabConfigurationUri = preferences.getString(SCIONLAB_CONFIGURATION_URI, null);
    }

    private void showFragment(int id) {

        navigationView.setCheckedItem(id);
        drawer.closeDrawer(GravityCompat.START);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;

        switch (id) {
            case R.id.nav_scion_control:
                if (controlFragment == null) {
                    controlFragment = new ScionControlFragment();
                }
                fragment = controlFragment;
                break;
            case R.id.nav_sensor_fetcher:
                if (sensorFetcherFragment == null) {
                    sensorFetcherFragment = new SensorFetcherFragment();
                }
                fragment = sensorFetcherFragment;
                break;
            case R.id.nav_log:
                if (logFragment == null) {
                    logFragment = new LogActivity();
                }
                fragment = logFragment;
                break;
            case R.id.nav_howto:
                if (howtoFragment == null) {
                    WebActivity f = new WebActivity();
                    f.ContentURL = "file:///android_asset/how.html";
                    howtoFragment = f;
                }
                fragment = howtoFragment;
                break;
            case R.id.nav_about:
                if (aboutFragment == null) {
                    WebActivity f = new WebActivity();
                    f.ContentURL = "file:///android_asset/about.html";
                    aboutFragment = f;
                }
                fragment = aboutFragment;
                break;
        }

        if (fragment != null) {
            ft.replace(R.id.main_activity_fragment_view, fragment);
            ft.commit();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    private void updateUserInterface(ScionAS.State state, Map<String, ScionAS.State> componentState) {
        if (controlFragment != null)
            controlFragment.updateUserInterface(state, componentState);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;
        Uri uri = data.getData();
        if (uri != null) {
            scionLabConfigurationUri = uri.toString();
            preferences.edit().putString(SCIONLAB_CONFIGURATION_URI, scionLabConfigurationUri).apply();
            ScionService.start(this, scionLabConfigurationUri, pingAddress);
        }
    }
}
