package com.kydah.powerwifidirect.networking.sockets;

import android.os.Handler;
import android.os.Looper;

import com.kydah.powerwifidirect.activity.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketManager implements Runnable {

    private static final int MAXMESSSAGELENGTH = 65535;
    private static final int BYTEMASK = 0xff;
    private static final int SHORTMASK = 0xffff;
    private static final int BYTESHIFT = 8;

    private Socket socket = null;
    private Handler handler;
    private String side;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

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

            DataInputStream dataInputStream = new DataInputStream(iStream);


            handler.obtainMessage(MainActivity.GET_OBJ, this).sendToTarget();
            handler.obtainMessage(MainActivity.HELLO).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    int prefixLength;
                    try {
                        prefixLength = dataInputStream.readUnsignedShort();
                        System.out.println(prefixLength);
                    } catch (EOFException e){
                        break;
                    }
                    byte[] bytes = new byte[prefixLength];
                    dataInputStream.readFully(bytes);
                    //bytes = dataInputStream.read(buffer,0, prefixLength-1);
//                    if (bytes == -1) {
//                        break;
//                    }

                    handler.obtainMessage(MainActivity.MESSAGE_READ, prefixLength , -1, bytes).sendToTarget();
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

    public void readFile(File file){
        write(("svr res wf " + file.getName() + " " + (int) Math.ceil(file.length()/65535.0)).getBytes());
        byte[] buffer = new byte[MAXMESSSAGELENGTH];
        try {
            FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
//                    DataOutputStream dataOutputStream = new DataOutputStream(oStream);
//                    dataOutputStream.writeInt(buffer.length+1);
//                    dataOutputStream.write(0);
//                    dataOutputStream.write(buffer);
//                    dataOutputStream.flush();

                        int rc = inputStream.read(buffer);
                        while(rc != -1){
                            if(buffer.length > MAXMESSSAGELENGTH) throw new IOException("message too long");
                            oStream.write((buffer.length >> BYTESHIFT) & BYTEMASK);
                            oStream.write(buffer.length & BYTEMASK);
                            oStream.write(buffer);
                            oStream.flush();
                            rc = inputStream.read(buffer);
                        }
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 100, TimeUnit.MILLISECONDS);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public void write(byte[] buffer) {
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
//                    DataOutputStream dataOutputStream = new DataOutputStream(oStream);
//                    dataOutputStream.writeInt(buffer.length+1);
//                    dataOutputStream.write(0);
//                    dataOutputStream.write(buffer);
//                    dataOutputStream.flush();
                    if(buffer.length > MAXMESSSAGELENGTH) throw new IOException("message too long");
                    oStream.write((buffer.length >> BYTESHIFT) & BYTEMASK);
                    oStream.write(buffer.length & BYTEMASK);
                    oStream.write(buffer);
                    oStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 100, TimeUnit.MILLISECONDS);
    }


    public String getSide(){
        return side;
    }

}

