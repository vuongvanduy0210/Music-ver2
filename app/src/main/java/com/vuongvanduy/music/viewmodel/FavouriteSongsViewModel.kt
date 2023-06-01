package com.vuongvanduy.music.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.TITLE_FAVOURITE_SONGS

class FavouriteSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private lateinit var song: Song

    fun setData(list: MutableList<Song>) {
        this.songs.value = list
    }

    fun setSong(song: Song) {
        this.song = song
    }

    fun getSongs(): LiveData<MutableList<Song>> = songs

    fun addSong(song: Song, activity: MainActivity) {
        FirebaseAuth.getInstance().currentUser?.email?.let {
            pushSongToFirebase(
                it,
                song,
                activity
            )
        }
    }

    fun isSongExists(songList: List<Song>, song: Song): Boolean {
        for (s in songList) {
            if (s.getResourceUri() == song.getResourceUri()) {
                return true
            }
        }
        return false
    }

    private fun pushSongToFirebase(email: String, song: Song, activity: MainActivity) {
        val database = Firebase.database
        val myRef = database.getReference("favourite_songs")
            .child(email.substringBefore("."))
        myRef.child(song.getName()!!).setValue(song).addOnCompleteListener {
            Log.e("MainActivity", "Add all song success")
            activity.viewModel.apply {
                if (currentListName != null && currentListName == TITLE_FAVOURITE_SONGS) {
                    if (songs.value != null) {
                        sendListSongToService(songs.value!!)
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "Add all song fail")
        }
    }

    fun removeSongFromFirebase(song: Song, context: Context) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return
        }
        val email = FirebaseAuth.getInstance().currentUser?.email?.substringBefore(".")
        val database = Firebase.database
        val myRef = song.getName()?.let {
            email?.let { it1 ->
                database.getReference("favourite_songs")
                    .child(it1).child(it)
            }
        }
        myRef?.removeValue { _, _ ->
            Toast.makeText(
                context,
                "Remove song success",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun removeSong(song: Song) {
        songs.value!!.remove(song)
    }

    fun getSong() = song
}