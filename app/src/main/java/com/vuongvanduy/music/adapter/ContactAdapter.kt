package com.vuongvanduy.music.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vuongvanduy.music.databinding.ItemContactBinding
import com.vuongvanduy.music.model.Contact
import com.vuongvanduy.music.my_interface.IClickContactListener

class ContactAdapter(private val list: MutableList<Contact>, private val listener: IClickContactListener)
    : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size


    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = list[position]

        Glide.with(holder.binding.root)
            .load(contact.getImageResource()).into(holder.binding.imgLogo)
        holder.binding.tvName.text = contact.getName()
        holder.binding.layoutItemContact.setOnClickListener {
            listener.onClickContact(contact.getName(), contact.getUrl())
        }
    }

    inner class ContactViewHolder(val binding: ItemContactBinding)
        : RecyclerView.ViewHolder(binding.root)

}