package org.scionlab.endhost;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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
    private static final String PINGPONG_CMD_LINE = MainActivity.class.getCanonicalName() + ".PPCMDLINE";
    static final String SERVICE_CHANNEL = MainActivity.class.getCanonicalName() + ".SERVICES";

    private Optional<String> sciondCfgPath;
    private Optional<String> dispCfgPath;
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

        createNotificationChannel();

        sciondButton = findViewById(R.id.sciondbutton);
        dispButton = findViewById(R.id.dispbutton);
        pingButton = findViewById(R.id.pingpongbutton);
        pingCmdLine = findViewById(R.id.pingpongcmdline);
        prefs = getPreferences(MODE_PRIVATE);

        activateButtons();
        pingCmdLine.setText(sIS.map(i->i.getString(PINGPONG_CMD_LINE)).orElse(prefs.getString(PINGPONG_CMD_LINE, "")));

        sciondButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
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

        pingButton.setOnClickListener(view -> {
            String cmdLine = Optional.ofNullable(pingCmdLine.getText()).map(CharSequence::toString).orElse("");
            startService(
                    new Intent(this, PingpongService.class)
                            .putExtra(
                                    PingpongService.PARAM_ARGS_QUERY,
                                    BackgroundService.commandLine(cmdLine.split("\n"))
                            )
            );
            putString(PINGPONG_CMD_LINE, cmdLine);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Function<String,Consumer<CharSequence>> putter = key->cs->outState.putString(key, cs.toString());
        sciondCfgPath.ifPresent(putter.apply(SCIOND_CFG_PATH));
        dispCfgPath.ifPresent(putter.apply(DISP_CFG_PATH));
        Optional.ofNullable(pingCmdLine.getText()).ifPresent(putter.apply(PINGPONG_CMD_LINE));
    }

    private void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
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

    private void activateButtons() {
        sciondButton.setEnabled(!sciondCfgPath.isPresent());
        dispButton.setEnabled(!dispCfgPath.isPresent());
        pingButton.setEnabled(sciondCfgPath.isPresent() && dispCfgPath.isPresent());
    }

    private void ensureWritePermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0
        );
    }
}
