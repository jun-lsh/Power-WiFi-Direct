package com.kydah.powerwifidirect

import android.app.Application
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import java.io.File
import kotlin.properties.Delegates

class MainApplication() : Application() {

    lateinit var accessPoint: SoftAccessPoint
    lateinit var serverNetsock : ServerNetsock

    lateinit var downloadsFolder : File
    lateinit var uploadsFolder : File

    var portNumber by Delegates.notNull<Int>()

}