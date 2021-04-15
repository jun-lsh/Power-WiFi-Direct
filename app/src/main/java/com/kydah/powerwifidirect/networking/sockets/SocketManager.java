package com.kydah.powerwifidirect.networking.sockets;

import android.os.Handler;
import android.util.Log;

import com.kydah.powerwifidirect.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketManager implements Runnable {
    private Socket socket = null;
    private Handler handler;

    public SocketManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    private InputStream iStream;
    private OutputStream oStream;


    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1048576]; //Megabyte buffer
            int bytes;
            handler.obtainMessage(MainActivity.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    handler.obtainMessage(MainActivity.MESSAGE_READ,bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    oStream.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

}

