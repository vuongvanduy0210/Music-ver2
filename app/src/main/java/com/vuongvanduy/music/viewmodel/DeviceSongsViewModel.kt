package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song

class DeviceSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private lateinit var song: Song

    fun getSongs(): LiveData<MutableList<Song>> = songs

    fun setSong(song: Song) {
        this.song = song
    }


    fun setData(list: MutableList<Song>) {
        this.songs.value = list
    }

    fun getSong() = song
}