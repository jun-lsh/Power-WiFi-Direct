package com.kydah.powerwifidirect.networking.sockets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kydah.powerwifidirect.activity.MainActivity;
import com.kydah.powerwifidirect.utils.NotificationUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketManager implements Runnable {

    private static final int MAXMESSSAGELENGTH = 65535;

    private static final byte[] byteDelim  = new byte[]{(byte) 0xFE,(byte) 0xFE,(byte) 0xFE};

    private final BlockingQueue<byte[]> writeQueue = new ArrayBlockingQueue<byte[]>(1024);

    private Socket socket = null;
    private Handler handler;
    private String side;
    private Context context;
    private int cumBytesRec = 0;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    private final ExecutorService executorService = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setPriority(7);
        return thread;
    });

    public SocketManager(Socket socket, Handler handler, String side, Context context) {
        this.socket = socket;
        this.side = side;
        this.handler = handler;
        this.context = context;
    }

    private InputStream iStream;
    private OutputStream oStream;

    private DataOutputStream doStream;

    @Override
    public void run() {

        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            System.out.println("SocketMan Open!");

            doStream = new DataOutputStream(oStream);
            //pwOStream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(oStream)), true);
            //os before is as is is blocking!! :<
            boolean firstCall = true;
            handler.obtainMessage(MainActivity.GET_OBJ, this).sendToTarget();
            DataInputStream dataInputStream = new DataInputStream(iStream);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                    while(!writeQueue.isEmpty()){
                        try {
                            writeBuffer(writeQueue.take());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }}
                }
            }).start();

            while (true) {
                try {
                    // Read from the InputStream
                    if(firstCall){
                        handler.obtainMessage(MainActivity.HELLO).sendToTarget();
                        firstCall = false;
                    }
                    //System.out.println("writing...");
                    //write("AAAAAAAAAA".getBytes(Charset.forName("ASCII")));
                    byte[] prefixBytes = new byte[4];
                    int prefixLength;
                    int consecDelim = 0;
                    do {
                        byte pain = dataInputStream.readByte();
                        cumBytesRec++;
                        if (pain == (byte) 0xFE) consecDelim++;
                        else consecDelim = 0;
                        System.out.println("checking " + consecDelim + " " + pain + " " + (byte) 0xFE);
                    } while (consecDelim != 3);

                    dataInputStream.readFully(prefixBytes, 0, 4);
                    cumBytesRec += 4;
                    //if(state == -1) break;
                    prefixLength = ByteBuffer.wrap(prefixBytes).order(ByteOrder.BIG_ENDIAN).getInt();
                    System.out.println("Received buffer: " + prefixLength);

                    byte[] bytes = new byte[prefixLength];
                    consecDelim = 0;
                    do {
                        byte pain = dataInputStream.readByte();
                        cumBytesRec++;
                        if (pain == (byte) 0xFE) consecDelim++;
                        else consecDelim = 0;
                        System.out.println("checking " + consecDelim + " " + pain + " " + (byte) 0xFE);
                    } while (consecDelim != 3);

                    dataInputStream.readFully(bytes, 0, prefixLength);
                    cumBytesRec += prefixLength;
                    System.out.println(cumBytesRec);

                    handler.obtainMessage(MainActivity.MESSAGE_READ,  prefixLength , -1, Arrays.copyOf(bytes, prefixLength)).sendToTarget();
                    //write("ACK".getBytes(StandardCharsets.UTF_8));
                    //byteArrayOutputStream.close();
                } catch (IOException e) {
                    System.out.println("WHAT?? " );
                    e.printStackTrace();
                    try {
                        oStream.close();
                        iStream.close();
                        dataInputStream.close();
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void closeConnection(String toSend){
        write("toSend".getBytes());
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void readFile(File file, boolean temp, String target){
        byte[] buffer = new byte[MAXMESSSAGELENGTH];
        try {
            int fileSize = (int) Math.ceil(file.length() / 65535.0);
            int cumRc = 0;

            FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
            try {
                int rc;
                while((rc = inputStream.read(buffer)) > -1){
                    cumRc += rc;
                    byte[] block = new byte[rc];
                    System.arraycopy(buffer, 0, block, 0, rc);
                    NotificationUtils.Companion.pushUploadingNotification(file.getName(), target, fileSize, (int) Math.ceil(cumRc / 65535.0), false, context);
                    if(buffer.length > MAXMESSSAGELENGTH) throw new IOException("message too long");
                    write(block);
                }
                inputStream.close();
                System.out.println("Completed file transfer!");
                NotificationUtils.Companion.pushUploadingNotification(file.getName(), target, 0,0, false, context);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(temp) file.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    public void write(byte[] buffer) {
        try {

            writeQueue.put(buffer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeBuffer(byte[] buffer){
        try {
            if(buffer.length > MAXMESSSAGELENGTH) throw new IOException("message too long");
            System.out.println("Buffer size: " + buffer.length);

            byte[] prefixLength = htonl(buffer.length);
            byte[] appendPain = Arrays.copyOf(byteDelim, byteDelim.length + prefixLength.length);
            System.arraycopy(prefixLength, 0, appendPain, byteDelim.length, prefixLength.length);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(appendPain);

            //oStream.write(htonl(buffer.length), 0, htonl( buffer.length).length);
            byte[] lengthBuffer = new byte[appendPain.length];
            int count;
            while((count = byteArrayInputStream.read(lengthBuffer)) > 0){
                // System.out.println("Buffer length bytes size: " + count);
                doStream.write(lengthBuffer, 0, count);
            }
            //oStream.write(0);
            //oStream.flush();
            doStream.flush();
            appendPain = Arrays.copyOf(byteDelim, byteDelim.length + buffer.length);
            System.arraycopy(buffer, 0, appendPain, byteDelim.length, buffer.length);

            //oStream.write);
            byteArrayInputStream = new ByteArrayInputStream(appendPain);
            lengthBuffer = new byte[appendPain.length];
            while((count = byteArrayInputStream.read(lengthBuffer)) > 0){
                doStream.write(lengthBuffer, 0, count);
            }
            //oStream.write(buffer, 0, buffer.length);
            doStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.execute(() -> { new Runnable() {
            @Override
            public void run() {
                System.out.println("made it");
            }
        };
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

