package com.vuongvanduy.music.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vuongvanduy.music.service.MusicService
import com.vuongvanduy.music.util.*

class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        //receive action from notification and send to service
        val actionMusic = intent?.getIntExtra(ACTION_MUSIC_NAME, 0)

        val intentService = Intent(context, MusicService::class.java)
        intentService.putExtra(KEY_ACTION, actionMusic)
        context?.startService(intentService)
    }
}