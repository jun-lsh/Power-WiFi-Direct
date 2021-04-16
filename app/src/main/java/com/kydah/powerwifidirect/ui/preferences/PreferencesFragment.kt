package com.kydah.powerwifidirect.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kydah.powerwifidirect.R

class PreferencesFragment : Fragment() {

    private lateinit var preferencesViewModel: PreferencesViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        preferencesViewModel =
                ViewModelProvider(this).get(PreferencesViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_prefs, container, false)
        val textView: TextView = root.findViewById(R.id.text_notifications)
        preferencesViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}