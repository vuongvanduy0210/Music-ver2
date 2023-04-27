package com.vuongvanduy.music.viewmodel

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.vuongvanduy.music.model.Category
import com.vuongvanduy.music.model.Photo
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*

class HomeViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private lateinit var context: Context

    private val onlineSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }
    private val deviceSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }
    private val photos: MutableLiveData<MutableList<Photo>> by lazy {
        MutableLiveData<MutableList<Photo>>()
    }
    private val allSongsShow: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }
    private val deviceSongsShow: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }

    private val listSongsPath = mutableListOf<String>()
    private val listImagesPath = mutableListOf<String>()

    fun setData(context: Context) {
        this.context = context
        getListOnlineSongs()
    }

    fun setListPhotos() {
        getListPhotos()
    }

    fun getPhotos(): LiveData<MutableList<Photo>> = photos
    fun getOnlineSongs(): LiveData<MutableList<Song>> = onlineSongs
    fun getDeviceSongs(): LiveData<MutableList<Song>> = deviceSongs

    private fun getListPhotos() {
        // 2 5 7 11 12
        val list = mutableListOf<Photo>()
        if (onlineSongs.value != null) {
            for (i in 0 until onlineSongs.value?.size!!) {
                when(i) {
                    2,6,8,11,12 -> {
                        val photo = Photo(onlineSongs.value!![i].getImageUri())
                        list.add(photo)
                    }
                }
            }
            photos.value = list
        }
    }

    fun getListCategories(): MutableList<Category> {
        getAllSongsShow()
        getDeviceSongsShow()
        val list = mutableListOf<Category>()
        allSongsShow.value?.let { Category("Online Songs", it) }?.let { list.add(it) }
        deviceSongsShow.value?.let { Category("Device Songs", it) }?.let { list.add(it) }
        return list
    }

    private fun getAllSongsShow() {
        val list = mutableListOf<Song>()
        if (onlineSongs.value != null) {
            for (i in 0 until onlineSongs.value!!.size - 10) {
                val song = onlineSongs.value!![i]
                list.add(song)
            }
            allSongsShow.value = list
        }
    }

    private fun getDeviceSongsShow() {
        val list = mutableListOf<Song>()
        if (deviceSongs.value != null) {
            for (i in 0 until deviceSongs.value!!.size) {
                val song = deviceSongs.value!![i]
                list.add(song)
            }
            deviceSongsShow.value = list
        }
    }

    @SuppressLint("Recycle")
    fun getLocalMusic() {
        val list = mutableListOf<Song>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(
            uri,
            null, null,
            null, null
        )

        if (cursor == null) {
            Toast.makeText(context, "Something Went Wrong.", Toast.LENGTH_SHORT).show()
        } else if (!cursor.moveToFirst()) {
            Log.e(DEVICE_SONGS_FRAGMENT_TAG, "No Music Found on Device")
        } else {
            //get columns
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            do {
                val resourceId = cursor.getLong(idColumn)
                val name = cursor.getString(titleColumn)
                val singer = cursor.getString(artistColumn)
                val imageId = cursor.getLong(albumIdColumn)

                val resourceUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, resourceId
                ).toString()
                val albumArtUri = Uri
                    .parse("content://media/external/audio/albumart/$imageId")
                    .toString()
                val song = Song(name, singer, resourceUri, albumArtUri)
                list.add(song)
            } while (cursor.moveToNext())

            list.sortBy { it.getName().lowercase() }
            deviceSongs.value = list
        }
    }

    private fun getListOnlineSongs() {
        val storageRef = Firebase.storage("gs://music-8f17d.appspot.com/").reference
        val musicRef = storageRef.child("music")
        musicRef.listAll().addOnSuccessListener {
            val items = it.items
            if (items.isEmpty()) {
                Log.e(ONLINE_SONGS_FRAGMENT_TAG, "List Song Firebase is empty")
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
                    onlineSongs.value = list
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
                    onlineSongs.value = list
                }
            }
        }
    }
}