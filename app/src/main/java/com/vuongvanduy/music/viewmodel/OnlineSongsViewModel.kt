package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song

class OnlineSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private lateinit var song: Song

    fun setData(list: MutableList<Song>) {
        songs.value = list
    }

    fun setSong(song: Song) {
        this.song = song
    }

    fun getSongs(): LiveData<MutableList<Song>> = songs

    fun getSong() = song
}