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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.vuongvanduy.music.model.Category
import com.vuongvanduy.music.model.Photo
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*
import java.text.Collator
import java.util.Locale

class HomeViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private lateinit var context: Context

    private val onlineSongs: MutableLiveData<MutableList<Song>> by lazy {
        MutableLiveData<MutableList<Song>>()
    }
    private val favouriteSongs: MutableLiveData<MutableList<Song>> by lazy {
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
    lateinit var song: Song
    lateinit var categoryName: String

    fun setData(context: Context) {
        this.context = context
        getListOnlineSongs()
        getListFavouriteSongs()
    }

    fun setSong(song: Song, categoryName: String) {
        this.song = song
        this.categoryName = categoryName
    }

    fun setListPhotos() {
        getListPhotos()
    }

    fun getPhotos(): LiveData<MutableList<Photo>> = photos
    fun getOnlineSongs(): LiveData<MutableList<Song>> = onlineSongs
    fun getFavouriteSongs(): LiveData<MutableList<Song>> = favouriteSongs
    fun getDeviceSongs(): LiveData<MutableList<Song>> = deviceSongs

    private fun getListPhotos() {
        // 2 5 7 11 12
        val list = mutableListOf<Photo>()
        if (onlineSongs.value != null) {
            for (i in 0 until onlineSongs.value?.size!!) {
                when (i) {
                    1, 2, 3, 4, 5 -> {
                        val photo = onlineSongs.value!![i].getImageUri()?.let { Photo(it) }
                        if (photo != null) {
                            list.add(photo)
                        }
                    }
                }
            }
            photos.value = list
        } else if (deviceSongs.value != null) {
            for (i in 0 until deviceSongs.value?.size!!) {
                val photo = deviceSongs.value!![i].getImageUri()?.let { Photo(it) }
                if (photo != null) {
                    list.add(photo)
                }
            }
            photos.value = list
        }
    }

    fun getListCategories(): MutableList<Category> {
        getOnlineSongsShow()
        getDeviceSongsShow()
        val list = mutableListOf<Category>()
        allSongsShow.value?.let { Category("Online Songs", it) }?.let { list.add(it) }
        deviceSongsShow.value?.let { Category("Device Songs", it) }?.let { list.add(it) }
        return list
    }

    private fun getOnlineSongsShow() {
        val list = mutableListOf<Song>()
        if (onlineSongs.value != null) {
            for (i in 0 until onlineSongs.value!!.size / 3) {
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

    private fun getListOnlineSongs() {
        val list = mutableListOf<Song>()
        val database = Firebase.database
        val myRef = database.getReference("all_songs")
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    // TODO: handle the post
                    val song = postSnapshot.getValue<Song>()
                    if (song != null) {
                        if (!isSongExists(list, song)) {
                            list.add(song)
                        }
                    }
                }
                val collator = Collator.getInstance(Locale("vi"))
                list.sortWith { obj1, obj2 ->
                    collator.compare(obj1.getName()?.lowercase(), obj2.getName()?.lowercase())
                }
                onlineSongs.value = list
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message

                // ...
            }
        })
    }

    private fun isSongExists(songList: List<Song>, song: Song): Boolean {
        for (s in songList) {
            if (s.getResourceUri() == song.getResourceUri()) {
                return true
            }
        }
        return false
    }

    private fun getListFavouriteSongs() {
        val list = mutableListOf<Song>()
        if (FirebaseAuth.getInstance().currentUser == null) {
            return
        }
        val email = FirebaseAuth.getInstance().currentUser?.email?.substringBefore(".")
        val database = Firebase.database
        val myRef = email?.let { database.getReference(it).child("favourite_songs") }
        myRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    // TODO: handle the post
                    val song = postSnapshot.getValue<Song>()
                    if (song != null) {
                        if (!isSongExists(list, song)) {
                            list.add(song)
                        }
                    }
                }
                val collator = Collator.getInstance(Locale("vi"))
                list.sortWith { obj1, obj2 ->
                    collator.compare(obj1.getName()?.lowercase(), obj2.getName()?.lowercase())
                }
                favouriteSongs.value = list
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message

                // ...
            }
        })
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

            val collator = Collator.getInstance(Locale("vi"))
            list.sortWith { obj1, obj2 ->
                collator.compare(obj1.getName()?.lowercase(), obj2.getName()?.lowercase())
            }
            deviceSongs.value = list
        }
    }
}