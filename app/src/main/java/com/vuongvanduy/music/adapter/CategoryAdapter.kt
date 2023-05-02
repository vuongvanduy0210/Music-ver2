package com.vuongvanduy.music.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vuongvanduy.music.databinding.ItemCategoryBinding
import com.vuongvanduy.music.model.Category
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickCategoryListener
import com.vuongvanduy.music.my_interface.IClickItemSongCategoryListener

class CategoryAdapter(
    private val listCategories: MutableList<Category>,
    private val context: Context,
    private val iClickCategoryListener: IClickCategoryListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listCategories.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = listCategories[position]

        holder.binding.apply {
            tvNameCategory.text = category.getName()

            val songCategoryAdapter = SongCategoryAdapter(category.getSongs(),
                object : IClickItemSongCategoryListener {
                    override fun onClickItemSong(song: Song) {
                        iClickCategoryListener.onClickSong(song, category.getName())
                    }
                })

            val manger = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            holder.binding.rcvSong.apply {
                adapter = songCategoryAdapter
                layoutManager = manger
                btViewAlls.setOnClickListener {
                    iClickCategoryListener.clickButtonViewAll(category.getName())
                }
                tvNameCategory.setOnClickListener {
                    iClickCategoryListener.clickButtonViewAll(category.getName())
                }
            }
        }
    }

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {}
}