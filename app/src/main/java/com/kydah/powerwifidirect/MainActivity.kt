package com.kydah.powerwifidirect


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.AccessPointData
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.AccessPointConnection
import com.kydah.powerwifidirect.ui.firstlaunch.FirstLaunchFragment
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var socketHandler: SocketsHandler

    private val networkViewModel : NetworkViewModel by viewModels()

    private lateinit var application : MainApplication

    private lateinit var socketAction : String
    private lateinit var socketsHandler: SocketsHandler

    companion object {
        const val MESSAGE_READ = 0x400 + 1
        const val MY_HANDLE = 0x400 + 2
        const val HELLO = 0x400 + 3
        const val GET_OBJ = 0x400 + 4
    }

    @SuppressLint("HardwareIds")
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



        socketHandler = SocketsHandler(networkViewModel, applicationContext)

        networkViewModel.deviceId.value = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID)

        networkViewModel.accessPoint.value = application.accessPoint

        networkViewModel.fileList.value = ArrayList()
        networkViewModel.peerList.value = HashSet()
        networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler)
        networkViewModel.serverNetsock.value!!.startServer()

        intentFilter = IntentFilter()
        intentFilter.addAction("SERVICE_SEARCH_PEER_INFO")
        intentFilter.addAction("CHANGE_TO_CLIENT")
        intentFilter.addAction("CHANGE_TO_SERVER")

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if(!sharedPrefs.getBoolean("set_folders", false)){
            println("first launch!")
            supportFragmentManager.let{ it1 ->
                FirstLaunchFragment().show(it1, "as_pop_up")
            }
        } else {
            networkViewModel.downloadsFolder.value = File(sharedPrefs.getString("downloads_folder", ""))
            networkViewModel.uploadsFolder.value = File(sharedPrefs.getString("uploads_folder", ""))
        }

        broadcastReceiver = MainBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

    }

    inner class MainBroadcastReceiver(private var activity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "SERVICE_SEARCH_PEER_INFO" -> {

                    val tokens = intent.getStringExtra("INSTANCE_NAME")?.split("/-/")
                    val peer = Peer(
                        tokens!![3],
                        tokens[2]
                    )
                    val accessPointData = AccessPointData(
                    //    intent.getStringExtra("INSTANCE_NAME")!!,
                        tokens[0],
                        tokens[1],
                        tokens[4]
                    )
                    peer.accessPointData = accessPointData
                    networkViewModel.peerList.value!!.add(peer)
                }

                "CLIENT_ACTION" -> {
                    var socketAction = intent.getStringExtra("ACTION_TYPE")
                }

                "CHANGE_TO_CLIENT" -> {
                    networkViewModel.accessPoint.value!!.terminateAP()
                    networkViewModel.serverNetsock.value!!.stopServer()
                    for(peer in networkViewModel.peerList.value!!){
                        AccessPointConnection(peer, applicationContext, activity, socketHandler).establishConnection()
                    }
                }

                "CHANGE_TO_SERVER" -> {
                    networkViewModel.accessPoint.value!!.startAP()
                    networkViewModel.serverNetsock.value!!.startServer()
                }

                "SOCK_MAN_OPEN" -> {
                    when(socketAction){
                        "FILE_REQ" -> {
                            socketsHandler.peerlistReq()
                            socketsHandler.filelistReq(2)
                        }
                    }
                }

            }
        }
    }
}