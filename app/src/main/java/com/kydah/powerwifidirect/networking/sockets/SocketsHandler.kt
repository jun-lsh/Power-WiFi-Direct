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
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.properties.Delegates

class SocketsHandler(private val networkViewModel: NetworkViewModel, private val context: Context) : Handler(Looper.getMainLooper()){

    private lateinit var socketManager: SocketManager
    private var localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var receivingFile = false

    private lateinit var receivedFile : FileOutputStream
    private var receivedFileChunks by Delegates.notNull<Int>()

    @SuppressLint("HardwareIds")
    override fun handleMessage(msg: Message) {
        when(msg.what){

            GET_OBJ -> {
                socketManager = msg.obj as SocketManager
            }

            HELLO -> {
                val helloBuffer = socketManager.side + " " + networkViewModel.deviceId.value + "\n"
                socketManager.write(helloBuffer.toByteArray())
                localBroadcastManager.sendBroadcast(Intent("SOCK_MAN_OPEN"))
            }

            MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray

                if (!receivingFile) {
                    val readMessage = String(readBuf, 0, msg.arg1)
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
                    if(receivedFileChunks == 0) {
                        receivingFile = false
                        receivedFile.close()
                    } else {
                        receivedFile.write(readBuf, 0, readBuf.size)
                        receivedFileChunks--
                    }
                }
            }
        }
    }

    private fun serverParser(tokens: List<String>){
        if(tokens[0] != "clt") return
        when(tokens[1]){
            "req" -> {
                when (tokens[2]) {
                    "fl" -> {
                        val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                        for (file in fileList) {
                            println(file.name)
                            fileRes(file.name,
                                    networkViewModel.deviceId.value!!
                            )
                        }
                    }

                    "pl" -> {
                        for (peer in networkViewModel.peerList.value!!) {
                            peerRes(peer)
                        }
                    }

                    "f" -> {
                        val fileList = networkViewModel.uploadsFolder.value!!.listFiles()
                        for (file in fileList) {
                            if (file.name == tokens[3]) {
                                socketManager.readFile(file)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clientParser(tokens: List<String>){
        if(tokens[0] != "svr") return
        when(tokens[1]){
            "res" -> {
                when (tokens[2]) {
                    "f" -> {
                        println("received " + tokens[3])
                        val peerFile = PeerFile(tokens[4], tokens[3])
                        networkViewModel.fileList.value!!.add(peerFile)
                        networkViewModel.fileList.value = networkViewModel.fileList.value!!
                        //println(networkViewModel.fileList.value!!.size)
                        //networkViewModel.fileList.postValue(networkViewModel.fileList.value!!)
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
        socketManager.write(("clt req pl\n").toByteArray())
    }

    fun filelistReq(hops: Int){
        socketManager.write(("clt req fl $hops\n").toByteArray())
    }

    fun fileReq(filename: String, deviceId: String){
        socketManager.write(("clt req f $filename $deviceId\n").toByteArray())
    }

    fun fileRes(filename: String, deviceId: String){
        socketManager.write(("svr res f $filename $deviceId\n").toByteArray())
    }

    fun peerRes(peer: Peer){
        socketManager.write(("svr res p $peer\n").toByteArray())
    }


}