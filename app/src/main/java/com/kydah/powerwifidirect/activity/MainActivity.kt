package com.kydah.powerwifidirect.activity


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kydah.powerwifidirect.MainApplication
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.sockets.SocketsHandler
import com.kydah.powerwifidirect.networking.wifidirect.AccessPointConnection
import com.kydah.powerwifidirect.utils.NotificationUtils


class MainActivity : AppCompatActivity(), RequiresPermissions, MainServiceCallbacks {

    private val PERMISSION_REQUEST_READ_STORAGE = 2
    private val PERMISSION_REQUEST_WRITE_STORAGE = 3

    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var socketHandler: SocketsHandler

    private lateinit var networkViewModel : NetworkViewModel

    private lateinit var application : MainApplication

    private lateinit var socketAction : String

    //private lateinit var snackbar: Snackbar
    private lateinit var mainService : MainService

    private var legacyGroupOwner : LegacyPeer? = null

    companion object {
        const val MESSAGE_READ = 0x400 + 1
        //const val MY_HANDLE = 0x400 + 2
        const val HELLO = 0x400 + 2
        const val GET_OBJ = 0x400 + 3
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
        networkViewModel.legacyPeerList.value = HashSet()


        intentFilter = IntentFilter()
        intentFilter.addAction("SERVICE_SEARCH_PEER_INFO")
        intentFilter.addAction("CLIENT_ACTION")
        intentFilter.addAction("SOCK_MAN_OPEN")
        intentFilter.addAction("INIT_AS_LC_MA")
        intentFilter.addAction("SOCKET_ACTION")
        networkViewModel.downloadsFolder.value = application.downloadsFolder
        networkViewModel.uploadsFolder.value = application.uploadsFolder

        broadcastReceiver = MainBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

        legacyGroupOwner = intent.getParcelableExtra<LegacyPeer>("groupOwner")
        if(legacyGroupOwner != null){
            networkViewModel.transmissionMode.value = "Legacy Client (LC)"
            socketHandler = SocketsHandler(networkViewModel, applicationContext, "clt")
            networkViewModel.connectingToAP.value = true
            AccessPointConnection(legacyGroupOwner!!, applicationContext, socketHandler, networkViewModel).establishConnection()
        } else {
            socketHandler = SocketsHandler(networkViewModel, applicationContext, "svr")
            networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler, applicationContext)
            networkViewModel.serverNetsock.value!!.startServer()
        }

        Intent(this, MainService::class.java)
        val serviceConnection = object:ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MainService.LocalBinder
                mainService = binder.service
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }

        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        NotificationUtils.setNetworkViewModel(networkViewModel)
        NotificationUtils.createNotificationManager(this)
        NotificationUtils.createNotificationChannel(channelId, "Power: WiFi Direct Notifications",
                "Notifications for Power", NotificationManager.IMPORTANCE_HIGH)
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

    override fun onDestroy() {
        super.onDestroy()
        networkViewModel.accessPoint.value!!.terminateAP()
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

    override fun onSupportNavigateUp(): Boolean {
        // setupActionBarWithNavController breaks the back button for fragments that you transition
        // to using actions manually. this fixes it by doing it manually.
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    inner class MainBroadcastReceiver(private var activity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "SERVICE_SEARCH_PEER_INFO" -> {
                    val peer = intent.getParcelableExtra<LegacyPeer>("PEER_OBJ")!!
                    networkViewModel.legacyPeerList.value!!.remove(peer)
                    networkViewModel.legacyPeerList.value!!.add(peer)
                    networkViewModel.legacyPeerList.value = networkViewModel.legacyPeerList.value!!
                }

                "SOCKET_ACTION" -> {
                    when(intent.getStringExtra("ACTION_TYPE")){
                        "SPECIFIC_FILE_REQ" -> {
                            socketHandler.fileReq(intent.getStringExtra("FILENAME")!!, intent.getStringExtra("PEER_ID")!!)
                        }
                    }
                }

                "SOCK_MAN_OPEN" -> {
                    if(legacyGroupOwner == null){

                    NotificationUtils.pushNotification(1, "New Legacy Client Joined!", "Current number of clients: " + socketHandler.sockets.size,
                            applicationContext, R.drawable.ic_baseline_group_24, activity, false, true)}
                    socketHandler.filelistReq(2)
                }

                "INIT_AS_LC_MA" -> {
                    networkViewModel.transmissionMode.value = "Legacy Client (LC)"
//                    networkViewModel.serverNetsock.value!!.stopServer()
                    socketHandler = SocketsHandler(networkViewModel, applicationContext, "clt")
                    legacyGroupOwner = intent.getParcelableExtra("groupOwner")
                    AccessPointConnection(legacyGroupOwner!!, applicationContext, socketHandler, networkViewModel).establishConnection()
                    networkViewModel.connectingToAP.value = true
                }

                "INIT_AS_GO_MA" -> {
                    socketHandler = SocketsHandler(networkViewModel, applicationContext, "svr")
                    networkViewModel.serverNetsock.value = ServerNetsock(application.portNumber, socketHandler, applicationContext)
                    networkViewModel.serverNetsock.value!!.startServer()
                }

                "GO_DISCONNECT" -> {
                    networkViewModel.transmissionMode.value = "Group Owner (GO)"
                    networkViewModel.accessPoint.value!!.startAP()
                }

            }
        }
    }

    override fun clearNetworking() {
        if(legacyGroupOwner == null){
            socketHandler.serverCloseConnection()
            networkViewModel.serverNetsock.value!!.stopServer()
        } else {
            socketHandler.clientCloseConnection()
        }
    }
}