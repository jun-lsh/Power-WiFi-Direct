package com.kydah.powerwifidirect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.PeerFile

class DownloadsFragment: AbstractTransferFragment() {

    private val networkViewModel : NetworkViewModel by activityViewModels()

    override fun getView(inflater: LayoutInflater, container: ViewGroup?): View {
        val root = inflater.inflate(R.layout.fragment_transfers, container, false)

        val uploadsTextView: TextView = root.findViewById(R.id.transfers_placeholder_textview)
        uploadsTextView.text = resources.getString(R.string.downloads)

        val noUploadsTextView: TextView = root.findViewById(R.id.no_transferring_files)
        noUploadsTextView.text = resources.getString(R.string.no_files_downloading)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        networkViewModel.downloading.observe(viewLifecycleOwner, {
            println("download added")
            fillTransferringRecyclerAdapter(it)
        })
        networkViewModel.pendingDownloads.observe(viewLifecycleOwner, {
            fillTransferringRecyclerAdapter(it)
        })
    }

}