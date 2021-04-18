package com.kydah.powerwifidirect.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile

class UploadsFragment: AbstractTransferFragment() {
    override fun getView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        val root = inflater.inflate(R.layout.fragment_transfers, container, false)

        val uploadsTextView: TextView = root.findViewById(R.id.transfers_placeholder_textview)
        uploadsTextView.text = resources.getString(R.string.uploads)

        val noUploadsTextView: TextView = root.findViewById(R.id.no_transferring_files)
        noUploadsTextView.text = resources.getString(R.string.no_files_uploading)

        return root
    }

    override fun fillTransferringRecyclerAdapter(): ArrayList<PeerFile> {
        // Return array list of peer files to fill the recycler adapter
        return arrayListOf()
    }

    override fun fillPendingRecyclerAdapter(): ArrayList<PeerFile> {
        // Return array list of peer files to fill the recycler adapter
        return arrayListOf()
    }
}