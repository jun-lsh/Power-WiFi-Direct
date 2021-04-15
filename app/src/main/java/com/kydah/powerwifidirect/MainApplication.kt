package com.kydah.powerwifidirect

import android.app.Application
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import kotlin.properties.Delegates

class MainApplication() : Application() {

    lateinit var accessPoint: SoftAccessPoint
    lateinit var serverNetsock : ServerNetsock
    var portNumber by Delegates.notNull<Int>()

}