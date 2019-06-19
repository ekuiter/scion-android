package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;


public class MainActivity extends AppCompatActivity {

    TextView output;

    static {
        System.loadLibrary("dispatcher-wrapper");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = findViewById(R.id.textview);


        new ChooserDialog(this)
                .withChosenListener((path, pathFile) -> startServices(path)).build().show();
    }

    private void startServices(String sciondCfgPath) {
        startService(new Intent(this, DispatcherService.class));
        startService(
                new Intent(this, SciondService.class)
                        .putExtra(SciondService.PARAM_CONFIG_PATH, sciondCfgPath)
        );
    }
}
