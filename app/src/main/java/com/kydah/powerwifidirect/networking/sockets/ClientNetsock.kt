package com.kydah.powerwifidirect.networking.sockets

import android.os.Handler
import java.net.InetSocketAddress
import java.net.Socket

class ClientNetsock(private var portNumber : Int, private var ipAddress : String, private var handler : Handler) : Thread() {

    override fun run() {
        val socket : Socket = Socket()
        socket.bind(null)
        socket.connect(InetSocketAddress(ipAddress, portNumber), 5000)
//        var socketHandler : SocketHandler = SocketHandler(socket, true)
//        socketHandler.start()
        var chat = SocketManager(socket, handler, "clt")
        Thread(chat).start()
    }

}