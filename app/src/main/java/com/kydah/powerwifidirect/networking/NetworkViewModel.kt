package com.kydah.powerwifidirect.networking

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint

class NetworkViewModel : ViewModel(){

    public val softAccessPoint : MutableLiveData<SoftAccessPoint> by lazy {
        MutableLiveData<SoftAccessPoint>()
    }

    public val serverNetsock : MutableLiveData<ServerNetsock> by lazy {
        MutableLiveData<ServerNetsock>()
    }

}