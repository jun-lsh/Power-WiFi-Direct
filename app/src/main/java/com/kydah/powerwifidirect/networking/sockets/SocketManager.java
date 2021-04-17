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
    private String side;

    public SocketManager(Socket socket, Handler handler, String side) {
        this.socket = socket;
        this.side = side;
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

            //client/server HELLO

            handler.obtainMessage(MainActivity.GET_OBJ, this).sendToTarget();
            handler.obtainMessage(MainActivity.HELLO).sendToTarget();


            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
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

    public void closeConnection(){
        write("close_con".getBytes());
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    public String getSide(){
        return side;
    }

}

