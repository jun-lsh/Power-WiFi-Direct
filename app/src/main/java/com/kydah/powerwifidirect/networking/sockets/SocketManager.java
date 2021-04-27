package com.kydah.powerwifidirect.networking.sockets;

import android.os.Handler;
import android.os.Looper;

import com.kydah.powerwifidirect.activity.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private byte[] aByte = new byte[1];

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);


    public SocketManager(Socket socket, Handler handler, String side) {
        this.socket = socket;
        this.side = side;
        this.handler = handler;
    }

    private InputStream iStream;
    private OutputStream oStream;

    private ObjectOutputStream doStream;

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            System.out.println("SocketMan Open!");


            //pwOStream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(oStream)), true);

            //os before is as is is blocking!! :<
            doStream = new ObjectOutputStream(oStream);
            ObjectInputStream dataInputStream = new ObjectInputStream(iStream);
            boolean firstCall = true;
            handler.obtainMessage(MainActivity.GET_OBJ, this).sendToTarget();
            while (true) {
                try {
                    // Read from the InputStream
                    if(firstCall){
                        handler.obtainMessage(MainActivity.HELLO).sendToTarget();
                        firstCall = false;
                    }
                    byte[] prefixBytes = new byte[4];
                    int prefixLength;
                    dataInputStream.readFully(prefixBytes, 0, 4);
                    prefixLength = ByteBuffer.wrap(prefixBytes).order(ByteOrder.BIG_ENDIAN).getInt();
                    byte[] bytes = new byte[prefixLength];//(byte[]) dataInputStream.readObject();//
//                    prefixLength = bytes.length;
                    System.out.println("Received buffer: " + prefixLength);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    int bytesRead;
                    while(prefixLength != 0){
                        dataInputStream.readFully(aByte);
                        byteArrayOutputStream.write(aByte, 0, aByte.length);
                        prefixLength--;
                    }

                    byte[] bytesBaos = byteArrayOutputStream.toByteArray();

                    handler.obtainMessage(MainActivity.MESSAGE_READ,  bytesBaos.length , -1, bytesBaos ).sendToTarget();
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    System.out.println("WHAT??");
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
                            System.out.println("Buffer size: " + buffer.length);
                            //doStream.write(htonl(buffer.length), 0, 4);
                            doStream.write(buffer, 0, buffer.length);
                            doStream.flush();
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
        executorService.submit(() -> {
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
//                        pwOStream.write((buffer.length >> BYTESHIFT) & BYTEMASK);
//                        pwOStream.write(buffer.length & BYTEMASK);
                        System.out.println("Buffer size: " + buffer.length);
                        doStream.write(htonl(buffer.length), 0, 4);
//                        doStream.flush();
//                        doStream.writeInt(buffer.length);
                        //doStream.write(0);
                        doStream.write(buffer);
                        doStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 100, TimeUnit.MILLISECONDS);
        });
    }


    public String getSide(){
        return side;
    }

    //we using C++ out here god DAMN IT

    private byte[] htonl(int value){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

}

