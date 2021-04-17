package com.kydah.powerwifidirect.networking.wifidirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
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
    private val wifiManager : WifiManager = (this.context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
    private lateinit var clientNetsock: ClientNetsock

    fun establishConnection() {

        if (peer.accessPointData != null) {
      //      WifiUtils.withContext(context).scanWifi(this::getScanResults).start();
            println("Attempting to connect to DIRECT! " + peer.accessPointData!!.SSID + " " + peer.accessPointData!!.passphrase)
//            establishConnectionQ()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                establishConnectionQ()
            } else {
                println("using pre-q method...")

                val wifiConfiguration = WifiConfiguration()
                wifiConfiguration.SSID = String.format("\"%s\"", peer.accessPointData!!.SSID)
                wifiConfiguration.preSharedKey = String.format("\"%s\"", peer.accessPointData!!.passphrase)

                wifiManager.addNetwork(wifiConfiguration)

//                wifiManager.disconnect()
//                wifiManager.enableNetwork(wifiConfiguration.networkId, false)
//                wifiManager.reconnect()
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                for(wifiConf in wifiManager.configuredNetworks){
                    if(wifiConf.SSID != null && wifiConf.SSID ==  String.format("\"%s\"", peer.accessPointData!!.SSID))
                    {

                        wifiManager.disconnect()
                        wifiManager.enableNetwork(wifiConf.networkId, false)
                        wifiManager.reconnect()
                        val intentFilter = IntentFilter()
                        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                        context.registerReceiver(WifiStateReceiver(), intentFilter)
                    }
                }
            }
        }
    }

    private fun establishConnectionQ(){

        WifiUtils.withContext(context)
                .connectWith(peer.accessPointData!!.SSID, peer.accessPointData!!.passphrase)
                .setTimeout(60000)
                .onConnectionResult(object : ConnectionSuccessListener {
                    override fun success() {
                        println("Connection successfully established with " + peer.accessPointData!!.SSID)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("CHANGE_TO_CLIENT"))
                        createClientSocket()
                    }

                    override fun failed(errorCode: ConnectionErrorCode) {
                        println("Failed to connect to " + peer.accessPointData!!.SSID + " " + errorCode)
                        peer.accessPointData = null
                        networkViewModel.peerList.value!!.add(peer)
                    }

                }).start()
    }

    private fun getScanResults(results : List<ScanResult>) {
        if(results.isNotEmpty()) println(results)
        else println("no network results!")
    }

    private fun createClientSocket(){
        clientNetsock = ClientNetsock(peer.portNumber.toInt(), peer.accessPointData!!.inetAddress, handler)
        clientNetsock.start()
    }

    inner class WifiStateReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent!!.action == ConnectivityManager.CONNECTIVITY_ACTION){
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = cm.activeNetworkInfo
                if(networkInfo!!.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected){
                    println("Successfully connected to " + wifiManager.connectionInfo.ssid)
                    createClientSocket()
                }
            } else if (intent.action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION, ignoreCase = true)){
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                if(wifiState == WifiManager.WIFI_STATE_DISABLED){
                    println("WiFi disconnected!!")
                }
            }
        }
    }

}