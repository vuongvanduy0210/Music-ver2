package com.vuongvanduy.music.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vuongvanduy.music.databinding.ItemExtendSongBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickSongListener
import com.vuongvanduy.music.util.*

class ExtendSongAdapter(
    private val iClickSongListener: IClickSongListener,
    private val name: String
) : RecyclerView.Adapter<ExtendSongAdapter.SongViewHolder>(), Filterable {

    private var songs: List<Song>? = null
    private var listSongsOld: List<Song>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<Song>) {
        this.songs = list
        this.listSongsOld = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemExtendSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
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
                if (name == TITLE_FAVOURITE_SONGS) {
                    holder.binding.tvAction.text = TEXT_REMOVE_FAVOURITES
                } else {
                    holder.binding.tvAction.text = TEXT_ADD_FAVOURITES
                }

                layoutAddFavourites.setOnClickListener {
                    if (name == TITLE_FAVOURITE_SONGS) {
                        iClickSongListener.onClickRemoveFavourites(song)
                    } else {
                        iClickSongListener.onClickAddFavourites(song)
                    }
                    holder.binding.layoutItemOnlineSong.close(true)
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
                        if (it.getName()?.lowercase()?.contains(strSearch.lowercase()) == true) {
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

    inner class SongViewHolder(val binding: ItemExtendSongBinding) :
        RecyclerView.ViewHolder(binding.root)

}