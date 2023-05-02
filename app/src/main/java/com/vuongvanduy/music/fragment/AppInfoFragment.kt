package com.vuongvanduy.music.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vuongvanduy.music.databinding.FragmentAppInfoBinding

class AppInfoFragment : Fragment() {

    private lateinit var binding: FragmentAppInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAppInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

}