package com.kydah.powerwifidirect.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import me.xdrop.fuzzywuzzy.FuzzySearch
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.PeerRecyclerAdapter

private const val SEARCH_CUT_OFF = 50
class SearchFragment : Fragment() {

    private lateinit var searchViewModel: SearchViewModel
    private var allPeers = arrayListOf<PeerFile>()
    private val networkViewModel : NetworkViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_search, container, false)

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val peerRecyclerView: RecyclerView = root.findViewById(R.id.peer_recycler_view)
        peerRecyclerView.layoutManager = LinearLayoutManager(context)
        val peerRecyclerAdapter = PeerRecyclerAdapter()
        peerRecyclerView.adapter = peerRecyclerAdapter

        fillPeerRecyclerAdapter(peerRecyclerAdapter)

        // Execute search and hide keyboard on click
        val searchInput: TextInputEditText = root.findViewById(R.id.search_input)
        val searchButton: ImageButton = root.findViewById(R.id.search_button)
        searchButton.setOnClickListener {
            hideSoftKeyboard(root)
            // Search for peer
            val peerName = searchInput.text.toString()
            if (peerName.isNotEmpty()) {
                peerRecyclerAdapter.peers = searchForPeer(peerName, allPeers)
                peerRecyclerAdapter.notifyDataSetChanged()
            }
            val fileName = searchInput.text.toString()
            startFileRequest()
        }

        val clearButton: ImageButton = root.findViewById(R.id.clear_search_button)
        // Clear search input and refill peer recycler adapter
        clearButton.setOnClickListener {
            hideSoftKeyboard(root)

            searchInput.text = null
            peerRecyclerAdapter.peers = allPeers
            peerRecyclerAdapter.notifyDataSetChanged()
        }

        val receivedFab: FloatingActionButton = root.findViewById(R.id.received_fab)
        receivedFab.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_search_to_navigation_received)
        }

        return root
    }

    private fun hideSoftKeyboard(root: View) {
        val activity = requireActivity()
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(root.windowToken, 0)
    }

    private fun startFileRequest(){
        if(networkViewModel.transmissionMode.value == "Server"){
            networkViewModel.switchMode()
            val intent = Intent("CLIENT_ACTION")
            intent.putExtra("ACTION_TYPE", "FILE_REQ_CHANGE")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent("CHANGE_TO_CLIENT"))
        } else {
            val intent = Intent("CLIENT_ACTION")
            intent.putExtra("ACTION_TYPE", "FILE_REQ_NO_CHANGE")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        }
    }

    private fun searchForPeer(fileName: String, data: ArrayList<PeerFile>): ArrayList<PeerFile> {
        // Get peer name list from peers
        val peerNameList = arrayListOf<String>()
        data.forEach {
            peerNameList.add(it.filename)
        }

        // Sort peer names with cutoff point and create arraylist of sorted peers
        val sortedPeerNames = FuzzySearch.extractSorted(fileName, peerNameList, SEARCH_CUT_OFF)
        val sortedPeers = arrayListOf<PeerFile>()

        sortedPeerNames.forEach {
            sortedPeers.add(data[it.index])
        }



        return sortedPeers

    }

    private fun fillPeerRecyclerAdapter(recyclerAdapter: PeerRecyclerAdapter){
        val peers = arrayListOf<PeerFile>()
        // Do code for getting peers here

        // This arraylist is for searching
        allPeers = peers

        recyclerAdapter.peers = peers
        recyclerAdapter.notifyDataSetChanged()
    }
}