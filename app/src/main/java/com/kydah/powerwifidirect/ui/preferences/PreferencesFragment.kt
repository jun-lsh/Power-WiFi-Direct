package com.kydah.powerwifidirect.ui.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel

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
        childFragmentManager.beginTransaction()
            .replace(R.id.prefsContainerView, PrefFragment())
            .commit()

        return root
    }
}

class PrefFragment : PreferenceFragmentCompat(){

    private val networkViewModel : NetworkViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("transmission_mode")?.summary = networkViewModel.transmissionMode.value
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key){
            "transmission_mode" -> {
                networkViewModel.switchMode()
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent("CHANGE_TO_CLIENT"))
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

}