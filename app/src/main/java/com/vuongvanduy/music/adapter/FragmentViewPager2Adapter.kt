package com.vuongvanduy.music.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.vuongvanduy.music.fragment.*

class FragmentViewPager2Adapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> OnlineSongsFragment()
            2 -> FavouriteSongsFragment()
            else -> DeviceSongsFragment()
        }
    }
}