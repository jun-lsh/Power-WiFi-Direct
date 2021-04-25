package com.kydah.powerwifidirect.activity


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kydah.powerwifidirect.MainApplication
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.AccessPointData
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.AccessPointConnection
import com.kydah.powerwifidirect.ui.firstlaunch.FirstLaunchFragment
import com.kydah.powerwifidirect.utils.NotificationUtils
import java.io.File


class MainActivity : AppCompatActivity(), RequiresPermissions {

    private val PERMISSION_REQUEST_READ_STORAGE = 2
    private val PERMISSION_REQUEST_WRITE_STORAGE = 3

    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var socketHandler: SocketsHandler

    private lateinit var networkViewModel : NetworkViewModel

    private lateinit var application : MainApplication

    private lateinit var socketAction : String

    private var legacyGroupOwner : LegacyPeer? = null

    companion object {
        const val MESSAGE_READ = 0x400 + 1
        const val MY_HANDLE = 0x400 + 2
        const val HELLO = 0x400 + 3
        const val GET_OBJ = 0x400 + 4
        const val channelId = "com.kydah.powerwifidirect"
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_home, R.id.navigation_search, R.id.navigation_prefs
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        application = applicationContext as MainApplication



        socketAction = ""


        networkViewModel.deviceId.value = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID)

        networkViewModel.accessPoint.value = application.accessPoint

        networkViewModel.fileList.value = ArrayList()

        for(file in networkViewModel.uploadsFolder.value!!.listFiles()){
            networkViewModel.fileList.value!!.add(PeerFile(networkViewModel.deviceId.value!!, file.name))
        }

        networkViewModel.legacyPeerList.value = HashSet()


        intentFilter = IntentFilter()
        intentFilter.addAction("SERVICE_SEARCH_PEER_INFO")
        intentFilter.addAction("CLIENT_ACTION")
        intentFilter.addAction("SOCK_MAN_OPEN")
        networkViewModel.downloadsFolder.value = application.downloadsFolder
        networkViewModel.uploadsFolder.value = application.uploadsFolder


        broadcastReceiver = MainBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

        legacyGroupOwner = intent.getParcelableExtra<LegacyPeer>("groupOwner")
        println(legacyGroupOwner != null)
        if(legacyGroupOwner != null){
            networkViewModel.transmissionMode.value = "Legacy Client (LC)"
            socketHandler = SocketsHandler(networkViewModel, applicationContext, "clt")
            AccessPointConnection(legacyGroupOwner!!, applicationContext, socketHandler).establishConnection()
        } else {
            socketHandler = SocketsHandler(networkViewModel, applicationContext, "svr")
            NotificationUtils.createNotificationManager(this)
            NotificationUtils.createNotificationChannel(channelId, "Power: WiFi Direct Notifications",
                    "Notifications for Power", NotificationManager.IMPORTANCE_HIGH)
            NotificationUtils.pushNotification(69, "Power Persistent Notification", "Current number of peers: 0",
                    applicationContext, R.drawable.ic_baseline_group_24, this, true, true)
            networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler)
            networkViewModel.serverNetsock.value!!.startServer()
        }

    }


    override fun checkPerms(){
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_WRITE_STORAGE
                )
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Storage access is required")
                builder.setMessage("Since storage access has not been granted, this app will not be able to function at all. Please go to Settings -> Applications -> Permissions and grant storage access to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { this.finishAffinity() }
                builder.show()
                return
            }
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_READ_STORAGE
                )
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Storage access is required")
                builder.setMessage("Since storage access has not been granted, this app will not be able to function at all. Please go to Settings -> Applications -> Permissions and grant storage access to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { this.finishAffinity() }
                builder.show()
                return
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_READ_STORAGE, PERMISSION_REQUEST_WRITE_STORAGE -> {
                if(grantResults.isEmpty()) return
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("permission granted. pawgers...")
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Storage access is required")
                    builder.setMessage("Since storage access has not been granted, this app will not be able to function at all.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { this.finishAffinity() }
                    builder.show()
                }
            }
        }
    }

    inner class MainBroadcastReceiver(private var activity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "SERVICE_SEARCH_PEER_INFO" -> {
                    val peer = intent.getParcelableExtra<LegacyPeer>("PEER_OBJ")!!
                    //val accessPointData = intent.getParcelableExtra<AccessPointData>("AP_OBJ")
                    //peer.accessPointData = accessPointData
                    if(!networkViewModel.legacyPeerList.value!!.contains(peer)){
                        NotificationUtils.pushNotification(70, "Power: New Peer Detected",
                                "New peer was detected: " +  peer.deviceID,
                                applicationContext, R.drawable.ic_baseline_group_24, activity, false, false)
                    }
                    networkViewModel.legacyPeerList.value!!.remove(peer)
                    networkViewModel.legacyPeerList.value!!.add(peer)
                    networkViewModel.legacyPeerList.value = networkViewModel.legacyPeerList.value!!
                    NotificationUtils.pushNotification(69, "Power Persistent Notification",
                            "Current number of peers: " +  networkViewModel.legacyPeerList.value!!.size,
                            applicationContext, R.drawable.ic_baseline_group_24, activity, true, true)
                }

                "SOCKET_ACTION" -> {
                    when(intent.getStringExtra("ACTION_TYPE")){
                        "SPECIFIC_FILE_REQ" -> {
                            socketHandler.fileReq(intent.getStringExtra("FILENAME")!!, intent.getStringExtra("PEER_ID")!!)
                        }
                    }
                }

                "SOCK_MAN_OPEN" -> {
//                    networkViewModel.accessPoint.value!!.createGroup()
                    socketHandler.peerlistReq()
                    socketHandler.filelistReq(2)
                }

            }
        }
    }
}