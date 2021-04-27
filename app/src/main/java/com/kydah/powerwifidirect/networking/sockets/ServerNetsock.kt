package com.kydah.powerwifidirect.networking.sockets

import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerNetsock(private var portNumber : Int, private var handler : Handler) : Thread() {

    private lateinit var serverSocket: ServerSocket
    private var running = false
    private lateinit var threadPooledServer: ThreadPooledServer

    fun startServer(){
        try {
            //serverSocket = ServerSocket(portNumber)
            threadPooledServer = ThreadPooledServer(portNumber, handler)
            running = true
            this.start()
        } catch (e : IOException){
            e.printStackTrace()
        }
    }

    fun stopServer(){
        running = false
        //serverSocket.close()
        threadPooledServer.stop()
        println("Socket server successfully shut down!")
        //this.interrupt()
    }

    override fun run() {
        running = true
        println("Socket server started! Running on port: $portNumber")
//        while(running){
//            try {
//                //make a blocking accept
//                println("Blocking accept!")
//                var socket : Socket = serverSocket.accept()
////                var socketHandler : SocketHandler = SocketHandler(socket, false)
////                socketHandler.start()
//                var chat = SocketManager(socket, handler, "svr")
//                Thread(chat).start()
//            } catch (e : IOException){
//                e.printStackTrace()
//            }
//        }
        Thread(threadPooledServer).start()
    }

}