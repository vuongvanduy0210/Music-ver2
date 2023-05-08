package com.vuongvanduy.music.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vuongvanduy.music.databinding.ItemPhotoBinding
import com.vuongvanduy.music.model.Photo

class PhotoViewPager2Adapter
    : RecyclerView.Adapter<PhotoViewPager2Adapter.PhotoViewHolder>() {

    private var photos: MutableList<Photo>? = null
    private lateinit var context: Context

    fun setData(list: MutableList<Photo>, context: Context) {
        this.photos = list
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        if (photos != null) {
            return photos!!.size
        }
        return 0
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos?.get(position) ?: return

        Glide.with(context).load(photo.getImageUri()).into(holder.binding.imgPhoto)
    }

    inner class PhotoViewHolder(val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)
}