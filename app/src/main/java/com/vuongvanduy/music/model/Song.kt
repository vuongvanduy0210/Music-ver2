package com.vuongvanduy.music.model

import java.io.Serializable

data class Song(private val name: String,
                private val singer: String,
                private val resourceUri: String,
                private var imageUri: String) : Serializable {
    fun getName() = name
    fun getSinger() = singer
    fun getResourceUri() = resourceUri
    fun getImageUri() = imageUri

    override fun toString(): String {
        return "Song{ name = $name, " +
                "singer = $singer, " +
                "resourceUri = $resourceUri, " +
                "imageUri = $imageUri"
    }
}
