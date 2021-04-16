package com.kydah.powerwifidirect.networking

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint

class NetworkViewModel : ViewModel(){

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

    fun switchMode(){
        if(transmissionMode.value == "Server") {
            transmissionMode.value = "Client"
            startClientCoroutine()
        }
        else {
            transmissionMode.value = "Server"
        }
    }

    private fun startClientCoroutine(){
        accessPoint.value!!.terminateAP()
        serverNetsock.value!!.stopServer()
    }

}