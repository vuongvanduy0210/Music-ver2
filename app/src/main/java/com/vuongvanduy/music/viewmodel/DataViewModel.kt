package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.model.Song

class DataViewModel : ViewModel() {

    private val listSongOnline: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }
    private val listSongDevice: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    fun setListSongsOnline(list: MutableList<Song>) {
        this.listSongOnline.value = list
    }

    fun setListSongsDevice(list: MutableList<Song>) {
        this.listSongDevice.value = list
    }

    fun getListSongsOnline() = listSongOnline
    fun getListSongsDevice() = listSongDevice
}