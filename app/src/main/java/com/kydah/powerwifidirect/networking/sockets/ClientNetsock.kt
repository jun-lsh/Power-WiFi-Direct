package com.kydah.powerwifidirect.networking.sockets

import android.os.Handler
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class ClientNetsock(private var portNumber : Int, private var ipAddress : String, private var handler : Handler) : Thread() {

    override fun run() {
        val socket : Socket = Socket()
        socket.bind(null)
        try{
        socket.connect(InetSocketAddress(ipAddress, portNumber), 5000)}
        catch(e : SocketTimeoutException){
            println("Not on the correct network! " + e.printStackTrace())
            return
        } catch (e : ConnectException){
            println("Connection refused? Perhaps double connect? " + e.printStackTrace())
            return
        }


//        var socketHandler : SocketHandler = SocketHandler(socket, true)
//        socketHandler.start()
        var chat = SocketManager(socket, handler, "clt")
        Thread(chat).start()
    }

}