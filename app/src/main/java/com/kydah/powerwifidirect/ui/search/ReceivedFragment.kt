package com.kydah.powerwifidirect.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.adapters.PeerRecyclerAdapter

class ReceivedFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_received, container, false)

        val receivedRecyclerView: RecyclerView = root.findViewById(R.id.received_recycler_view)
        receivedRecyclerView.layoutManager = LinearLayoutManager(context)
        val receivedRecyclerAdapter = PeerRecyclerAdapter(false, requireContext())
        receivedRecyclerView.adapter = receivedRecyclerAdapter

        val peers = arrayListOf<PeerFile>()
        // Fill it up here

        receivedRecyclerAdapter.peers = peers
        receivedRecyclerAdapter.notifyDataSetChanged()


        return root
    }
}