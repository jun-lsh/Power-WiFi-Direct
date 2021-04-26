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

    private BufferedOutputStream doStream;

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            BufferedInputStream dataInputStream = new BufferedInputStream(iStream);
            //pwOStream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(oStream)), true);
            doStream = new BufferedOutputStream(oStream);

            handler.obtainMessage(MainActivity.GET_OBJ, this).sendToTarget();
            handler.obtainMessage(MainActivity.HELLO).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream

                    int prefixLength;
                    ByteArrayOutputStream prefixLengthBytes = new ByteArrayOutputStream();
                    try {
                        //dataInputStream.readFully(prefixLengthBytes);
                        //prefixLength = ((prefixLengthBytes[0] >> BYTESHIFT) & BYTEMASK) + prefixLengthBytes[1] & BYTEMASK;//dataInputStream.readUnsignedShort();
                        for(int i = 0; i < 4; i++) {
                            if(dataInputStream.read(aByte) != -1) prefixLengthBytes.write(aByte);
                        }
                        prefixLength = ByteBuffer.wrap(prefixLengthBytes.toByteArray()).order(ByteOrder.BIG_ENDIAN).getInt();
                        //prefixLength = dataInputStream.readInt();
                        System.out.println("Received buffer: " + prefixLength);
                        if(prefixLength == 0){}
                    } catch (EOFException e){
                        //break;
                        continue;
                    }

                    if(prefixLength > MAXMESSSAGELENGTH) continue;

                    //byte[] bytes = new byte[prefixLength];
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    //dataInputStream.readFully(bytes, 0, prefixLength);

                    while(prefixLength > 0){
                        int len = dataInputStream.read(aByte);
                       // System.out.println(len);
                        if(len != -1) {
                         //   System.out.println("received: " + aByte[0]);
                            byteArrayOutputStream.write(aByte,0,len);
                            prefixLength-=len;
                        }
                    }

                    byte[] bytes = byteArrayOutputStream.toByteArray();
                    handler.obtainMessage(MainActivity.MESSAGE_READ, prefixLength , -1, bytes).sendToTarget();
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
                            doStream.write(htonl(buffer.length), 0, 4);
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
                        doStream.write(buffer, 0, buffer.length);
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

