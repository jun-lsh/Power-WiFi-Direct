package com.kydah.powerwifidirect.activity

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.kydah.powerwifidirect.MainApplication
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import com.kydah.powerwifidirect.ui.firstlaunch.FirstLaunchFragment
import java.io.File
import kotlin.properties.Delegates


class SplashscreenActivity : AppCompatActivity(), RequiresPermissions, DialogInterface.OnDismissListener {

    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_READ_STORAGE = 2
    private val PERMISSION_REQUEST_WRITE_STORAGE = 3

    private lateinit var loadingText : TextView
    private lateinit var accessPoint: SoftAccessPoint
    //private lateinit var serverNetsock: ServerNetsock

    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: BroadcastReceiver

    private var portNumber by Delegates.notNull<Int>()

    private lateinit var application : MainApplication


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_splashscreen)

        loadingText = findViewById(R.id.loadingTextView)

        application = applicationContext as MainApplication

        intentFilter = IntentFilter()

        intentFilter.addAction("GROUP_CREATION_SUCCESSFUL")
        intentFilter.addAction("GROUP_CREATION_STALLED")
        intentFilter.addAction("GROUP_CREATION_UNSUCCESSFUL")
        intentFilter.addAction("SERVICE_CREATION_SUCCESSFUL")
        intentFilter.addAction("SERVICE_CREATION_UNSUCCESSFUL")
        intentFilter.addAction("SERVICE_DISCOVERY_CLEARED_SUCCESSFULLY")
        intentFilter.addAction("SERVICE_REQUEST_ADDED_SUCCESSFULLY")
        intentFilter.addAction("PEER_DISCOVERY_ADDED_SUCCESSFULLY")
        intentFilter.addAction("SERVICE_DISCOVERY_ADDED_SUCCESSFULLY")
        intentFilter.addAction("SERVICE_DISCOVERY_ADDED_UNSUCCESSFULLY")
        intentFilter.addAction("PEER_DISCOVERY_ADDED_UNSUCCESSFULLY")
        intentFilter.addAction("SERVICE_REQUEST_ADDED_UNSUCCESSFULLY")
        intentFilter.addAction("SERVICE_DISCOVERY_CLEARED_UNSUCCESSFULLY")
        intentFilter.addAction("INIT_AS_LC")
        intentFilter.addAction("INIT_AS_GO")

        broadcastReceiver = InitializationBroadcastReceiver()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            broadcastReceiver,
            intentFilter
        )
        updateText("Starting up...")
        checkPerms()

    }

    private fun startInit(){
        portNumber = ((0..64329).random() + 1023)
        application.portNumber = portNumber
        accessPoint = SoftAccessPoint(applicationContext, portNumber)
        application.accessPoint = accessPoint
        accessPoint.startAP()
    }

    private fun startMainActivity(legacyPeer: LegacyPeer?){
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
        Handler(mainLooper).postDelayed({
            val startIntent: Intent = Intent(this, MainActivity::class.java)
            if(legacyPeer != null) startIntent.putExtra("groupOwner", legacyPeer)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(startIntent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 2000)

    }

    inner class InitializationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "GROUP_CREATION_SUCCESSFUL" -> {
                    updateText("Group successfully created...")
                }
                "GROUP_CREATION_STALLED" -> {
                    updateText("Group creation failed! Please turn your WiFi off and on again and restart the application.")
                }
                "GROUP_CREATION_UNSUCCESSFUL" -> {
                    updateText("Group creation failed... retrying...")
                }
                "SERVICE_CREATION_SUCCESSFUL" -> {
                    updateText("Service successfully created...")
                }
                "SERVICE_CREATION_UNSUCCESSFUL" -> {
                    updateText("Service creation failed! AMOGUS")
                }
                "SERVICE_DISCOVERY_CLEARED_SUCCESSFULLY" -> {
                    updateText("Service discovery flushed...")
                }
                "SERVICE_REQUEST_ADDED_SUCCESSFULLY" -> {
                    updateText("Service request added...")
                }
                "PEER_DISCOVERY_ADDED_SUCCESSFULLY" -> {
                    updateText("Peer discovery started...")
                }
                "SERVICE_DISCOVERY_ADDED_SUCCESSFULLY" -> {
                    updateText("Service discovery started...")
                }
                "SERVICE_DISCOVERY_ADDED_UNSUCCESSFULLY" -> {
                    updateText("Service discovery failed! AMOGUS")
                }
                "PEER_DISCOVERY_ADDED_UNSUCCESSFULLY" -> {
                    updateText("Peer discovery failed! SUS")
                }
                "SERVICE_REQUEST_ADDED_UNSUCCESSFULLY" -> {
                    updateText("Service requesting failed! SUS")
                }
                "SERVICE_DISCOVERY_CLEARED_UNSUCCESSFULLY" -> {
                    updateText("Service discovery could not be flushed! AMOGUS")
                }
                "INIT_AS_LC" -> {
                    println("Successfully connected to GO...")
                    updateText("Formed a connection to a GO!")
                    startMainActivity(intent.getParcelableExtra("groupOwner"))
                }

                "INIT_AS_GO" -> {
                    updateText("Now a GO!")
                    startMainActivity(null)
                }
            }
        }
    }

    private fun updateText(text: String){
        loadingText.text = text
    }

    private fun isWifiDirectSupported(): Boolean {
        val pm = this.packageManager
        val features = pm.systemAvailableFeatures
        for (info in features) {
            if (info?.name != null && info.name.equals(
                    "android.hardware.wifi.direct",
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    override fun checkPerms(){

        if(!isWifiDirectSupported()){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("WiFi Direct is not supported on this device!")
            builder.setMessage("Your phone does not support WiFi Direct, which this app relies on.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener { this.finishAffinity() }
            builder.show()
        }


        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSION_REQUEST_FINE_LOCATION
                    )
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Location access is required")
                builder.setMessage("Since location access has not been granted, this app will not be able to function at all. Please go to Settings -> Applications -> Permissions and grant location access to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { this.finishAffinity() }
                builder.show()
            }
        } else {
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

            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            if(!sharedPrefs.getBoolean("set_folders", false)){
                supportFragmentManager.let{ it1 ->
                    FirstLaunchFragment().show(it1, "as_pop_up")
                }
            } else {
                application.downloadsFolder = File(sharedPrefs.getString("downloads_folder", ""))
                application.uploadsFolder = File(sharedPrefs.getString("uploads_folder", ""))
                startInit()
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
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("permission granted. pawgers...")
                    startInit()
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Location access is required")
                    builder.setMessage("Since location access has not been granted, this app will not be able to function at all. Don't worry about tracking, your internet need not be connected.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { this.finishAffinity() }
                    builder.show()
                }
            }
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

    override fun onDismiss(dialog: DialogInterface?) {
        startInit()
    }

}

