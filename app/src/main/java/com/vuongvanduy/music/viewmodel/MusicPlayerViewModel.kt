package com.vuongvanduy.music.viewmodel

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*


class MusicPlayerViewModel : ViewModel() {

    lateinit var currentSong: Song
    val action: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    var currentTime: Int = 0
    val finalTime: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val isPlaying: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val isLooping: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val isShuffling: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    fun receiveDataFromServiceReceiver(intent: Intent) {
        if (intent.action == SEND_DATA) {
            val bundle = intent.extras
            if (bundle != null) {
                currentSong = bundle.getSerializable(KEY_SONG) as Song
                action.value = bundle.getInt(KEY_ACTION)
                isPlaying.value = bundle.getBoolean(KEY_STATUS_MUSIC)
                isLooping.value = bundle.getBoolean(KEY_STATUS_LOOP)
                isShuffling.value = bundle.getBoolean(KEY_STATUS_SHUFFLE)
                finalTime.value = bundle.getInt(KEY_FINAL_TIME)
            }
        }
    }

    fun receiveCurrentTime(intent: Intent) {
        if (intent.action == SEND_CURRENT_TIME) {
            val bundle = intent.extras
            if (bundle != null) {
                currentTime =bundle.getInt(KEY_CURRENT_TIME)
            }
        }
    }


}