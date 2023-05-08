package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.ViewModel
import com.vuongvanduy.music.R
import com.vuongvanduy.music.model.Contact

class ContactViewModel : ViewModel() {

    private lateinit var listContact: MutableList<Contact>

    fun setData() {
        this.listContact = getListContact()
    }

    fun getData() = listContact

    private fun getListContact(): MutableList<Contact> {
        val list = mutableListOf<Contact>()

        list.add(Contact(R.drawable.img_fb, "Facebook", "https://www.facebook.com/vuongduy03"))
        list.add(Contact(R.drawable.img_zalo, "Zalo", "https://zalo.me/0987786011"))
        list.add(Contact(R.drawable.img_gmail, "Gmail", null))
        list.add(Contact(R.drawable.img_microsoft, "Microsoft", null))

        return list
    }
}