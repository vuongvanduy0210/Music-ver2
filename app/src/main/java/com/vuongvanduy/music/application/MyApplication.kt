package com.vuongvanduy.music.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.vuongvanduy.music.R
import com.vuongvanduy.music.SharedPreferences.DataLocalManager
import com.vuongvanduy.music.util.CHANNEL_ID

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createDataLocalManager()
    }

    private fun createDataLocalManager() {
        DataLocalManager.init(applicationContext)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            channel.setSound(null, null)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}