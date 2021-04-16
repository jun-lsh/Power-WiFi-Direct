package com.kydah.powerwifidirect


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.AccessPointData
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.AccessPointConnection


class MainActivity : AppCompatActivity() {

    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var socketHandler: SocketsHandler

    private val networkViewModel : NetworkViewModel by viewModels()

    private lateinit var application : MainApplication

    companion object {
        const val MESSAGE_READ = 0x400 + 1
        const val MY_HANDLE = 0x400 + 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_prefs))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        application = applicationContext as MainApplication

        socketHandler = SocketsHandler(networkViewModel)

        networkViewModel.accessPoint.value = application.accessPoint

        networkViewModel.peerList.value = HashSet()
        networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler)
        networkViewModel.serverNetsock.value!!.startServer()

        intentFilter = IntentFilter()
        intentFilter.addAction("SERVICE_SEARCH_PEER_INFO")
        intentFilter.addAction("CHANGE_TO_CLIENT")

        broadcastReceiver = MainBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

    }

    inner class MainBroadcastReceiver(private var activity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "SERVICE_SEARCH_PEER_INFO" -> {
                    val peer = Peer(
                        intent.getStringExtra("DEVICE_ID")!!,
                        intent.getStringExtra("PORT_NUMBER")!!
                    )
                    val tokens = intent.getStringExtra("INSTANCE_NAME")?.split("/-/")
                    val accessPointData = AccessPointData(
                        tokens!![0],
                        tokens[1],
                        intent.getStringExtra("INET_ADDRESS")!!
                    )
                    peer.accessPointData = accessPointData
                    networkViewModel.peerList.value!!.add(peer)
                }

                "CHANGE_TO_CLIENT" -> {
                    networkViewModel.accessPoint.value!!.terminateAP()
                    networkViewModel.serverNetsock.value!!.stopServer()
                    for(peer in networkViewModel.peerList.value!!){
                        AccessPointConnection(peer, applicationContext, activity, socketHandler).establishConnection()
                    }
                }

            }
        }
    }

}