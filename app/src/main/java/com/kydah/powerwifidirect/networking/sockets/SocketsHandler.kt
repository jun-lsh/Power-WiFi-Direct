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
import com.kydah.powerwifidirect.utils.NotificationUtils.Companion.pushDownloadingNotification
import com.kydah.powerwifidirect.utils.NotificationUtils.Companion.pushUploadingNotification
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.properties.Delegates

class SocketsHandler(private val networkViewModel: NetworkViewModel, private val context: Context, private val side: String) : Handler(Looper.getMainLooper()){

    private lateinit var socketManager: SocketManager
    private var localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var receivingFile = false

    var sockets = HashMap<String, SocketManager>()

    private var asciiCharset = Charset.forName("ASCII")

    private var receivingFileName = ""
    private var targetSocket : String = ""

    private lateinit var receivedFile : FileOutputStream
    private var receivedFileChunks by Delegates.notNull<Int>()
    private var fullReceivedFileChunks by Delegates.notNull<Int>()

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
                socketManager.write(helloBuffer.toByteArray(asciiCharset))
            }

            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray

                if (!receivingFile) {
                    println(readBuf.size)
                    val readMessage = String(readBuf, asciiCharset)
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
                        //socketManager.write("ACK".toByteArray(asciiCharset))
                        println(receivedFileChunks)
                        receivedFile.write(readBuf, 0, readBuf.size)
                        receivedFileChunks--
                        pushDownloadingNotification(receivingFileName, "", fullReceivedFileChunks, fullReceivedFileChunks-receivedFileChunks, false, context)
                        if (receivedFileChunks == 0) {
                            receivedFile.close()
                            pushDownloadingNotification(receivingFileName, "", 0, 0, true, context)
                            receivingFile = false
                            if (targetSocket != "") {
                                val file = File(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp/" + receivingFileName)
                                sockets[targetSocket]!!.write(("svr res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt()).toByteArray(asciiCharset))
                                sockets[targetSocket]!!.readFile(file, true, targetSocket)
                                pushUploadingNotification(file.name, targetSocket, 0, 0, true, context)
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

            "cl" -> {
                sockets.remove(tokens[2])
                val fileList = networkViewModel.fileList.value!!
                val toRemove = arrayListOf<PeerFile>()
                for (file in fileList) {
                    if (file.peerDeviceId == tokens[2]) toRemove.add(file)
                }
                for (file in toRemove) {
                    fileList.remove(file)
                }
                for (socketMan in sockets) {
                    socketMan.component2().write(("svr r " + tokens[2]).toByteArray(asciiCharset))
                }
            }

            "req" -> {
                when (tokens[2]) {
                    "fl" -> {
                        val fileList = networkViewModel.fileList.value!!
                        for (file in networkViewModel.uploadsFolder.value!!.listFiles()) {
                            if (!file.isDirectory) fileRes(file.name!!, networkViewModel.deviceId.value!!)
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
                                    sockets[tokens[5]]!!.write(("svr res wf " + file.name + " " + ceil(file.length() / 65535.0).toInt()).toByteArray(asciiCharset))
                                    sockets[tokens[5]]!!.readFile(file, false, tokens[5])
                                    networkViewModel.pendingUploads.value!!.add(PeerFile("", file.name))
                                    networkViewModel.pendingUploads.value =networkViewModel.pendingUploads.value!!
                                    pushUploadingNotification(file.name, tokens[5], 0, 0, true, context)
                                }
                            }
                        } else {
                            sockets[tokens[4]]!!.write(("svr req tmpf " + tokens[3] + " " + tokens[5]).toByteArray(asciiCharset))
                            networkViewModel.pendingDownloads.value!!.add(PeerFile("", tokens[3]))
                            networkViewModel.pendingDownloads.value = networkViewModel.pendingDownloads.value!!
                            pushDownloadingNotification(tokens[3], tokens[5], 0, 0, true, context)
                        }
                    }
                }
            }
            "res" -> {
                when (tokens[2]) {
                    "f" -> {
                        if (tokens[4] != networkViewModel.deviceId.value!!) {
                            println("received " + tokens[3])
                            val peerFile = PeerFile(tokens[4], tokens[3])
                            networkViewModel.fileList.value!!.add(peerFile)
                            networkViewModel.fileList.value = networkViewModel.fileList.value!!
                        }
                        //println(networkViewModel.fileList.value!!.size)
                        //networkViewModel.fileList.postValue(networkViewModel.fileList.value!!)
                    }

                    "wf" -> {
                        receivingFile = true
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                        networkViewModel.pendingDownloads.value!!.add(PeerFile("", tokens[3]))
                        networkViewModel.pendingDownloads.value = networkViewModel.pendingDownloads.value!!
                        pushDownloadingNotification(tokens[3], "", 0, 0, true, context)
                        fullReceivedFileChunks = receivedFileChunks
                    }

                    "wtmpf" -> {
                        receivingFile = true
                        receivingFileName = tokens[3]
                        if (!File(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp").exists()) {
                            File(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp").mkdir()
                        }
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/tmp/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                        fullReceivedFileChunks = receivedFileChunks
                        networkViewModel.pendingDownloads.value!!.add(PeerFile("", tokens[3]))
                        networkViewModel.pendingDownloads.value = networkViewModel.pendingDownloads.value!!
                        pushDownloadingNotification(tokens[3], "", 0, 0, true, context)
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
                //sockets[tokens[2]] = socketManager
                localBroadcastManager.sendBroadcast(Intent("SOCK_MAN_OPEN"))
            }

            "cl" -> {
                networkViewModel.fileList.value = arrayListOf<PeerFile>()
                localBroadcastManager.sendBroadcast(Intent("GO_DISCONNECT"))
            }

            "r" -> {
                val fileList = networkViewModel.fileList.value!!
                val toRemove = arrayListOf<PeerFile>()
                for (file in fileList) {
                    if (file.peerDeviceId == tokens[2]) toRemove.add(file)
                }
                for (file in toRemove) {
                    fileList.remove(file)
                }
            }

            "req" -> {
                when (tokens[2]) {
                    "fl" -> {
                        val fileList = networkViewModel.fileList.value!!
                        for (file in networkViewModel.uploadsFolder.value!!.listFiles()) {
                            if (!file.isDirectory) fileRes(file.name!!, networkViewModel.deviceId.value!!)
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
                                socketManager.write(("clt res wf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt() + " " + tokens[4]).toByteArray(asciiCharset))
                                socketManager.readFile(file, false, "")
                                networkViewModel.pendingUploads.value!!.add(PeerFile("", file.name))
                                networkViewModel.pendingUploads.value =networkViewModel.pendingUploads.value!!
                                pushUploadingNotification(file.name, "", 0, 0, true, context)
                            }
                        }
                    }

                    "tmpf" -> {
                        val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                        for (file in fileList) {
                            if (file.name == tokens[3]) {
                                socketManager.write(("clt res wtmpf " + file.name + " " + Math.ceil(file.length() / 65535.0).toInt() + " " + tokens[4]).toByteArray(asciiCharset))
                                socketManager.readFile(file, false, "")
                                networkViewModel.pendingUploads.value!!.add(PeerFile("", file.name))
                                networkViewModel.pendingUploads.value =networkViewModel.pendingUploads.value!!
                                pushUploadingNotification(file.name, "", 0, 0, true, context)
                            }
                        }
                    }
                }
            }
            "res" -> {
                when (tokens[2]) {
                    "f" -> {
                        if (tokens[4] != networkViewModel.deviceId.value!!) {
                            println("received " + tokens[3])
                            val peerFile = PeerFile(tokens[4], tokens[3])
                            networkViewModel.fileList.value!!.add(peerFile)
                            networkViewModel.fileList.value = networkViewModel.fileList.value!!
                        }
                    }

                    "wf" -> {
                        receivingFile = true
                        receivedFile = FileOutputStream(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        println(networkViewModel.downloadsFolder.value!!.absolutePath + "/" + tokens[3])
                        receivedFileChunks = tokens[4].toInt()
                        fullReceivedFileChunks = receivedFileChunks
                        networkViewModel.pendingDownloads.value!!.add(PeerFile("", tokens[3]))
                        networkViewModel.pendingDownloads.value = networkViewModel.pendingDownloads.value!!
                        pushDownloadingNotification(tokens[3], "", 0, 0, true, context)

                    }
                }
            }
        }
    }


    private fun charsToBytes(chars: CharArray?): ByteArray? {
        val byteBuffer: ByteBuffer = asciiCharset.encode(CharBuffer.wrap(chars))
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit())
    }

    fun peerlistReq(){
        if(!receivingFile) socketManager.write((side + " req pl $myDeviceId").toByteArray(asciiCharset))
    }

    fun filelistReq(hops: Int, targetId: String){
        if(!receivingFile) sockets[targetId]!!.write((side + " req fl $myDeviceId").toByteArray(asciiCharset))
    }


    fun filelistReq(hops: Int){
        if(!receivingFile) socketManager.write((side + " req fl $myDeviceId").toByteArray(asciiCharset))
    }

    fun fileReq(filename: String, deviceId: String){
        if(!receivingFile) {
            socketManager.write((side + " req f $filename $deviceId $myDeviceId").toByteArray(asciiCharset))
            networkViewModel.pendingDownloads.value!!.add(PeerFile("", filename))
            networkViewModel.pendingDownloads.value = networkViewModel.pendingDownloads.value!!
            pushDownloadingNotification(filename, "", 0, 0, true, context)
            println("successful req!")
        }
    }

    fun fileReq(filename: String, deviceId: String, targetId: String){
        if(!receivingFile) {
            sockets[targetId]!!.write((side + " req f $filename $deviceId $myDeviceId").toByteArray(asciiCharset))
            println("successful req!")
        }
    }

    fun fileRes(filename: String, deviceId: String){
        socketManager.write((side + " res f $filename $deviceId $myDeviceId").toByteArray(asciiCharset))
    }

    fun fileRes(filename: String, deviceId: String, targetId: String){
        sockets[targetId]!!.write((side + " res f $filename $deviceId $myDeviceId").toByteArray(asciiCharset))
    }

    fun fileRes(file: PeerFile){
        socketManager.write((side + " res f " + file.filename + " " + file.peerDeviceId + " $myDeviceId").toByteArray(asciiCharset))
    }

    fun fileRes(file: PeerFile, targetId: String){
        sockets[targetId]!!.write((side + " res f " + file.filename + " " + file.peerDeviceId + " $myDeviceId").toByteArray(asciiCharset))
    }

    fun peerRes(legacyPeer: LegacyPeer){
        socketManager.write((side + " res p $legacyPeer $myDeviceId").toByteArray(asciiCharset))
    }

    fun clientCloseConnection(){
        socketManager.closeConnection("clt cl $myDeviceId")
    }

    fun serverCloseConnection(){
        for(socketMan in sockets){
            socketMan.component2().closeConnection("svr cl $myDeviceId")
        }
    }
}