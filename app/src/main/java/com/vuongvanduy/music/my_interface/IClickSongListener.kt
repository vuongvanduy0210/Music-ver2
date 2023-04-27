package com.vuongvanduy.music.my_interface

import com.vuongvanduy.music.model.Song

interface IClickSongListener {

    fun onClickSong(song: Song)
}