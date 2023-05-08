package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song

class FavouriteSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    fun getSongs(): LiveData<MutableList<Song>> = songs
}