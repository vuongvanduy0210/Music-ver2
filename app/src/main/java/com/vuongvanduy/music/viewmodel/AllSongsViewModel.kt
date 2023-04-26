package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song

class AllSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    fun setData(list: MutableList<Song>) {
        songs.value = list
    }

    fun getSongs(): LiveData<MutableList<Song>> = songs
}