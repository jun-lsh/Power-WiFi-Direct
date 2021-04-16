package com.kydah.powerwifidirect.networking.sockets

import android.os.Handler
import android.os.Looper
import android.os.Message

class SocketHandler : Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
    }
}