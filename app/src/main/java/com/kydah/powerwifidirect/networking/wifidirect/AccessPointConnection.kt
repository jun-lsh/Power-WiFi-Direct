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
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.sockets.ClientNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class AccessPointConnection(private var legacyPeer: LegacyPeer, private var context: Context, private val handler: SocketsHandler, private val networkViewModel: NetworkViewModel) {

    private val wifiManager : WifiManager = (this.context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
    private lateinit var clientNetsock: ClientNetsock

    fun establishConnection() {
        if (legacyPeer.accessPointData != null) {
      //      WifiUtils.withContext(context).scanWifi(this::getScanResults).start();
            println("Attempting to connect to DIRECT! " + legacyPeer.accessPointData!!.SSID + " " + legacyPeer.accessPointData!!.passphrase)
//            establishConnectionQ()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                establishConnectionQ()
            } else {
                println("using pre-q method...")

                val wifiConfiguration = WifiConfiguration()
                wifiConfiguration.SSID = String.format("\"%s\"", legacyPeer.accessPointData!!.SSID)
                wifiConfiguration.preSharedKey = String.format("\"%s\"", legacyPeer.accessPointData!!.passphrase)

                wifiManager.addNetwork(wifiConfiguration)
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                for(wifiConf in wifiManager.configuredNetworks){
                    if(wifiConf.SSID != null && wifiConf.SSID ==  String.format("\"%s\"", legacyPeer.accessPointData!!.SSID))
                    {
                        //wifiManager.disableNetwork(wifiConf.networkId)
                        wifiManager.disconnect()
                        wifiManager.enableNetwork(wifiConf.networkId, true)
                        println(wifiManager.reconnect())
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
                .connectWith(legacyPeer.accessPointData!!.SSID, legacyPeer.accessPointData!!.passphrase)
                .setTimeout(25000)
                .onConnectionResult(object : ConnectionSuccessListener {
                    override fun success() {
                        println("Connection successfully established with " + legacyPeer.accessPointData!!.SSID)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("CHANGE_TO_CLIENT"))
                        networkViewModel.connectingToAP.value = false
                        createClientSocket()
                    }

                    override fun failed(errorCode: ConnectionErrorCode) {
                        println("Failed to connect to " + legacyPeer.accessPointData!!.SSID + " " + errorCode)
                        legacyPeer.accessPointData = null
                        //networkViewModel.legacyPeerList.value!!.add(legacyPeer)
                    }

                }).start()
    }

    private fun getScanResults(results: List<ScanResult>) {
        if(results.isNotEmpty()) println(results)
        else println("no network results!")
    }

    private fun createClientSocket(){
        getIpAddress()
        clientNetsock = ClientNetsock(legacyPeer.portNumber!!.toInt(), legacyPeer.accessPointData!!.inetAddress, handler, context)
        clientNetsock.start()
    }

    inner class WifiStateReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent!!.action == ConnectivityManager.CONNECTIVITY_ACTION){
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = cm.activeNetworkInfo
                if(networkInfo != null){
                if(networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected){
                    println("Successfully connected to " + wifiManager.connectionInfo.ssid)
                    networkViewModel.connectingToAP.value = false
                    createClientSocket()
                }}
            } else if (intent.action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION, ignoreCase = true)){
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                if(wifiState == WifiManager.WIFI_STATE_DISABLED){
                    println("WiFi disconnected!!")
                }
            }
        }
    }


    fun getIpAddress() {
        try {
            val interfaces: List<NetworkInterface> = Collections
                    .list(NetworkInterface.getNetworkInterfaces())
            /*
         * for (NetworkInterface networkInterface : interfaces) { Log.v(TAG,
         * "interface name " + networkInterface.getName() + "mac = " +
         * getMACAddress(networkInterface.getName())); }
         */for (intf in interfaces) {

                //if (!intf.getName().contains("p2p")) continue
                val addrs: List<InetAddress> = Collections.list(intf
                        .getInetAddresses())
                for (addr in addrs) {
                    // Log.v(TAG, "inside");
                    if (!addr.isLoopbackAddress()) {
                        // Log.v(TAG, "isnt loopback");
                        val sAddr: String = addr.getHostAddress().toUpperCase()
                        //Log.v(TAG, "ip=$sAddr")
                        val isIPv4: Boolean = validateIPv4(sAddr)
                        if (isIPv4) {
                            if (sAddr.contains("192.168.49.")) {
                                //Log.v(TAG, "ip = $sAddr")
                                //return sAddr
                                println(sAddr)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            //Log.v(TAG, "error in parsing")
        } // for now eat exceptions
    }

    fun getMACAddress(interfaceName: String?): String? {
        try {
            val interfaces: List<NetworkInterface> = Collections
                    .list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac = intf.hardwareAddress ?: return ""
                val buf = StringBuilder()
                for (idx in mac.indices) buf.append(String.format("%02X:", mac[idx]))
                if (buf.length > 0) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: java.lang.Exception) {
        } // for now eat exceptions
        return ""
        /*
         * try { // this is so Linux hack return
         * loadFileAsString("/sys/class/net/" +interfaceName +
         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
         * null; }
         */
    }

    fun validateIPv4(ip: String): Boolean {
        val PATTERN = Regex("^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$")
        return ip.matches(PATTERN)
    }

}