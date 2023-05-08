package com.vuongvanduy.music.model

class Contact(private val imageResource: Int, private val name: String, private val url: String?) {

    fun getImageResource() = imageResource
    fun getName() = name
    fun getUrl() = url
}