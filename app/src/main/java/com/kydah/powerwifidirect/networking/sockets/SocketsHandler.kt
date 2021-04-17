package com.kydah.powerwifidirect.networking.sockets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import com.kydah.powerwifidirect.MainActivity.Companion.GET_OBJ
import com.kydah.powerwifidirect.MainActivity.Companion.HELLO
import com.kydah.powerwifidirect.MainActivity.Companion.MESSAGE_READ
import com.kydah.powerwifidirect.MainActivity.Companion.MY_HANDLE
import com.kydah.powerwifidirect.networking.NetworkViewModel

class SocketsHandler(private val networkViewModel: NetworkViewModel, private val context : Context) : Handler(Looper.getMainLooper()){

    lateinit var socketManager: SocketManager

    @SuppressLint("HardwareIds")
    override fun handleMessage(msg: Message) {
        when(msg.what){

            GET_OBJ -> {
                socketManager = msg.obj as SocketManager
            }

            HELLO -> {
                val helloBuffer = socketManager.side + " " + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
                socketManager.write(helloBuffer.toByteArray())
            }

            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray
                val readMessage = String(readBuf, 0, msg.arg1)
                println("Got message: $readMessage")

            }
        }
    }

    fun filelistReq(hops : Int){
        socketManager.write(("client req fl $hops").toByteArray())
    }

    fun fileReq(filename: String, deviceId : String){
        socketManager.write(("client req f $filename $deviceId").toByteArray())
    }

    fun fileRes(filename: String, deviceId : String){
        socketManager.write(("server res f $filename $deviceId").toByteArray())
    }
}