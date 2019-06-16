package com.example.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.*;
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
        File confFile = new File(getFilesDir(), "dispatcher.conf");
        try {
            InputStream in = getAssets().open(confFile.getName());
            OutputStream out = new FileOutputStream(confFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                Log.d("TAG", String.format("Wrote %d bytes to %s", read, confFile.getAbsolutePath()));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean existed;
        boolean success;
        File shm = new File(getFilesDir(), "run/shm");
        existed = shm.exists();
        success = shm.mkdirs();
        Log.d("TAG", String.format("Creating shm dir %s: %b %b %b", shm.getAbsolutePath(), existed, success, shm.exists()));
        File logs = new File(getFilesDir(), "logs");
        existed = logs.exists();
        success = logs.mkdirs();
        Log.d("TAG", String.format("Creating logs dir %s: %b %b %b", logs.getAbsolutePath(), existed, success, logs.exists()));
        File socket = new File(shm, "dispatcher/default.sock");
        existed = socket.exists();
        success = socket.delete();
        Log.d("TAG", String.format("Deleting dispatcher socket %s: %b %b %b", socket.getAbsolutePath(), existed, success, socket.exists()));
        int retValue = main(confFile.getAbsolutePath());
        output.setText(String.format(Locale.UK,"Dispatcher Main returned: %d", retValue));
    }

    public native int main(String confFileName);
}
