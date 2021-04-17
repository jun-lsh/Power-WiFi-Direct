package com.kydah.powerwifidirect.networking

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.AccessPointConnection
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import java.io.File
import kotlin.coroutines.coroutineContext

class NetworkViewModel : ViewModel(){

    val downloadsFolder : MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    val uploadsFolder : MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    val transmissionMode : MutableLiveData<String> by lazy {
        MutableLiveData<String>("Server")
    }

    val accessPoint : MutableLiveData<SoftAccessPoint> by lazy {
        MutableLiveData<SoftAccessPoint>()
    }

    val serverNetsock : MutableLiveData<ServerNetsock> by lazy {
        MutableLiveData<ServerNetsock>()
    }

    val peerList : MutableLiveData<HashSet<Peer>> by lazy {
        MutableLiveData<HashSet<Peer>>()
    }

    val socketsHandler : MutableLiveData<SocketsHandler> by lazy {
        MutableLiveData<SocketsHandler>()
    }

    fun switchMode(){
        if(transmissionMode.value == "Server") {
            transmissionMode.value = "Client"
        } else {
            transmissionMode.value = "Server"
        }
    }

}