package com.vuongvanduy.music.model

class Category(private val name: String, private val listSongs: MutableList<Song>) {

    fun getName() = name
    fun getSongs() = listSongs
}