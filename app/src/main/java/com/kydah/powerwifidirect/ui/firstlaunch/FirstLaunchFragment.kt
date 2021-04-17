package com.kydah.powerwifidirect.ui.firstlaunch

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.kydah.powerwifidirect.R
import com.kydah.powerwifidirect.networking.NetworkViewModel
import com.kydah.powerwifidirect.ui.home.HomeViewModel
import com.kydah.powerwifidirect.utils.FileUtils
import org.w3c.dom.Text
import java.io.File

import kotlin.properties.Delegates

class FirstLaunchFragment  : DialogFragment(){

    private lateinit var folderType : String
    private lateinit var intent : Intent
    private lateinit var launchIntent : ActivityResultLauncher<Intent>

    private var dlSet by Delegates.notNull<Boolean>()
    private var ulSet by Delegates.notNull<Boolean>()
    private lateinit var sharedPrefs : SharedPreferences

    private val networkViewModel : NetworkViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_first_launch, container, false)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launchIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                val data = it.data
                val uri = data!!.data
                val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )
                val path = FileUtils.getPath(context, docUri)
                when(folderType){
                    "downloads_folder" -> {
                        sharedPrefs.edit().putString("downloads_folder", path).apply()
                        networkViewModel.downloadsFolder.value = File(path)
                    }
                    "uploads_folder" -> {
                        sharedPrefs.edit().putString("uploads_folder", path).apply()
                        networkViewModel.uploadsFolder.value = File(path)
                    }
                }
            }
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val proceedButton : Button = view.findViewById(R.id.afterSetPaths)
        val chooseDownloads: ImageButton = view.findViewById(R.id.chooseDownload)
        val chooseUploads : ImageButton = view.findViewById(R.id.chooseUpload)
        val downloadsPath : TextView = view.findViewById(R.id.downloadsPath)
        val uploadsPath : TextView = view.findViewById(R.id.uploadsPath)

        proceedButton.isEnabled = false
        ulSet = false; dlSet = false

        proceedButton.setOnClickListener {
            sharedPrefs.edit().putBoolean("set_folders", true).apply()
            dialog?.dismiss()
        }

        networkViewModel.downloadsFolder.observe(viewLifecycleOwner, {
            dlSet = it.isDirectory
            downloadsPath.text = it.canonicalPath
            if(dlSet && ulSet) proceedButton.isEnabled = true
        })


        networkViewModel.uploadsFolder.observe(viewLifecycleOwner, {
            ulSet = it.isDirectory
            uploadsPath.text = it.canonicalPath
            if(dlSet && ulSet) proceedButton.isEnabled = true
        })

        chooseDownloads.setOnClickListener{
            folderType = "downloads_folder"
            launchIntent.launch(intent)
        }

        chooseUploads.setOnClickListener{
            folderType = "uploads_folder"
            launchIntent.launch(intent)
        }


    }


}