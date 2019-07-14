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

    public static final int MSG_INIT = 0;
    public static final int MSG_DIAL_SCION = 1;
    public static final int MSG_WRITE = 2;
    public static final int MSG_READ_FROM = 30;
    public static final int MSG_READ_FROM_RESULT = 31;

    public static final String CLIENT_ADDRESS = "org.scionlab.CLIENT_ADDRESS";
    public static final String SERVER_ADDRESS = "org.scionlab.SERVER_ADDRESS";
    public static final String WRITE_BUFFER = "org.scionlab.WRITE_BUFFER";
    public static final String READ_BUFFER = "org.scionlab.READ_BUFFER";
    public static final String BUFFER_SIZE = "org.scionlab.BUFFER_SIZE";

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
            applicationContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Received message: " + msg.what);
            Bundle bundle = msg.getData();
            String clientAddress;

            switch (msg.what) {
                case MSG_INIT:
                    clientAddress = bundle.getString(CLIENT_ADDRESS);
                    Snet.init(applicationContext.getFilesDir().getAbsolutePath(), "run/shm/sciond/default.sock", "run/shm/dispatcher/default.sock", clientAddress);
                    break;
                case MSG_DIAL_SCION:
                    clientAddress = bundle.getString(CLIENT_ADDRESS);
                    String serverAddress = bundle.getString(SERVER_ADDRESS);
                    Log.d(TAG, "Dialing SCION: Local: " + clientAddress + " Remote: " + serverAddress);
                    Snet.dialScion(serverAddress, clientAddress);
                    break;
                case MSG_WRITE:
                    Log.d(TAG, "Writing to SCION");
                    byte[] writeBuffer = bundle.getByteArray(WRITE_BUFFER);
                    Snet.write(writeBuffer);
                    break;
                case MSG_READ_FROM:
                    Log.d(TAG, "Reading from SCION");
                    long bufferSize = bundle.getLong(BUFFER_SIZE, 2500);
                    byte[] readBuffer = Snet.readFrom(bufferSize);
                    Log.d(TAG, "Read from SCION");
                    Bundle replyBundle = new Bundle();
                    replyBundle.putByteArray(READ_BUFFER, readBuffer);
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
