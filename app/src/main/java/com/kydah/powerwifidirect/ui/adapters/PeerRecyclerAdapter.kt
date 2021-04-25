package com.kydah.powerwifidirect.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile

class PeerRecyclerAdapter(private val uploading: Boolean = false, private var context: Context? = null): RecyclerView.Adapter<PeerRecyclerAdapter.ViewHolder>() {
    var peers = arrayListOf<PeerFile>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peerId: TextView = itemView.findViewById(R.id.peer_id_textview)
        val fileName: TextView = itemView.findViewById(R.id.file_name_textview)
        val requestButton: Button = itemView.findViewById(R.id.request_button)
        val progressBar: ProgressBar = itemView.findViewById(R.id.uploading_progress_bar)
        val filetypeIcon : ImageView = itemView.findViewById(R.id.filetypeIcon)

        init {
            requestButton.setOnClickListener {
                if(context != null){
                    println("making file request for: " + fileName.text)
                    val intent = Intent("SOCKET_ACTION")
                    intent.putExtra("ACTION_TYPE", "SPECIFIC_FILE_REQ")
                    intent.putExtra("PEER_ID", peerId.text)
                    intent.putExtra("FILENAME", fileName.text)
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.card_layout_peer, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peerFile = peers[position]
        holder.peerId.text = peerFile.peerDeviceId
        holder.fileName.text = peerFile.filename

        //val extension = peerFile.filename.substring(peerFile.filename.lastIndexOf("."))


        if (uploading) {
            holder.requestButton.visibility = View.GONE
            holder.progressBar.visibility = View.VISIBLE

            // Set the progress bar here have fun
        }
        else {
            holder.requestButton.visibility = View.VISIBLE
            holder.progressBar.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return peers.size
    }
}