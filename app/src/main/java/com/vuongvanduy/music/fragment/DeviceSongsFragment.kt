package com.vuongvanduy.music.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.adapter.SongAdapter
import com.vuongvanduy.music.databinding.FragmentAllSongsBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.my_interface.IOnClickSongListener
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.DataViewModel
import com.vuongvanduy.music.viewmodel.DeviceSongsViewModel

class DeviceSongsFragment : Fragment() {

    private lateinit var binding: FragmentAllSongsBinding

    private lateinit var activity: MainActivity

    private lateinit var songAdapter: SongAdapter

    private lateinit var viewModel: DeviceSongsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllSongsBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[DeviceSongsViewModel::class.java]
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
        dataViewModel.getListSongsDevice().value?.let { viewModel.setData(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun setRecyclerViewSong() {
        songAdapter = SongAdapter(object: IOnClickSongListener {
            override fun onClickSong(song: Song) {
                playSong(song)
            }
        })
        viewModel.getSongs().apply {
            if (value == null || value!!.isEmpty()) {
                binding.apply {
                    tvNotiListSong.apply {
                        text = EMPTY_LIST_SONG_TEXT_DEVICE
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
        activity.apply {
            viewModel.apply {
                currentSong = song
                this@DeviceSongsFragment.viewModel.getSongs().value?.let {
                    sendListSongToService(it)
                    sendDataToService(ACTION_START)
                }
            }
            openMusicPlayer()
            getBinding().apply {
                miniPlayer.visibility = View.VISIBLE
            }
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

            addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) { songAdapter.filter.filter(s) }
            })
        }
    }

    private fun observerDisplayKeyboard(): ViewTreeObserver.OnGlobalLayoutListener {
        // Tạo một ViewTreeObserver.OnGlobalLayoutListener
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
                    // Xử lý sự kiện mở bàn phím ảo
                    activity.getBinding().apply {

                        bottomNavigation.visibility = View.GONE
                    }
                } else {
                    // Xử lý sự kiện đóng bàn phím ảo
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
            // Nếu chiều cao của bàn phím > 200dp, xem như bàn phím đang mở
        }
        // Đăng ký ViewTreeObserver.OnGlobalLayoutListener với rootView
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