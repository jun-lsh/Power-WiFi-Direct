package com.kydah.powerwifidirect.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.kydah.powerwifidirect.R

class NotificationUtils {
    companion object {
        private lateinit var notificationManager : NotificationManager
        private lateinit var notificationChannel: NotificationChannel
        private lateinit var currentNotificationChannelID : String

        private var runningNotificationId = 2
        private val fileNotificationId = HashMap<String, Int>()

        fun createNotificationManager(activity: Activity){
            notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

// Importance can be set using NotificationManager.IMPORTANCE_XXXXXX
// channelID ideally should be in the form com.xxxxxx.xxxxxxx

        @RequiresApi(Build.VERSION_CODES.O)
        fun createNotificationChannel(channelID : String, channelName : String, channelDescription: String, importance : Int){
            notificationChannel = NotificationChannel(channelID, channelName, importance)
            notificationChannel.description = channelDescription
            notificationManager.createNotificationChannel(notificationChannel)
            currentNotificationChannelID = notificationChannel.id
        }

// Get the icon from drawable

        @RequiresApi(Build.VERSION_CODES.O)
        fun pushNotification(notificationID: Int, title: String, text: String, context: Context, channelID: String, icon: Int, targetActivity: Activity?, persistent : Boolean, onlyOnce : Boolean){
            val pendingIntent : PendingIntent = PendingIntent.getActivity(context, 0, Intent(context, targetActivity!!::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            val builder : Notification.Builder = Notification.Builder(context, channelID)
            if(icon != -1) builder.setSmallIcon(icon)
            builder.setContentTitle(title)
            builder.setContentText(text)
            builder.setContentIntent(pendingIntent)
            builder.setChannelId(channelID)
            builder.setOngoing(persistent)
            builder.setOnlyAlertOnce(onlyOnce)
            notificationManager.notify(notificationID, builder.build())
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun pushNotification(notificationID : Int, title : String, text : String, context: Context){
            pushNotification(notificationID, title, text, context, currentNotificationChannelID, -1, null, false, false)
        }


        @RequiresApi(Build.VERSION_CODES.O)
        fun pushNotification(notificationID : Int, title : String, text : String, context: Context, icon : Int, targetActivity: Activity, persistent: Boolean, onlyOnce: Boolean){
            pushNotification(notificationID, title, text, context, currentNotificationChannelID, icon, targetActivity, persistent, onlyOnce)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun pushNotification(notificationID : Int, title : String, text : String, context: Context, targetActivity: Activity) {
            pushNotification(notificationID, title, text, context, currentNotificationChannelID, -1, targetActivity, false, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun pushNotification(notificationID : Int, title : String, text : String, context: Context, icon : Int, targetActivity: Activity) {
            pushNotification(notificationID, title, text, context, currentNotificationChannelID, icon, targetActivity, false, false)
        }

        fun pushProgressNotification(notificationID: Int, title : String, text : String, icon : Int, packets:Int, progress : Int, pending : Boolean, context: Context){
            val builder : Notification.Builder = Notification.Builder(context, currentNotificationChannelID)
            builder.setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(icon)
                    .setProgress(packets, progress, pending)
            notificationManager.notify(notificationID, builder.build())

        }

        fun pushUploadingNotification(filename : String, deviceName : String, packets : Int, progress : Int, pending : Boolean, context: Context){
            val notificationID  : Int
            if(fileNotificationId[filename+deviceName] != null) notificationID = fileNotificationId[filename+deviceName]!!
            else {
                notificationID = runningNotificationId
                fileNotificationId[filename+deviceName] = notificationID
                runningNotificationId++
            }
            if(packets != progress && packets != 0) pushProgressNotification(notificationID, "Uploading file: $filename", "File source: $deviceName", R.drawable.ic_baseline_upload_24, packets, progress, pending, context)
            else pushProgressNotification(notificationID, "Uploaded file: $filename", "File source: $deviceName", R.drawable.ic_baseline_task_alt_24, 0, 0, false, context)
        }


        fun pushDownoadingNotification(filename : String, deviceName : String, packets : Int, progress : Int, pending : Boolean, context: Context){
            val notificationID  : Int
            if(fileNotificationId[filename+deviceName] != null) notificationID = fileNotificationId[filename+deviceName]!!
            else {
                notificationID = runningNotificationId
                fileNotificationId[filename+deviceName] = notificationID
                runningNotificationId++
            }
            if(packets != progress && packets != 0) pushProgressNotification(notificationID, "Downloading file: $filename", "File source: $deviceName", R.drawable.ic_baseline_download_24, packets, progress, pending, context)
            else pushProgressNotification(notificationID, "Downloaded file: $filename", "File source: $deviceName", R.drawable.ic_baseline_task_alt_24, 0, 0, false, context)
        }

    }
}