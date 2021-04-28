package com.kydah.powerwifidirect.networking

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import java.io.File

class NetworkViewModel : ViewModel(){

    val connectingToAP : MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }

    val deviceId : MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val downloadsFolder : MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    val uploadsFolder : MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    val transmissionMode : MutableLiveData<String> by lazy {
        MutableLiveData<String>("GO (Group Owner)")
    }

    val accessPoint : MutableLiveData<SoftAccessPoint> by lazy {
        MutableLiveData<SoftAccessPoint>()
    }

    val serverNetsock : MutableLiveData<ServerNetsock> by lazy {
        MutableLiveData<ServerNetsock>()
    }

    val legacyPeerList : MutableLiveData<HashSet<LegacyPeer>> by lazy {
        MutableLiveData<HashSet<LegacyPeer>>()
    }

    val fileList : MutableLiveData<ArrayList<PeerFile>> by lazy {
        MutableLiveData<ArrayList<PeerFile>>()
    }


}