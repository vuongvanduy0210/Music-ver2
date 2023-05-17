package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.vuongvanduy.music.model.Song

class DataViewModel : ViewModel() {

    private val listSongOnline: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val listSongFavourite: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val listSongDevice: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val favouriteSong: MutableLiveData<Song> by lazy {
        MutableLiveData<Song>()
    }

    private val user: MutableLiveData<FirebaseUser> by lazy {
        MutableLiveData<FirebaseUser>()
    }

    fun setListSongsOnline(list: MutableList<Song>) {
        this.listSongOnline.value = list
    }

    fun setListSongsFavourite(list: MutableList<Song>) {
        this.listSongFavourite.value = list
    }

    fun setListSongsDevice(list: MutableList<Song>) {
        this.listSongDevice.value = list
    }

    fun setFavouriteSong(song: Song) {
        this.favouriteSong.value = song
    }

    fun setUser(user: FirebaseUser?) {
        this.user.value = user
    }

    fun getListSongsOnline() = listSongOnline
    fun getListSongsFavourite() = listSongFavourite
    fun getListSongsDevice() = listSongDevice
    fun getFavouriteSong(): LiveData<Song> = favouriteSong

    fun getUser(): LiveData<FirebaseUser> = user
}