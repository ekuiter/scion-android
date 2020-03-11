package org.scionlab.scion;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context context;

    public UncaughtExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace();
            e.printStackTrace(new PrintWriter(sw));

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, sw.toString());
            intent.setType("text/plain");
            context.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(1);
    }
}
