package com.kydah.powerwifidirect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.PeerRecyclerAdapter

private val historyRecyclerAdapter = PeerRecyclerAdapter()
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val uploadButton: ImageButton = root.findViewById(R.id.upload_button)
        val downloadButton: ImageButton = root.findViewById(R.id.download_button)

        uploadButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_uploads)
        }
        downloadButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_downloads)
        }

        val historyRecyclerView: RecyclerView = root.findViewById(R.id.history_recycler_view)
        historyRecyclerView.layoutManager = LinearLayoutManager(context)
        historyRecyclerView.adapter = historyRecyclerAdapter

        return root
    }
}

fun addToHistory(peerFile: PeerFile) {
    historyRecyclerAdapter.peers.add(0, peerFile)
    historyRecyclerAdapter.notifyItemInserted(0)

    if (historyRecyclerAdapter.peers.size> 15) {
        historyRecyclerAdapter.peers.removeAt(15)
        historyRecyclerAdapter.notifyItemRemoved(15)
    }
}