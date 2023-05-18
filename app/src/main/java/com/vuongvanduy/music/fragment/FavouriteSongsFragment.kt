package com.vuongvanduy.music.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.adapter.ExtendSongAdapter
import com.vuongvanduy.music.databinding.FragmentFavouriteSongsBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IClickSongListener
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.DataViewModel
import com.vuongvanduy.music.viewmodel.FavouriteSongsViewModel

class FavouriteSongsFragment : Fragment() {

    private lateinit var binding: FragmentFavouriteSongsBinding

    private lateinit var activity: MainActivity

    private lateinit var songAdapter: ExtendSongAdapter

    private lateinit var viewModel: FavouriteSongsViewModel

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                playMusic(viewModel.getSong())
            } else {
                Log.e("FRAGMENT_NAME", "Permission denied")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavouriteSongsBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[FavouriteSongsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getDataFromHomeFragment()

        setRecyclerViewSong()

        setOnClickBtSearchView()

        observerDisplayKeyboard()
    }

    private fun getDataFromHomeFragment() {
        val dataViewModel: DataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
        dataViewModel.getListSongsFavourite().observe(activity) {
            if (it != null) {
                viewModel.setData(it)
            }
        }

        dataViewModel.getFavouriteSong().observe(activity) {
            if (it != null) {
                if (viewModel.getSongs().value != null
                    && viewModel.isSongExists(viewModel.getSongs().value!!, it)
                ) {
                    Toast.makeText(
                        activity,
                        "This song is exist in favourites", Toast.LENGTH_SHORT
                    ).show()
                    return@observe
                }
                viewModel.addSong(it, activity)
                Toast.makeText(
                    activity,
                    "Add song to favourites success", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setRecyclerViewSong() {
        songAdapter = ExtendSongAdapter(object : IClickSongListener {
            override fun onClickSong(song: Song) {
                viewModel.setSong(song)
                playSong(song)
            }

            override fun onClickAddFavourites(song: Song) {}

            override fun onClickRemoveFavourites(song: Song) {
                removeSong(song)
            }

        }, TITLE_FAVOURITE_SONGS)

        viewModel.getSongs().apply {
            if (value == null || value!!.isEmpty()) {
                binding.apply {
                    tvNotiListSong.apply {
                        text = EMPTY_LIST_SONG_TEXT_FAVOURITE
                        visibility = View.VISIBLE
                    }
                    rcvListSongs.visibility = View.GONE
                }
            }

            observe(activity) {
                if (value?.isNotEmpty() == true) {
                    binding.apply {
                        tvNotiListSong.visibility = View.GONE
                        rcvListSongs.visibility = View.VISIBLE
                    }
                } else {
                    binding.apply {
                        tvNotiListSong.text = EMPTY_LIST_SONG_TEXT_FAVOURITE
                        tvNotiListSong.visibility = View.VISIBLE
                        rcvListSongs.visibility = View.GONE
                    }
                }
                songAdapter.setData(it)
            }
        }

        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        binding.rcvListSongs.apply {
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(decoration)
            adapter = songAdapter
        }
    }

    private fun playSong(song: Song) {
        requestPermissionPostNotification(song)
    }

    private fun removeSong(song: Song) {
        viewModel.removeSongFromFirebase(song, activity)
        viewModel.removeSong(song)
        activity.viewModel.apply {
            if (currentListName != null && currentListName == TITLE_FAVOURITE_SONGS) {
                if (viewModel.getSongs().value != null) {
                    sendListSongToService(viewModel.getSongs().value!!)
                }
            }
        }
    }

    private fun requestPermissionPostNotification(song: Song) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                playMusic(song)
            } else {
                activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            playMusic(song)
        }
    }

    private fun playMusic(song: Song) {
        activity.apply {
            viewModel.apply {
                currentListName = TITLE_FAVOURITE_SONGS
                currentSong = song
                this@FavouriteSongsFragment.viewModel.getSongs().value?.let {
                    sendListSongToService(it)
                    sendDataToService(ACTION_START)
                }
            }
            openMusicPlayer()
            getBinding().miniPlayer.visibility = View.VISIBLE
        }
    }

    private fun setOnClickBtSearchView() {
        binding.imgClear.apply {
            setOnClickListener {
                binding.edtSearch.setText("")
            }
        }
        binding.edtSearch.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard()
                    return@setOnEditorActionListener true
                }
                false
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    songAdapter.filter.filter(s)
                }
            })
        }
    }

    private fun observerDisplayKeyboard(): ViewTreeObserver.OnGlobalLayoutListener {
        val keyboardVisibilityListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            val rootView = activity.window.decorView.rootView
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (isAdded && !isDetached) {
                val isKeyboardOpen = keyboardHeight > TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    200f,
                    resources.displayMetrics
                )

                if (isKeyboardOpen) {
                    // keyboard is open
                    activity.getBinding().apply {
                        bottomNavigation.visibility = View.GONE
                    }
                } else {
                    Looper.myLooper()?.let {
                        Handler(it).postDelayed({
                            activity.getBinding().apply {
                                if (bottomNavigation.visibility == View.GONE) {
                                    bottomNavigation.visibility = View.VISIBLE
                                }
                            }
                        }, 10)
                    }
                }
            }
        }
        val rootView = activity.window.decorView.rootView
        rootView.viewTreeObserver.addOnGlobalLayoutListener(keyboardVisibilityListener)
        return keyboardVisibilityListener
    }

    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        val rootView = activity.window.decorView.rootView
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(observerDisplayKeyboard())
    }
}