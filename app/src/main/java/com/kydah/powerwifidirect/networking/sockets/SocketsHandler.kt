package com.kydah.powerwifidirect.networking.sockets

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message

import com.kydah.powerwifidirect.MainActivity.Companion.MESSAGE_READ
import com.kydah.powerwifidirect.MainActivity.Companion.MY_HANDLE
import com.kydah.powerwifidirect.networking.NetworkViewModel

class SocketsHandler(private val networkViewModel : NetworkViewModel) : Handler(Looper.getMainLooper()){

    override fun handleMessage(msg: Message) {
        when(msg.what){
            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray

                val readMessage = String(readBuf, 0, msg.arg1)

                println("Got message: $readMessage")

            }

            MY_HANDLE -> {
                val obj = msg.obj
                var chat = obj as SocketManager

                val helloBuffer = "Hello There from  :" + Build.VERSION.SDK_INT

                chat.write(helloBuffer.toByteArray())
                println("Wrote message: $helloBuffer")
            }
        }
    }
}