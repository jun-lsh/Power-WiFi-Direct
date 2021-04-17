package com.kydah.powerwifidirect.ui.uploads

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
import com.kydah.powerwifidirect.ui.PeerRecyclerAdapter

class UploadsFragment: Fragment() {

    private lateinit var uploadingRecyclerView: RecyclerView
    private lateinit var pendingRecyclerView: RecyclerView
    private lateinit var noUploadingFilesNotice: TextView
    private lateinit var noPendingFilesNotice: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_uploads, container, false)

        // Uploading recycler view init
        uploadingRecyclerView = root.findViewById(R.id.uploading_recycler_view)
        uploadingRecyclerView.layoutManager = LinearLayoutManager(context)
        val uploadingRecyclerAdapter = PeerRecyclerAdapter(true)
        uploadingRecyclerView.adapter = uploadingRecyclerAdapter

        noUploadingFilesNotice = root.findViewById(R.id.no_uploading_files)
        fillUploadingRecyclerAdapter(uploadingRecyclerAdapter)

        // Pending recycler view init
        pendingRecyclerView = root.findViewById(R.id.pending_recycler_view)
        pendingRecyclerView.layoutManager = LinearLayoutManager(context)
        val pendingRecyclerAdapter = PeerRecyclerAdapter(false)
        pendingRecyclerView.adapter = pendingRecyclerAdapter

        noPendingFilesNotice = root.findViewById(R.id.no_pending_files)
        fillPendingRecyclerAdapter(pendingRecyclerAdapter)

        return root
    }

    private fun fillUploadingRecyclerAdapter(recyclerAdapter: PeerRecyclerAdapter) {
        val peers = arrayListOf<PeerFile>()
        // Fill shit here

        recyclerAdapter.peers = peers
        recyclerAdapter.notifyDataSetChanged()


        // Show no uploading files if recycler adapter is empty
        if (recyclerAdapter.itemCount == 0) {
            uploadingRecyclerView.visibility = View.GONE
            noUploadingFilesNotice.visibility = View.VISIBLE
        }
        else {
            uploadingRecyclerView.visibility = View.VISIBLE
            noUploadingFilesNotice.visibility = View.GONE
        }
    }

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
}