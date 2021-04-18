package com.kydah.powerwifidirect.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import org.w3c.dom.Text

class HomeFragment : Fragment() {

    private val networkViewModel : NetworkViewModel by activityViewModels()

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

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceName : TextView = view.findViewById(R.id.device_name_textview)
        val deviceId : TextView = view.findViewById(R.id.device_address_textview)
        val fileNumber : TextView = view.findViewById(R.id.files_hosted_textview)

        networkViewModel.accessPoint.observe(viewLifecycleOwner, {
            deviceName.text = it.ssid
        })

        networkViewModel.deviceId.observe(viewLifecycleOwner, {
            deviceId.text = it
        })

        networkViewModel.uploadsFolder.observe(viewLifecycleOwner, {
            fileNumber.text = "Currently hosting: " + it.listFiles().size + " files"
        })

    }
}