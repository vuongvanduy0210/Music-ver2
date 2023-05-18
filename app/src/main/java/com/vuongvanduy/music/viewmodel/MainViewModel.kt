package com.vuongvanduy.music.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.vuongvanduy.music.R
import com.vuongvanduy.music.fragment.MusicPlayerFragment
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.service.MusicService
import com.vuongvanduy.music.util.*
import java.io.Serializable

class MainViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
    private val onlineSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val favouriteSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val deviceSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val user: MutableLiveData<FirebaseUser> by lazy {
        MutableLiveData<FirebaseUser>()
    }

    var currentSong: Song? = null
    var currentListName: String? = null
    private val isPlaying: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val actionMusic: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    private var themeMode: String? = null

    fun setData(context: Context) {
        this.context = context
    }

    fun setUser(user: FirebaseUser?) {
        this.user.value = user
    }

    fun getUser(): LiveData<FirebaseUser> = user

    fun setOnlineSongs(list: MutableList<Song>) {
        onlineSongs.value = list
    }

    fun setFavouriteSongs(list: MutableList<Song>) {
        favouriteSongs.value = list
    }

    fun setDeviceSongs(list: MutableList<Song>) {
        deviceSongs.value = list
    }

    fun setThemeMode(mode: String) {
        this.themeMode = mode
    }

    fun getOnlineSongs(): LiveData<MutableList<Song>> = onlineSongs
    fun getFavouriteSongs(): LiveData<MutableList<Song>> = favouriteSongs
    fun getDeviceSongs(): LiveData<MutableList<Song>> = deviceSongs

    fun getThemeMode() = themeMode

    fun receiveDataFromReceiver(intent: Intent) {
        val bundle = intent.extras ?: return
        currentSong = bundle.getSerializable(KEY_SONG) as Song?
        isPlaying.value = bundle.getBoolean(KEY_STATUS_MUSIC)
        actionMusic.value = bundle.getInt(KEY_ACTION)
    }

    fun onClickMiniPlayer() {
        openMusicPlayer()
        if (isPlaying.value == false) {
            sendDataToService(ACTION_RESUME)
        }
        sendDataToService(ACTION_OPEN_MUSIC_PLAYER)
    }

    @SuppressLint("CommitTransaction")
    fun openMusicPlayer() {
        val musicPlayerFragment = MusicPlayerFragment()
        val fragmentActivity = context as FragmentActivity
        fragmentActivity.supportFragmentManager.popBackStack()
        val transaction = fragmentActivity.supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_right)
            .replace(R.id.layout_music_player, musicPlayerFragment)
            .addToBackStack("Backstack 1")
            .commit()
    }

    fun onClickPlayOrPause() {
        if (isPlaying.value == true) {
            sendDataToService(ACTION_PAUSE)
        } else {
            sendDataToService(ACTION_RESUME)
        }
    }

    fun sendDataToService(action: Int) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(KEY_ACTION, action)
        val bundle = Bundle()
        bundle.putSerializable(KEY_SONG, currentSong)
        intent.putExtras(bundle)
        context.startService(intent)
    }

    fun sendListSongToService(songs: MutableList<Song>) {
        val intent = Intent(context, MusicService::class.java)
        val bundle = Bundle()
        bundle.putSerializable(KEY_LIST_SONGS, songs as Serializable)
        intent.putExtras(bundle)
        context.startService(intent)
    }

    fun getPlaying(): MutableLiveData<Boolean> = isPlaying
}