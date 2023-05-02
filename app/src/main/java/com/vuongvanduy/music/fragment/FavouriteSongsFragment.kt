package com.vuongvanduy.music.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vuongvanduy.music.R
import com.vuongvanduy.music.util.*

class FavouriteSongsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favourite_songs, container, false)
    }

    @SuppressLint("CommitTransaction")
    override fun onResume() {
        super.onResume()
    }

}