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
        // Presetting values remember to comment this out pls
        application.portNumber = 0


        socketHandler = SocketsHandler(networkViewModel)

        networkViewModel.peerList.value = HashSet()
        networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler)

        intentFilter = IntentFilter()
        intentFilter.addAction("SERVICE_SEARCH_PEER_INFO")

        broadcastReceiver = MainBroadcastReceiver()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

    }

    inner class MainBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "SERVICE_SEARCH_PEER_INFO" -> {
                    val peer = Peer(
                        intent.getStringExtra("INSTANCE_NAME")!!,
                        intent.getStringExtra("PORT_NUMBER")!!
                    )
                    val tokens = intent.getStringExtra("DEVICE_ID")?.split("/-/")
                    val accessPointData = AccessPointData(
                        tokens!![0],
                        tokens[1],
                        intent.getStringExtra("INET_ADDRESS")!!
                    )
                    peer.accessPointData = accessPointData
                    networkViewModel.peerList.value!!.add(peer)
                }
            }
        }
    }

}