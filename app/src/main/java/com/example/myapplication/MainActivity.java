package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Optional;


public class MainActivity extends AppCompatActivity {

    private static final String SCIOND_CFG_PATH = MainActivity.class.getCanonicalName() + ".SCIOND";
    private static final String DISP_CFG_PATH = MainActivity.class.getCanonicalName() + ".DISPATCHER";
    private static final String SERVICES_STARTED = MainActivity.class.getCanonicalName() + ".SERVICES_STARTED";
    private static final String PINGPONG_CMD_LINE = MainActivity.class.getCanonicalName() + ".PPCMDLINE";
    static final String SERVICE_CHANNEL = MainActivity.class.getCanonicalName() + ".SERVICES";

    private Optional<String> sciondCfgPath;
    private Optional<String> dispCfgPath;
    private boolean servicesStarted;
    private AppCompatButton sciondButton;
    private AppCompatButton dispButton;
    private AppCompatButton pingButton;
    private TextInputEditText pingCmdLine;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Optional<Bundle> sIS = Optional.ofNullable(savedInstanceState);

        sciondCfgPath = sIS.map(i->i.getString(SCIOND_CFG_PATH));
        dispCfgPath = sIS.map(i->i.getString(DISP_CFG_PATH));
        servicesStarted = sIS.map(i->i.getBoolean(SERVICES_STARTED)).orElse(false);

        createNotificationChannel();

        sciondButton = findViewById(R.id.sciondbutton);
        dispButton = findViewById(R.id.dispbutton);
        pingButton = findViewById(R.id.pingpongbutton);
        pingCmdLine = findViewById(R.id.pingpongcmdline);
        prefs = getPreferences(MODE_PRIVATE);

        sciondButton.setEnabled(!sciondCfgPath.isPresent() && !servicesStarted);
        dispButton.setEnabled(!dispCfgPath.isPresent() && !servicesStarted);
        pingButton.setEnabled(servicesStarted);
        pingCmdLine.setText(sIS.map(i->i.getString(PINGPONG_CMD_LINE)).orElse(prefs.getString(PINGPONG_CMD_LINE, "")));

        sciondButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                    .withStartFile(prefs.getString(SCIOND_CFG_PATH, null))
                    .withChosenListener((path, pathFile) -> {
                        sciondCfgPath = Optional.ofNullable(path);
                        sciondButton.setEnabled(!sciondCfgPath.isPresent());
                        startServices();
                    }).build().show()
        );
        dispButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosedispcfg, R.string.ok, R.string.cancel)
                    .withStartFile(prefs.getString(DISP_CFG_PATH, null))
                    .withChosenListener((path, pathFile) -> {
                        dispCfgPath = Optional.ofNullable(path);
                        dispButton.setEnabled(!dispCfgPath.isPresent());
                        startServices();
                    }).build().show()
        );

        pingButton.setOnClickListener(view -> {
            startService(
                new Intent(this, PingpongService.class)
                        .putExtra(PingpongService.PARAM_ARGS_QUERY, BackgroundService.commandLine(
                                pingCmdLine.getText().toString().split("\n")
                        ))
            );
            prefs.edit().putString(PINGPONG_CMD_LINE, pingCmdLine.getText().toString()).apply();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        sciondCfgPath.ifPresent(s->outState.putString(SCIOND_CFG_PATH, s));
        dispCfgPath.ifPresent(s->outState.putString(DISP_CFG_PATH, s));
        outState.putBoolean(SERVICES_STARTED, servicesStarted);
        outState.putString(PINGPONG_CMD_LINE, pingCmdLine.getText().toString());
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.servicechannel_name);
        String description = getString(R.string.servicechannel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void startServices() {
        if (sciondCfgPath.isPresent() && dispCfgPath.isPresent()) {
            SharedPreferences.Editor e = prefs.edit();
            startService(
                    new Intent(this, DispatcherService.class)
                            .putExtra(DispatcherService.PARAM_CONFIG_PATH, dispCfgPath.get())
            );
            startService(
                    new Intent(this, SciondService.class)
                            .putExtra(SciondService.PARAM_CONFIG_PATH, sciondCfgPath.get())
            );
            servicesStarted = true;
            pingButton.setEnabled(true);
            e.putString(SCIOND_CFG_PATH, sciondCfgPath.get());
            e.putString(DISP_CFG_PATH, dispCfgPath.get());
            e.apply();
        }
    }
}
