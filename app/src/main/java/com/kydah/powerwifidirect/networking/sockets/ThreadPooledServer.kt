package com.kydah.powerwifidirect.networking.sockets

import android.content.Context
import android.os.Handler
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class ThreadPooledServer(private var port: Int, private var handler: Handler, private var context : Context) : Runnable {
    private var serverSocket: ServerSocket? = null

    @get:Synchronized
    private var isStopped = false
    private var runningThread: Thread? = null
    private val threadPool = Executors.newFixedThreadPool(25)
    override fun run() {
        synchronized(this) { runningThread = Thread.currentThread() }
        openServerSocket()
        while (!isStopped) {
            var clientSocket: Socket? = null
            clientSocket = try {
                serverSocket!!.accept()
            } catch (e: IOException) {
                if (isStopped) {
                    println("Server Stopped.")
                    break
                }
                throw RuntimeException(
                        "Error accepting client connection", e)
            }
            threadPool.execute(
                    SocketManager(clientSocket, handler,
                            "svr", context))
        }
        threadPool.shutdown()
        println("Server Stopped.")
    }

    @Synchronized
    fun stop() {
        isStopped = true
        try {
            serverSocket!!.close()
        } catch (e: IOException) {
            throw RuntimeException("Error closing server", e)
        }
    }

    private fun openServerSocket() {
        try {
            serverSocket = ServerSocket(port)
        } catch (e: IOException) {
            throw RuntimeException("Cannot open port $port", e)
        }
    }

}