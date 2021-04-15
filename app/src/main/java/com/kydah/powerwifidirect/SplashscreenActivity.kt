package com.kydah.powerwifidirect

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kydah.powerwifidirect.networking.sockets.ServerNetsock
import com.kydah.powerwifidirect.networking.wifidirect.SoftAccessPoint
import kotlin.properties.Delegates

class SplashscreenActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_FINE_LOCATION = 1

    private lateinit var loadingText : TextView
    private lateinit var accessPoint: SoftAccessPoint
    private lateinit var serverNetsock: ServerNetsock

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

        broadcastReceiver = InitializationBroadcastReceiver()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver, intentFilter)

        checkPerms()

        updateText("Starting up...")

        portNumber = ((0..64329).random() + 1023)
        application.portNumber = portNumber

        accessPoint = SoftAccessPoint(applicationContext, portNumber)

        application.accessPoint = accessPoint
        accessPoint.startAP()
    }

    fun startMainActivity(){
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
        Handler(mainLooper).postDelayed({
            val startIntent : Intent = Intent(this, MainActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(startIntent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 2000)

    }

    inner class InitializationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "GROUP_CREATION_SUCCESSFUL" -> { updateText("Group successfully created...")}
                "GROUP_CREATION_STALLED" -> { updateText("Group creation failed! Please turn your WiFi off and on again and restart the application.")}
                "GROUP_CREATION_UNSUCCESSFUL" -> { updateText("Group creation failed... retrying...")}
                "SERVICE_CREATION_SUCCESSFUL" -> { updateText("Service successfully created...")}
                "SERVICE_CREATION_UNSUCCESSFUL" -> { updateText("Service creation failed! AMOGUS")}
                "SERVICE_DISCOVERY_CLEARED_SUCCESSFULLY" -> { updateText("Service discovery flushed...")}
                "SERVICE_REQUEST_ADDED_SUCCESSFULLY" -> { updateText("Service request added...")}
                "PEER_DISCOVERY_ADDED_SUCCESSFULLY" -> { updateText("Peer discovery started...")}
                "SERVICE_DISCOVERY_ADDED_SUCCESSFULLY" -> {
                    updateText("Service discovery started...")
                    startMainActivity()
                }
                "SERVICE_DISCOVERY_ADDED_UNSUCCESSFULLY" -> { updateText("Service discovery failed! AMOGUS")}
                "PEER_DISCOVERY_ADDED_UNSUCCESSFULLY" -> { updateText("Peer discovery failed! SUS")}
                "SERVICE_REQUEST_ADDED_UNSUCCESSFULLY" -> { updateText("Service requesting failed! SUS")}
                "SERVICE_DISCOVERY_CLEARED_UNSUCCESSFULLY" -> { updateText("Service discovery could not be flushed! AMOGUS")}
            }
        }
    }

    private fun updateText(text : String){
        loadingText.text = text
    }

    private fun checkPerms(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_FINE_LOCATION)
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Location access is required")
                builder.setMessage("Since location access has not been granted, this app will not be able to function at all. Please go to Settings -> Applications -> Permissions and grant location access to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { }
                builder.show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("permission granted. pawgers...")
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Location access is required")
                    builder.setMessage("Since location access has not been granted, this app will not be able to function at all. Don't worry about tracking, your internet need not be connected.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
            }
        }
    }

}

