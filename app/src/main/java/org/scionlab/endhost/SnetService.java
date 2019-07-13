package org.scionlab.endhost;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import snet.Snet;

public class SnetService extends Service {
    private static final String TAG = "snet";

    static final int MSG_DIAL_SCION = 1;
    static final int MSG_WRITE = 2;
    static final int MSG_READ_FROM = 30;
    static final int MSG_READ_FROM_RESULT = 31;

    static boolean isInitialized = false;

    Messenger messenger;

    @Override
    public IBinder onBind (Intent intent) {
        Log.d(TAG, "Service bound");
        // only called the first time the service is bound by a client -- all other clients receive the same IBinder
        messenger = new Messenger(new IncomingHandler(this));
        return messenger.getBinder();
    }

    static class IncomingHandler extends Handler {
        private Context applicationContext;

        IncomingHandler(Context context) {
            applicationContext = context.getApplicationContext();
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Received message: " + msg.what);
            Bundle bundle = msg.getData();

            if (!isInitialized) {
                Log.d(TAG, "Initializing SCION");
                String clientAddress = bundle.getString("org.scionlab.CLIENT_ADDRESS");
                Snet.init(applicationContext.getFilesDir().getAbsolutePath(), "run/shm/sciond/default.sock", "run/shm/dispatcher/default.sock", clientAddress);
                isInitialized = true;
            }

            switch (msg.what) {
                case MSG_DIAL_SCION:
                    String serverAddress = bundle.getString("org.scionlab.SERVER_ADDRESS");
                    String clientAddress = bundle.getString("org.scionlab.CLIENT_ADDRESS");
                    Log.d(TAG, "Dialing SCION: Local: " + clientAddress + " Remote: " + serverAddress);
                    Snet.dialScion(serverAddress, clientAddress);
                    break;
                case MSG_WRITE:
                    Log.d(TAG, "Writing to SCION");
                    byte[] writeBuffer = bundle.getByteArray("com.scionlab.WRITE_BUFFER");
                    Snet.write(writeBuffer);
                    break;
                case MSG_READ_FROM:
                    Log.d(TAG, "Reading from SCION");
                    byte[] readBuffer = Snet.readFrom(2500);
                    Log.d(TAG, "Read from SCION");
                    Bundle replyBundle = new Bundle();
                    replyBundle.putByteArray("org.scionlab.READ_FROM_RESULT", readBuffer);
                    Messenger replyMessenger = msg.replyTo;
                    Message reply = Message.obtain(null, MSG_READ_FROM_RESULT, 0,0);
                    reply.setData(replyBundle);
                    try {
                        replyMessenger.send(reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
