package com.kydah.powerwifidirect.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile

class PeerRecyclerAdapter(private val uploading: Boolean = false): RecyclerView.Adapter<PeerRecyclerAdapter.ViewHolder>() {
    var peers = arrayListOf<PeerFile>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peerId: TextView = itemView.findViewById(R.id.peer_id_textview)
        val fileName: TextView = itemView.findViewById(R.id.file_name_textview)
        val requestButton: Button = itemView.findViewById(R.id.request_button)
        val progressBar: ProgressBar = itemView.findViewById(R.id.uploading_progress_bar)

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