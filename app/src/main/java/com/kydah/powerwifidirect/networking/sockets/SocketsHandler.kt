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
import com.kydah.powerwifidirect.networking.model.LegacyPeer
import com.kydah.powerwifidirect.networking.model.PeerFile
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class SocketsHandler(private val networkViewModel: NetworkViewModel, private val context: Context, private val side: String) : Handler(Looper.getMainLooper()){

    private lateinit var socketManager: SocketManager
    private var localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var receivingFile = false

    private var sockets = HashMap<String, SocketManager>()

    private var receivingFileName = ""
    private var targetSocket : String = ""

    private lateinit var receivedFile : FileOutputStream
    private var receivedFileChunks by Delegates.notNull<Int>()

    private var myDeviceId = networkViewModel.deviceId.value!!

    @SuppressLint("HardwareIds")
    override fun handleMessage(msg: Message) {
        when(msg.what){

            GET_OBJ -> {
                socketManager = msg.obj as SocketManager
            }

            HELLO -> {
                println("Sending HELLO!")
                val helloBuffer = socketManager.side + " dvn " + networkViewModel.deviceId.value
                socketManager.write(helloBuffer.toByteArray(StandardCharsets.UTF_8))
            }

            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray

                if (!receivingFile) {
                    println(readBuf.size)
                    val readMessage = String(readBuf, StandardCharsets.UTF_8)
                    println("Got message: $readMessage")
                    val tokens = readMessage.split(" ")

                    when (socketManager.side) {
                        "svr" -> {
                            serverParser(tokens)
                        }

                        "clt" -> {
                            clientParser(tokens)
                        }
                    }
                } else {
                    if (receivedFileChunks == 0) {
                        receivedFile.close()
                        receivingFile = false
                    } else {
                        println(receivedFileChunks)
                        receivedFile.write(readBuf, 0, readBuf.size)
                        receivedFileChunks--
                        if (receivedFileChunks == 0) {
                            receivedFile.close()
                            receivingFile = false
                            if(targetSocket != ""){
                                val file = File(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp/" + receivingFileName)
                                sockets[targetSocket]!!.write(("svr res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt()).toByteArray(StandardCharsets.UTF_8))
                                sockets[targetSocket]!!.readFile(file)
                                targetSocket = ""
                            }
                        }
                    }
                }
            }
        }
    }

    private fun serverParser(tokens: List<String>){
        if(tokens[0] != "clt") return
        when(tokens[1]){

            "dvn" -> {
                sockets[tokens[2]] = socketManager
                localBroadcastManager.sendBroadcast(Intent("SOCK_MAN_OPEN"))
            }

            "req" -> {
                when (tokens[2]) {
                    "fl" -> {
                        val fileList = networkViewModel.fileList.value!!
                        for(file in networkViewModel.uploadsFolder.value!!.listFiles()){
                            fileRes(file.name!!, networkViewModel.deviceId.value!!, tokens[3])
                        }
                        for (file in fileList) {
                            fileRes(file, tokens[3])
                        }
                    }

                    "pl" -> {
                        for (peer in networkViewModel.legacyPeerList.value!!) {
                            peerRes(peer)
                        }
                    }

                    "f" -> {
                        if (tokens[4] == networkViewModel.deviceId.value!!) {
                            val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                            for (file in fileList) {
                                if (file.name == tokens[3]) {
                                    sockets[tokens[5]]!!.write(("svr res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt()).toByteArray(StandardCharsets.UTF_8))
                                    sockets[tokens[5]]!!.readFile(file)
                                }
                            }
                        } else {
                            sockets[tokens[4]]!!.write(("svr req tmpf " + tokens[3] + " " + tokens[5]).toByteArray(StandardCharsets.UTF_8))
                        }
                    }
                }
            }
            "res" -> {
                when (tokens[2]) {
                    "f" -> {
                        if(tokens[4] != networkViewModel.deviceId.value!!){
                        println("received " + tokens[3])
                        val peerFile = PeerFile(tokens[4], tokens[3])
                        networkViewModel.fileList.value!!.add(peerFile)
                        networkViewModel.fileList.value = networkViewModel.fileList.value!!}
                        //println(networkViewModel.fileList.value!!.size)
                        //networkViewModel.fileList.postValue(networkViewModel.fileList.value!!)
                    }

                    "wf" -> {
                        receivingFile = true
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                    }

                    "wtmpf" -> {
                        receivingFile = true
                        receivingFileName = tokens[3]
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                        targetSocket = tokens[5]
                    }
                }
            }
        }
    }

    private fun clientParser(tokens: List<String>){
        if(tokens[0] != "svr") return
        when(tokens[1]){
            "dvn" -> {
                sockets[tokens[2]] = socketManager
                localBroadcastManager.sendBroadcast(Intent("SOCK_MAN_OPEN"))
            }

            "req" -> {
                when (tokens[2]) {
                    "fl" -> {
                        val fileList = networkViewModel.fileList.value!!
                        for(file in networkViewModel.uploadsFolder.value!!.listFiles()){
                            fileRes(file.name!!, networkViewModel.deviceId.value!!)
                        }
                        for (file in fileList) {
                            fileRes(file)
                        }
                    }

                    "pl" -> {
                        for (peer in networkViewModel.legacyPeerList.value!!) {
                            peerRes(peer)
                        }
                    }

                    "f" -> {
                        val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                        for (file in fileList) {
                            if (file.name == tokens[3]) {
                                socketManager.write(("clt res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt()+ " " + tokens[4]).toByteArray(StandardCharsets.UTF_8))
                                        socketManager.readFile(file)
                            }
                        }
                    }

                    "tmpf" -> {
                        val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                        for (file in fileList) {
                            if (file.name == tokens[3]) {
                                socketManager.write(("clt res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt()+ " " + tokens[4]).toByteArray(StandardCharsets.UTF_8))
                                socketManager.readFile(file)
                            }
                        }
                    }
                }
            }
            "res" -> {
                when (tokens[2]) {
                    "f" -> {
                        if(tokens[4] != networkViewModel.deviceId.value!!){
                            println("received " + tokens[3])
                            val peerFile = PeerFile(tokens[4], tokens[3])
                            networkViewModel.fileList.value!!.add(peerFile)
                            networkViewModel.fileList.value = networkViewModel.fileList.value!!}
                    }

                    "wf" -> {
                        receivingFile = true
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                    }
                }
            }
        }
    }


    private fun charsToBytes(chars: CharArray?): ByteArray? {
        val byteBuffer: ByteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars))
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit())
    }

    fun peerlistReq(){
        if(!receivingFile) socketManager.write(("$side req pl $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun filelistReq(hops: Int, targetId: String){
        if(!receivingFile) sockets[targetId]!!.write(("$side req fl $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }


    fun filelistReq(hops: Int){
        if(!receivingFile) socketManager.write(("$side req fl $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun fileReq(filename: String, deviceId: String){
        if(!receivingFile) {
            socketManager.write(("$side req f $filename $deviceId $myDeviceId").toByteArray(StandardCharsets.UTF_8))
            println("successful req!")
        }
    }


    fun fileReq(filename: String, deviceId: String, targetId: String){
        if(!receivingFile) {
            sockets[targetId]!!.write(("$side req f $filename $deviceId $myDeviceId").toByteArray(StandardCharsets.UTF_8))
            println("successful req!")
        }
    }

    fun fileRes(filename: String, deviceId: String){
        socketManager.write(("$side res f $filename $deviceId $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun fileRes(filename: String, deviceId: String, targetId : String){
        sockets[targetId]!!.write(("$side res f $filename $deviceId $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun fileRes(file: PeerFile){
        socketManager.write(("$side res f " + file.filename + " " + file.peerDeviceId + " $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun fileRes(file: PeerFile, targetId: String){
        sockets[targetId]!!.write(("$side res f " + file.filename + " " + file.peerDeviceId + " $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }

    fun peerRes(legacyPeer: LegacyPeer){
        socketManager.write(("$side res p $legacyPeer $myDeviceId").toByteArray(StandardCharsets.UTF_8))
    }


}