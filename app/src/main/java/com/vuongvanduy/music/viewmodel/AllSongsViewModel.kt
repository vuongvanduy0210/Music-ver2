package com.vuongvanduy.music.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*


class AllSongsViewModel : ViewModel() {

    private val songs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val listSongsPath = mutableListOf<String>()
    private val listImagesPath = mutableListOf<String>()

    fun setData() {
        getListSongs()
    }

    fun getSongs(): LiveData<MutableList<Song>> = songs

    private fun getListSongs() {
        val storageRef = Firebase.storage("gs://music-8f17d.appspot.com/").reference
        val musicRef = storageRef.child("music")
        musicRef.listAll().addOnSuccessListener {
            val items = it.items
            if (items.isEmpty()) {
                Log.e(ALL_SONGS_FRAGMENT_TAG, "List Song Firebase is empty")
                return@addOnSuccessListener
            }
            items.forEach {result ->
                val strUri = result.toString()
                if (strUri.contains("img")) {
                    listImagesPath.add(strUri)
                } else {
                    listSongsPath.add(strUri)
                }
            }
        }.addOnCompleteListener {
            getListSongUriFromFirebase()
        }
    }

    private fun getListSongUriFromFirebase() {

        val list = mutableListOf<Song>()
        for (i in 0 until listSongsPath.size) {
            val songReference = Firebase.storage.getReferenceFromUrl(listSongsPath[i])
            val imageReference = Firebase.storage.getReferenceFromUrl(listImagesPath[i])
            var song: Song
            var songUri: Uri? = null
            var imageUri: Uri? = null
            songReference.downloadUrl.addOnSuccessListener {
                songUri = it
                if (songUri != null && imageUri != null) {
                    val uriString = it.toString()
                    val fileName = Uri.parse(uriString)
                        .lastPathSegment?.
                        substringAfter("/")?.
                        substringBefore(".")?.
                        replace("_", " ")
                        .toString()
                    val songName = fileName.substringBefore("-")
                    val singer = fileName.substringAfter("-")
                    song = Song(songName, singer, songUri.toString(), imageUri.toString())
                    list.add(song)
                }
            }.addOnSuccessListener {
                if (songUri != null && imageUri != null) {
                    list.sortBy {song ->
                        song.getName().lowercase()
                    }
                    songs.value = list
                }
            }

            imageReference.downloadUrl.addOnSuccessListener {
                imageUri = it
                if (songUri != null && imageUri != null) {
                    val uriString = it.toString()
                    val fileName = Uri.parse(uriString)
                        .lastPathSegment?.
                        substringAfter("img_")?.
                        substringBefore(".")?.
                        replace("_", " ")
                        .toString()
                    val songName = fileName.substringBefore("-")
                    val singer = fileName.substringAfter("-")
                    song = Song(songName, singer, songUri.toString(), imageUri.toString())
                    list.add(song)
                }
            }.addOnSuccessListener {
                if (songUri != null && imageUri != null) {
                    list.sortBy {song ->
                        song.getName().lowercase()
                    }
                    songs.value = list
                }
            }
        }
    }
}