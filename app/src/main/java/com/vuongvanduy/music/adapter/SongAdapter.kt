package com.vuongvanduy.music.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vuongvanduy.music.databinding.ItemSongBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickSongListener

class SongAdapter(private val iClickSongListener: IClickSongListener) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>(), Filterable {

    private var songs: List<Song>? = null
    private var listSongsOld: List<Song>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<Song>) {
        this.songs = list
        this.listSongsOld = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding: ItemSongBinding =
            ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (songs != null && songs!!.isNotEmpty()) {
            songs!!.size
        } else 0
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        if (songs?.isEmpty() == true) {
            return
        }
        val song = songs?.get(position)
        if (song != null) {
            holder.binding.apply {
                Glide.with(holder.binding.root)
                    .load(Uri.parse(song.getImageUri()))
                    .into(imgMusicInList)
                tvMusicNameInList.text = song.getName()
                tvSingerInList.text = song.getSinger()
                layoutItem.setOnClickListener {
                    iClickSongListener.onClickSong(song)
                }
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val strSearch = constraint.toString()
                songs = if (strSearch.isEmpty()) {
                    listSongsOld
                } else {
                    val list = ArrayList<Song>()
                    listSongsOld?.forEach {
                        if (it.getName().lowercase().contains(strSearch.lowercase())) {
                            list.add(it)
                        }
                    }
                    list
                }

                val filterResult = FilterResults()
                filterResult.values = songs
                return filterResult
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values != null) {
                    songs = results.values as List<Song>
                }
                notifyDataSetChanged()
            }
        }
    }

    inner class SongViewHolder(val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root)

}