package com.kydah.powerwifidirect.networking.wifidirect

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.AccessPointData
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.sockets.ClientNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener

class AccessPointConnection(private var peer: Peer, private var context : Context, private val vmStoreOwner: ViewModelStoreOwner, private val handler : SocketsHandler) {

    private val networkViewModel : NetworkViewModel = ViewModelProvider(vmStoreOwner).get(NetworkViewModel::class.java)
    private lateinit var clientNetsock: ClientNetsock

    fun establishConnection() {
        if (peer.accessPointData != null) {
            println("Attempting to connect to DIRECT!")
            WifiUtils.withContext(context)
                    .connectWith(peer.accessPointData!!.SSID, peer.accessPointData!!.passphrase)
                    .setTimeout(12000)
                    .onConnectionResult(object : ConnectionSuccessListener {
                        override fun success() {
                            println("Connection successfully established with " + peer.accessPointData!!.SSID)
                            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("CHANGE_TO_CLIENT"))
                        }

                        override fun failed(errorCode: ConnectionErrorCode) {
                            println("Failed to connect to " + peer.accessPointData!!.SSID + " " + errorCode)
                            peer.accessPointData = null
                            networkViewModel.peerList.value!!.add(peer)
                        }

                    })

        }
    }

    private fun createClientSocket(){
        clientNetsock = ClientNetsock(peer.portNumber.toInt(), peer.accessPointData!!.inetAddress, handler)
        clientNetsock.start()
    }

}