package com.kydah.powerwifidirect.networking.sockets

import android.os.Handler
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerNetsock(private var portNumber : Int, private var handler : Handler) : Thread() {

    private lateinit var serverSocket: ServerSocket
    private var running = false

    fun startServer(){
        try {
            serverSocket = ServerSocket(portNumber)
            running = true
            this.start()
        } catch (e : IOException){
            e.printStackTrace()
        }
    }

    fun stopServer(){
        running = false
        serverSocket.close()
        println("Socket server successfully shut down!")
        //this.interrupt()
    }

    override fun run() {
        running = true
        println("Socket server started! Running on port: $portNumber")
        while(running){
            try {
                //make a blocking accept
                println("Blocking accept!")
                var socket : Socket = serverSocket.accept()
//                var socketHandler : SocketHandler = SocketHandler(socket, false)
//                socketHandler.start()
                var chat = SocketManager(socket, handler)
                Thread(chat).start()
            } catch (e : IOException){
                e.printStackTrace()
            }
        }
    }

}