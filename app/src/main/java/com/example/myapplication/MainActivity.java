package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Optional;


public class MainActivity extends AppCompatActivity {

    private static final String SCIOND_CFG_PATH = MainActivity.class.getCanonicalName() + ".SCIOND";
    private static final String DISP_CFG_PATH = MainActivity.class.getCanonicalName() + ".DISPATCHER";
    static final String SERVICE_CHANNEL = MainActivity.class.getCanonicalName() + ".SERVICES";

    private Optional<String> sciondCfgPath;
    private Optional<String> dispCfgPath;
    private AppCompatButton sciondButton;
    private AppCompatButton dispButton;

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

        sciondButton.setEnabled(!sciondCfgPath.isPresent());
        dispButton.setEnabled(!dispCfgPath.isPresent());

        sciondButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosesciondcfg, R.string.ok, R.string.cancel)
                    .withChosenListener((path, pathFile) -> {
                        sciondCfgPath = Optional.ofNullable(path);
                        sciondButton.setEnabled(!sciondCfgPath.isPresent());
                        startServices();
                    }).build().show()
        );
        dispButton.setOnClickListener(view ->
            new ChooserDialog(view.getContext())
                    .withResources(R.string.choosedispcfg, R.string.ok, R.string.cancel)
                    .withChosenListener((path, pathFile) -> {
                        dispCfgPath = Optional.ofNullable(path);
                        dispButton.setEnabled(!dispCfgPath.isPresent());
                        startServices();
                    }).build().show()
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        sciondCfgPath.ifPresent(s->outState.putString(SCIOND_CFG_PATH, s));
        dispCfgPath.ifPresent(s->outState.putString(DISP_CFG_PATH, s));
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
            startService(
                    new Intent(this, DispatcherService.class)
                            .putExtra(DispatcherService.PARAM_CONFIG_PATH, dispCfgPath.get())
            );
            startService(
                    new Intent(this, SciondService.class)
                            .putExtra(SciondService.PARAM_CONFIG_PATH, sciondCfgPath.get())
            );
        }
    }
}
