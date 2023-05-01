package com.vuongvanduy.music.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.get
import androidx.fragment.app.FragmentActivity
import com.vuongvanduy.music.R
import com.vuongvanduy.music.SharedPreferences.DataLocalManager
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.adapter.ThemeModeAdapter
import com.vuongvanduy.music.databinding.FragmentAppearanceBinding
import com.vuongvanduy.music.model.ThemeMode
import com.vuongvanduy.music.util.DARK_MODE
import com.vuongvanduy.music.util.LIGHT_MODE
import com.vuongvanduy.music.util.SYSTEM_MODE


class AppearanceFragment : Fragment() {

    private lateinit var binding: FragmentAppearanceBinding

    private lateinit var themeModeAdapter: ThemeModeAdapter

    private lateinit var activity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("AppearanceFragment", "onCreateView")
        // Inflate the layout for this fragment
        binding = FragmentAppearanceBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        themeModeAdapter = ThemeModeAdapter(requireActivity(), R.layout.item_selected, getListThemeMode())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity.openNavigation()
        setThemeModeAdapter()
    }

    private fun setSelectionSpinner() {
        when(AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ->
                binding.themeSpinner.setSelection(0)
            AppCompatDelegate.MODE_NIGHT_NO ->
                binding.themeSpinner.setSelection(1)
            AppCompatDelegate.MODE_NIGHT_YES ->
                binding.themeSpinner.setSelection(2)
        }
    }

    private fun setThemeModeAdapter() {
        binding.themeSpinner.apply {
            adapter = themeModeAdapter
            setSelectionSpinner()
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    changeThemeMode(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun changeThemeMode(position: Int) {
//        activity.closeMusicPlayerView()
        when(position) {
            0 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.viewModel.setThemeMode(SYSTEM_MODE)
            }
            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.viewModel.setThemeMode(LIGHT_MODE)
            }
            2 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.viewModel.setThemeMode(DARK_MODE)
            }
        }

        binding.themeSpinner.setSelection(position)
    }

    private fun getListThemeMode(): MutableList<ThemeMode> {
        val list = mutableListOf<ThemeMode>()
        list.add(ThemeMode(SYSTEM_MODE))
        list.add(ThemeMode(LIGHT_MODE))
        list.add(ThemeMode(DARK_MODE))
        return list
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("AppearanceFragment", "onDestroy")
    }
}