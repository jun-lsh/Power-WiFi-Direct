package com.kydah.powerwifidirect.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.ui.setfolders.SetFoldersDialog

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        networkViewModel.transmissionMode.observe(viewLifecycleOwner, {
            findPreference<Preference>("transmission_mode")?.summary = it
        })
        networkViewModel.downloadsFolder.observe(viewLifecycleOwner, {
            findPreference<Preference>("downloads_folder")?.summary = it.canonicalPath
        })
        networkViewModel.uploadsFolder.observe(viewLifecycleOwner, {
            findPreference<Preference>("uploads_folder")?.summary = it.canonicalPath
        })
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key){
            "transmission_mode" -> {
            }
            "downloads_folder", "uploads_folder" -> {
                requireActivity().supportFragmentManager.let{ it1 ->
                    SetFoldersDialog(true).show(it1, "as_pop_up")
                }
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

}