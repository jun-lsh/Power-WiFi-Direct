package com.kydah.powerwifidirect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.OnBackPressedListener
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.PeerRecyclerAdapter

abstract class AbstractTransferFragment: Fragment() {

    private lateinit var transferringRecyclerView: RecyclerView
    private lateinit var pendingRecyclerView: RecyclerView
    private lateinit var noTransferringFilesNotice: TextView
    private lateinit var noPendingFilesNotice: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = getView(inflater, container)

        // Transferring recycler view init
        transferringRecyclerView = root.findViewById(R.id.transferring_recycler_view)
        transferringRecyclerView.layoutManager = LinearLayoutManager(context)
        val transferringRecyclerAdapter = PeerRecyclerAdapter(true)
        transferringRecyclerView.adapter = transferringRecyclerAdapter

        noTransferringFilesNotice = root.findViewById(R.id.no_transferring_files)
        fillTransferringRecyclerAdapter(transferringRecyclerAdapter)

        // Pending recycler view init
        pendingRecyclerView = root.findViewById(R.id.pending_recycler_view)
        pendingRecyclerView.layoutManager = LinearLayoutManager(context)
        val pendingRecyclerAdapter = PeerRecyclerAdapter(false)
        pendingRecyclerView.adapter = pendingRecyclerAdapter

        noPendingFilesNotice = root.findViewById(R.id.no_pending_files)
        fillPendingRecyclerAdapter(pendingRecyclerAdapter)

        return root
    }

    abstract fun getView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View

    private fun fillTransferringRecyclerAdapter(recyclerAdapter: PeerRecyclerAdapter) {
        recyclerAdapter.peers = fillTransferringRecyclerAdapter()
        recyclerAdapter.notifyDataSetChanged()

        // Show no uploading files if recycler adapter is empty
        if (recyclerAdapter.itemCount == 0) {
            transferringRecyclerView.visibility = View.GONE
            noTransferringFilesNotice.visibility = View.VISIBLE
        }
        else {
            transferringRecyclerView.visibility = View.VISIBLE
            noTransferringFilesNotice.visibility = View.GONE
        }
    }

    abstract fun fillTransferringRecyclerAdapter(): ArrayList<PeerFile>

    private fun fillPendingRecyclerAdapter(recyclerAdapter: PeerRecyclerAdapter) {
        val peers = arrayListOf<PeerFile>()
        // Fill shit here

        recyclerAdapter.peers = peers
        recyclerAdapter.notifyDataSetChanged()


        // Show no pending files if recycler adapter is empty
        if (recyclerAdapter.itemCount == 0) {
            pendingRecyclerView.visibility = View.GONE
            noPendingFilesNotice.visibility = View.VISIBLE
        }
        else {
            pendingRecyclerView.visibility = View.VISIBLE
            noPendingFilesNotice.visibility = View.GONE
        }
    }

    abstract fun fillPendingRecyclerAdapter(): ArrayList<PeerFile>
}