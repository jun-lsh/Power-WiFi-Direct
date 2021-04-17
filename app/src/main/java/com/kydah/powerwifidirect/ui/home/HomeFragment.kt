package com.kydah.powerwifidirect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.kydah.powerwifidirect.R

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
            // Go to downloads
        }

        return root
    }
}