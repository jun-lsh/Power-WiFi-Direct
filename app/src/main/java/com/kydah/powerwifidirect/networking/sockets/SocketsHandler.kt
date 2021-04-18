package com.kydah.powerwifidirect.networking.sockets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kydah.powerwifidirect.activity.MainActivity.Companion.GET_OBJ
import com.kydah.powerwifidirect.activity.MainActivity.Companion.HELLO
import com.kydah.powerwifidirect.activity.MainActivity.Companion.MESSAGE_READ
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.Peer
import com.kydah.powerwifidirect.networking.model.PeerFile

class SocketsHandler(private val networkViewModel: NetworkViewModel, private val context : Context) : Handler(Looper.getMainLooper()){

    private lateinit var socketManager: SocketManager
    private var localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var receivingFile = false

    @SuppressLint("HardwareIds")
    override fun handleMessage(msg: Message) {
        when(msg.what){

            GET_OBJ -> {
                socketManager = msg.obj as SocketManager
            }

            HELLO -> {
                val helloBuffer = socketManager.side + " " + networkViewModel.deviceId.value
                socketManager.write(helloBuffer.toByteArray())
                localBroadcastManager.sendBroadcast(Intent("SOCK_MAN_OPEN"))
            }

            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray
                val readMessage = String(readBuf, 0, msg.arg1)
                println("Got message: $readMessage")
                if(!receivingFile){

                    val tokens = readMessage.split(" ")

                    when(socketManager.side){
                        "svr" -> {
                            if(tokens[0] != "clt") return
                            when(tokens[1]){
                                "req" -> {
                                    when(tokens[2]){
                                        "fl" -> {
                                            val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                                            for(file in fileList){
                                                println(file.name)
                                                fileRes(file.name,
                                                    networkViewModel.deviceId.value!!
                                                )
                                            }
                                        }

                                        "pl" -> {
                                            for(peer in networkViewModel.peerList.value!!){
                                                peerRes(peer)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "clt" -> {
                            if(tokens[0] != "svr") return
                            when(tokens[1]){
                                "res" -> {
                                    when(tokens[2]){
                                        "f" -> {
                                            val peerFile = PeerFile(tokens[4], tokens[3])
                                            networkViewModel.fileList.value!!.add(peerFile)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun peerlistReq(){
        socketManager.write(("clt req pl\n").toByteArray())
    }

    fun filelistReq(hops : Int){
        socketManager.write(("clt req fl $hops\n").toByteArray())
    }

    fun fileReq(filename: String, deviceId : String){
        socketManager.write(("clt req f $filename $deviceId\n").toByteArray())
    }

    fun fileRes(filename: String, deviceId : String){
        socketManager.write(("svr res f $filename $deviceId\n").toByteArray())
    }

    fun peerRes(peer : Peer){
        socketManager.write(("svr res p $peer\n").toByteArray())
    }
}