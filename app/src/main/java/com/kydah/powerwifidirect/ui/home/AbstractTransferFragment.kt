package com.kydah.powerwifidirect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.adapters.PeerRecyclerAdapter

abstract class AbstractTransferFragment: Fragment() {

    private lateinit var transferringRecyclerView: RecyclerView
    private lateinit var pendingRecyclerView: RecyclerView
    private lateinit var noTransferringFilesNotice: TextView
    private lateinit var noPendingFilesNotice: TextView

    private lateinit var transferAdapter : PeerRecyclerAdapter
    private lateinit var pendingAdapter : PeerRecyclerAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = getView(inflater, container)

        // Transferring recycler view init
        transferringRecyclerView = root.findViewById(R.id.transferring_recycler_view)
        transferringRecyclerView.layoutManager = LinearLayoutManager(context)
        transferAdapter = PeerRecyclerAdapter(false)
        transferringRecyclerView.adapter = transferAdapter

        noTransferringFilesNotice = root.findViewById(R.id.no_transferring_files)

        // Pending recycler view init
        pendingRecyclerView = root.findViewById(R.id.pending_recycler_view)
        pendingRecyclerView.layoutManager = LinearLayoutManager(context)
        pendingAdapter = PeerRecyclerAdapter(false)
        pendingRecyclerView.adapter = pendingAdapter

        noPendingFilesNotice = root.findViewById(R.id.no_pending_files)

        return root
    }

    abstract fun getView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View

    protected fun fillTransferringRecyclerAdapter(peerFiles : ArrayList<PeerFile>) {
        transferAdapter.peers = peerFiles
        transferAdapter.notifyDataSetChanged()

        // Show no uploading files if recycler adapter is empty
        if (transferAdapter.itemCount == 0) {
            transferringRecyclerView.visibility = View.GONE
            noTransferringFilesNotice.visibility = View.VISIBLE
        }
        else {
            transferringRecyclerView.visibility = View.VISIBLE
            noTransferringFilesNotice.visibility = View.GONE
        }
    }

    protected fun fillPendingRecyclerAdapter(peerFiles : ArrayList<PeerFile>) {
        pendingAdapter.peers = peerFiles
        pendingAdapter.notifyDataSetChanged()

        // Show no pending files if recycler adapter is empty
        if (pendingAdapter.itemCount == 0) {
            pendingRecyclerView.visibility = View.GONE
            noPendingFilesNotice.visibility = View.VISIBLE
        }
        else {
            pendingRecyclerView.visibility = View.VISIBLE
            noPendingFilesNotice.visibility = View.GONE
        }
    }

}