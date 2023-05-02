package com.vuongvanduy.music.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vuongvanduy.music.databinding.ItemSongInCategoryBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickItemSongCategoryListener

class SongCategoryAdapter(
    private var listSongShow: MutableList<Song>,
    private val listener: IClickItemSongCategoryListener
) : RecyclerView.Adapter<SongCategoryAdapter.SongCategoryViewHolder>() {

    private var listSongPlay: MutableList<Song>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: MutableList<Song>) {
        this.listSongPlay = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongCategoryViewHolder {
        val binding = ItemSongInCategoryBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return SongCategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listSongShow.size

    }

    override fun onBindViewHolder(holder: SongCategoryViewHolder, position: Int) {
        val song = listSongShow[position]
        val imageUri = Uri.parse(song.getImageUri())
        holder.binding.apply {
            Glide.with(this.root).load(imageUri).into(imgSong)
            tvNameSong.text = song.getName()
            tvSinger.text = song.getSinger()
            layoutItemSong.setOnClickListener {
                listener.onClickItemSong(song)
            }
        }
    }

    inner class SongCategoryViewHolder(val binding: ItemSongInCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {}
}