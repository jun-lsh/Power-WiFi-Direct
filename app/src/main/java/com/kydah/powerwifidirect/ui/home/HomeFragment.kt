package com.kydah.powerwifidirect.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.model.PeerFile
import com.kydah.powerwifidirect.ui.adapters.PeerRecyclerAdapter
import com.kydah.powerwifidirect.networking.NetworkViewModel


class HomeFragment : Fragment() {

    private val networkViewModel : NetworkViewModel by activityViewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val deviceName : TextView = root.findViewById(R.id.device_name_textview)
        val deviceId : TextView = root.findViewById(R.id.device_address_textview)
        val fileNumber : TextView = root.findViewById(R.id.files_hosted_textview)

        networkViewModel.accessPoint.observe(viewLifecycleOwner, {
            deviceName.text = it.ssid
        })

        networkViewModel.deviceId.observe(viewLifecycleOwner, {
            deviceId.text = it
        })

        networkViewModel.uploadsFolder.observe(viewLifecycleOwner, {
            fileNumber.text = "Currently hosting: " + it.listFiles().size + " files"
        })

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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val snackbar = Snackbar.make(requireActivity().findViewById(android.R.id.content), "Connecting to a group owner... ", Snackbar.LENGTH_INDEFINITE)
        val snackView = snackbar.view
        val snackViewParams = (snackView.layoutParams as FrameLayout.LayoutParams)
        snackViewParams.gravity = Gravity.TOP
        snackView.layoutParams = snackViewParams
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE

        networkViewModel.connectingToAP.observe(viewLifecycleOwner, {
            if(it) snackbar.show()
            else snackbar.dismiss()
        })
    }
}

