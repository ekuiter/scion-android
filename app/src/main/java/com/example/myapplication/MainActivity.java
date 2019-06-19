package com.example.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

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

        startService(new Intent(this, DispatcherService.class));
        // output.setText(String.format(Locale.UK,"Dispatcher Main returned: %d", retValue));
    }
}
