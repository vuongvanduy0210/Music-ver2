package com.vuongvanduy.music.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.vuongvanduy.music.R
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.adapter.CategoryAdapter
import com.vuongvanduy.music.adapter.PhotoViewPager2Adapter
import com.vuongvanduy.music.databinding.FragmentHomeBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickCategoryListener
import com.vuongvanduy.music.transformer.DepthPageTransformer
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.DataViewModel
import com.vuongvanduy.music.viewmodel.HomeViewModel


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var activity: MainActivity

    private lateinit var viewModel: HomeViewModel

    private lateinit var photosAdapter: PhotoViewPager2Adapter

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.getLocalMusic()
                Toast.makeText(
                    requireContext(),
                    "Get music from your phone success",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.e("FRAGMENT_NAME", "Permission denied")
            }
        }

    private var myHandler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        binding.slideImage.apply {
            val count = viewModel.getPhotos().value?.size
            if (count != null) {
                if (currentItem == count - 1) {
                    currentItem = 0
                } else {
                    currentItem += 1
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        viewModel.setData(activity)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissionReadStorage()

        sendDataToFragment()

        setAutoSlideImage()
    }

    /*private fun pushListSong() {
        viewModel.getOnlineSongs().observe(activity) {
            val database = Firebase.database
            val myRef = database.getReference("all_songs")
            myRef.setValue(it).addOnCompleteListener {
                Log.e(MAIN_ACTIVITY_TAG, "Add all song success")
            }.addOnFailureListener {
                Log.e(MAIN_ACTIVITY_TAG, "Add all song fail")
            }
        }
    }*/

    private fun requestPermissionReadStorage() {
        Looper.myLooper()?.let {
            Handler(it).postDelayed({
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    viewModel.getLocalMusic()
                    return@postDelayed
                }
                if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // get list song from device and send to music device fragment
                    viewModel.getLocalMusic()
                } else {
                    activityResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }, 500)
        }
    }

    private fun sendDataToFragment() {
        val dataViewModel: DataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
        viewModel.apply {
            getOnlineSongs().observe(activity) {
                dataViewModel.setListSongsOnline(it)
                setListPhotos()
                setRecyclerViewCategory()
            }
            getFavouriteSongs().observe(activity) {
                dataViewModel.setListSongsFavourite(it)
            }
            getDeviceSongs().observe(activity) {
                dataViewModel.setListSongsDevice(it)
                setListPhotos()
                setRecyclerViewCategory()
            }
        }

        dataViewModel.getUser().observe(activity) {
            if (it != null) {
                viewModel.setData(activity)
            }
        }
    }

    private fun setRecyclerViewCategory() {
        val categoryAdapter = CategoryAdapter(
            viewModel.getListCategories(),
            activity,
            object : IClickCategoryListener {
                override fun clickButtonViewAll(categoryName: String) {
                    gotoViewAll(categoryName)
                }

                override fun onClickSong(song: Song, categoryName: String) {
                    playSong(song, categoryName)
                }
            })
        binding.rcvCategory.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = categoryAdapter
        }
    }

    private fun gotoViewAll(categoryName: String) {
        if (categoryName == "Online Songs") {
            activity.getBinding().viewPager2.currentItem = 1
        } else if (categoryName == "Device Songs") {
            activity.getBinding().viewPager2.currentItem = 3
        }
    }

    private fun playSong(song: Song, categoryName: String) {
        activity.apply {
            viewModel.apply {
                currentSong = song
                if (categoryName == "Online Songs") {
                    currentListName = TITLE_ONLINE_SONGS
                    getOnlineSongs().observe(activity) {
                        sendListSongToService(it)
                    }
                } else if (categoryName == "Device Songs") {
                    currentListName = TITLE_DEVICE_SONGS
                    getDeviceSongs().observe(activity) {
                        sendListSongToService(it)
                    }
                }
                sendDataToService(ACTION_START)
            }
            openMusicPlayer()
            getBinding().miniPlayer.visibility = View.VISIBLE
        }
    }

    private fun setAutoSlideImage() {
        Glide.with(activity).load(R.drawable.img_home).into(binding.imgBackGround)
        viewModel.getPhotos().observe(activity) {
            if (it != null && it.isNotEmpty()) {
                binding.slideImage.visibility = View.VISIBLE
                photosAdapter = PhotoViewPager2Adapter()
                photosAdapter.setData(it, activity)
                binding.slideImage.apply {
                    adapter = photosAdapter
                    setPageTransformer(DepthPageTransformer())
                    binding.circleIndicator.setViewPager(this)
                    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            myHandler.removeCallbacks(runnable)
                            myHandler.postDelayed(runnable, 2500)
                        }
                    })
                }
            } else {
                binding.slideImage.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        myHandler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        myHandler.postDelayed(runnable, 2500)
    }
}