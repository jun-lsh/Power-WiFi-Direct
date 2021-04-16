package com.kydah.powerwifidirect.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile

class PeerRecyclerAdapter: RecyclerView.Adapter<PeerRecyclerAdapter.ViewHolder>() {
    var peers = arrayListOf<PeerFile>()

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val peerId: TextView = itemView.findViewById(R.id.peer_id_textview)
        val fileName: TextView = itemView.findViewById(R.id.file_name_textview)
        private val requestButton: Button = itemView.findViewById(R.id.request_button)

        init {
            requestButton.setOnClickListener {
                // Do ur stuff
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
    }

    override fun getItemCount(): Int {
        return peers.size
    }
}