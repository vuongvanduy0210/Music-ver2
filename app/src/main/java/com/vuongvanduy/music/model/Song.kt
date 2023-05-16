package com.vuongvanduy.music.model

import java.io.Serializable

data class Song(private val name: String? = null,
                private val singer: String? = null,
                private val resourceUri: String? = null,
                private var imageUri: String? = null) : Serializable {
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
