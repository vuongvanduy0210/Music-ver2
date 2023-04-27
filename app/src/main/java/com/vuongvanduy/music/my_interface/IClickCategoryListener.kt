package com.vuongvanduy.music.my_interface

import com.vuongvanduy.music.model.Song

interface IClickCategoryListener {

    fun clickButtonViewAll(categoryName: String)
    fun onClickSong(song: Song, categoryName: String)
}