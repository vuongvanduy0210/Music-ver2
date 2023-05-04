package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.vuongvanduy.music.R
import com.vuongvanduy.music.shared_preferences.DataLocalManager
import com.vuongvanduy.music.adapter.FragmentViewPager2Adapter
import com.vuongvanduy.music.databinding.ActivityMainBinding
import com.vuongvanduy.music.databinding.LayoutHeaderNavigationBinding
import com.vuongvanduy.music.fragment.*
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.transformer.ZoomOutPageTransformer
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.DataViewModel
import com.vuongvanduy.music.viewmodel.MainViewModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var layoutHeaderBinding: LayoutHeaderNavigationBinding
    private lateinit var customButton: View

    var currentFragment: Int = 0
    private var currentFragmentViewPager2Adapter: Int = 0

    lateinit var viewModel: MainViewModel

    private var selectedMenuItem: MenuItem? = null

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                viewModel.receiveDataFromReceiver(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e(MAIN_ACTIVITY_TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val headerView = binding.navigationView.getHeaderView(0)
        layoutHeaderBinding = LayoutHeaderNavigationBinding.bind(headerView)

        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.setData(this)
        observerAction()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(serviceReceiver, IntentFilter(SEND_DATA))

        checkServiceIsRunning()

        setToolbarAndDrawerLayout()

        initUINavigationHeader()

        setViewPager2Adapter()

        setBottomNavigation()

        initListenerMiniPlayer()
    }

    @SuppressLint("SetTextI18n")
    private fun initUINavigationHeader() {
        viewModel.setUser(FirebaseAuth.getInstance().currentUser)
        viewModel.getUser().observe(this) {
            if (it != null) {
                val name = it.displayName
                val email = it.email
                val photoUrl = it.photoUrl
                layoutHeaderBinding.apply {
                    Glide.with(this@MainActivity).load(photoUrl)
                        .error(R.drawable.img_avatar_error)
                        .into(imgAvatar)
                    if (name != null) {
                        tvUserName.text = name
                    } else {
                        tvUserName.text = ""
                    }
                    tvEmail.text = email
                }
            } else {
                layoutHeaderBinding.apply {
                    Glide.with(this@MainActivity).load(R.drawable.img_avatar_error).into(imgAvatar)
                    tvUserName.text = "Guest"
                    tvEmail.text = "Someone@gmail.com"
                }
            }
        }
    }

    private fun setLayoutMiniPlayer(song: Song) {

        val imageUri = Uri.parse(song.getImageUri())
        // set layout
        binding.apply {
            Glide.with(this@MainActivity).load(imageUri).into(imgMusic)
            tvMusicName.text = song.getName()
            tvSinger.text = song.getSinger()
            Glide.with(this@MainActivity).load(imageUri).into(imgBgMiniPlayer)
        }
    }

    private fun initListenerMiniPlayer() {
        binding.apply {
            imgPlay.setOnClickListener {
                viewModel.onClickPlayOrPause()
            }
            imgPrevious.setOnClickListener {
                viewModel.sendDataToService(ACTION_PREVIOUS)
            }
            imgNext.setOnClickListener {
                viewModel.sendDataToService(ACTION_NEXT)
            }
            imgClear.setOnClickListener {
                viewModel.sendDataToService(ACTION_CLEAR)
            }

            miniPlayer.setOnClickListener {
                openMusicPlayer()
            }
        }
    }

    fun openMusicPlayer() {
        viewModel.onClickMiniPlayer()
        openMusicPlayerView()
        hideKeyboard()
    }

    private fun observerAction() {
        viewModel.getPlaying().observe(this) {
            binding.imgPlay.apply {
                if (it) {
                    setImageResource(R.drawable.ic_pause)
                } else {
                    setImageResource(R.drawable.ic_play)
                }
            }
        }
        viewModel.actionMusic.observe(this) { value ->
            when (value) {
                ACTION_START -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_NEXT -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_PREVIOUS -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_CLEAR -> {
                    viewModel.getPlaying().value = false
                    binding.apply {
                        miniPlayer.visibility = View.GONE
                        if (layoutMusicPlayer.visibility == View.VISIBLE) {
                            closeMusicPlayerView()
                        }
                    }
                }

                ACTION_RELOAD_DATA -> {
                    viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                    binding.miniPlayer.visibility = View.VISIBLE
                }

                ACTION_OPEN_MUSIC_PLAYER -> {
                    if (viewModel.getPlaying().value == true
                        && binding.contentFrame.visibility == View.VISIBLE
                    ) {
                        return@observe
                    }
                    openMusicPlayerView()
                    Log.e(MAIN_ACTIVITY_TAG, "openMusicPlayerView")
                }

                else -> {}
            }
        }
    }

    private fun setToolbarAndDrawerLayout() {
        getDataFromHomeFragment()
        binding.toolBar.title = ""
        setSupportActionBar(binding.toolBar)
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolBar,
            R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                hideKeyboard()
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        binding.navigationView.setNavigationItemSelectedListener(this)

        setOnClickButtonPlayAll()
    }

    private fun setOnClickButtonPlayAll() {
        customButton = LayoutInflater.from(this)
            .inflate(R.layout.custom_buttom, binding.toolBar, false)
        binding.toolBar.addView(customButton)
        customButton.visibility = View.GONE

        // click button play all
        customButton.setOnClickListener {
            when (currentFragmentViewPager2Adapter) {
                FRAGMENT_ONLINE_SONGS -> {
                    viewModel.getOnlineSongs().observe(this) { list ->
                        if (list != null) {
                            viewModel.apply {
                                currentSong = list[0]
                                sendListSongToService(list)
                                sendDataToService(ACTION_START)
                            }
                            binding.miniPlayer.visibility = View.VISIBLE
                            openMusicPlayer()
                        }
                    }
                }

                FRAGMENT_DEVICE_SONGS -> {
                    viewModel.getDeviceSongs().observe(this) { list ->
                        if (list != null) {
                            viewModel.apply {
                                currentSong = list[0]
                                sendListSongToService(list)
                                sendDataToService(ACTION_START)
                            }
                            binding.miniPlayer.visibility = View.VISIBLE
                            openMusicPlayer()
                        }
                    }
                }
            }
        }
    }

    private fun getDataFromHomeFragment() {
        val dataViewModel: DataViewModel = ViewModelProvider(this)[DataViewModel::class.java]
        dataViewModel.getListSongsOnline().observe(this) {
            viewModel.setOnlineSongs(it)
        }
        dataViewModel.getListSongsDevice().observe(this) {
            viewModel.setDeviceSongs(it)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        selectedMenuItem = item
        customButton.visibility = View.GONE

        when (item.itemId) {
            R.id.nav_account ->
                replaceFragment(AccountFragment(), FRAGMENT_ACCOUNT, TITLE_ACCOUNT)

            R.id.nav_appearance ->
                replaceFragment(AppearanceFragment(), FRAGMENT_APPEARANCE, TITLE_APPEARANCE)

            R.id.nav_app_info ->
                replaceFragment(AppInfoFragment(), FRAGMENT_APP_INFO, TITLE_APP_INFO)

            R.id.nav_contact ->
                replaceFragment(ContactFragment(), FRAGMENT_CONTACT, TITLE_CONTACT)

            R.id.nav_feedback ->
                replaceFragment(FeedbackFragment(), FRAGMENT_FEEDBACK, TITLE_FEEDBACK)
        }
        selectedMenuItem!!.isChecked = true
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @SuppressLint("CommitTransaction")
    fun replaceFragment(fragment: Fragment, current: Int, title: String) {
        if (currentFragment != current) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.content_frame, fragment).commit()
            currentFragment = current
            binding.toolBarTitle.text = title
            openNavigation()
        }
    }

    private fun setViewPager2Adapter() {
        val adapter = FragmentViewPager2Adapter(this)

        binding.viewPager2.apply {
            setAdapter(adapter)
            isUserInputEnabled = false
            setPageTransformer(ZoomOutPageTransformer())

            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                @SuppressLint("LongLogTag")
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.bottomNavigation.currentItem = position
                    currentFragmentViewPager2Adapter = position + 1
                    setTitleForToolbar(position, 2)
                    customButton.apply {
                        when (currentFragmentViewPager2Adapter) {
                            FRAGMENT_HOME -> {
                                visibility = View.GONE
                            }

                            FRAGMENT_ONLINE_SONGS -> {
                                visibility = View.VISIBLE
                            }

                            FRAGMENT_FAVOURITE_SONGS -> {
                                visibility = View.VISIBLE
                            }

                            FRAGMENT_DEVICE_SONGS -> {
                                visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })
        }
    }

    private fun setBottomNavigation() {
        val itemAll = AHBottomNavigationItem(
            R.string.title_online, R.drawable.ic_all, R.color.teal_200
        )
        val itemHome = AHBottomNavigationItem(
            R.string.title_home, R.drawable.ic_home, R.color.blueLight
        )
        val itemFavourite = AHBottomNavigationItem(
            R.string.title_favourite, R.drawable.ic_favourite, R.color.red
        )
        val itemDevice = AHBottomNavigationItem(
            R.string.title_device, R.drawable.ic_device, R.color.orange
        )

        val listItems = listOf(itemHome, itemAll, itemFavourite, itemDevice)
        binding.bottomNavigation.apply {
            addItems(listItems)
            isColored = true
            accentColor = Color.parseColor("#FFFFFFFF")
            inactiveColor = Color.parseColor("#120433")

            setOnTabSelectedListener { position, _ ->
                binding.viewPager2.currentItem = position
                return@setOnTabSelectedListener true
            }
        }
    }

    @SuppressLint("ResourceType")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        binding.apply {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) { // khi drawer đang mở
                drawerLayout.closeDrawer(GravityCompat.START)
            } else if (binding.layoutMusicPlayer.visibility == View.VISIBLE) { // khi music player đang mở
                closeMusicPlayerView()
                val fragment = supportFragmentManager.findFragmentById(R.id.layout_music_player)
                if (fragment != null) {
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                    supportFragmentManager.popBackStack()
                }
            } else if (mainUi.visibility == View.GONE && contentFrame.visibility == View.VISIBLE) {
                // khi main đang ẩn và content frame đang mở
                if (currentFragment == FRAGMENT_CHANGE_PASSWORD) {
                    supportFragmentManager.popBackStack()
                    currentFragment = FRAGMENT_ACCOUNT
                } else {
                    currentFragment = 0
                    setTitleForToolbar(0, 1)
                    if (selectedMenuItem == null) {
                        selectedMenuItem = binding.navigationView.menu.findItem(R.id.nav_appearance)
                    }
                    selectedMenuItem!!.isChecked = false
                    closeNavigation()
                    if (currentFragmentViewPager2Adapter != FRAGMENT_HOME) {
                        customButton.visibility = View.VISIBLE
                    }
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun setTitleForToolbar(position: Int, mode: Int) {
        binding.toolBarTitle.text = if (mode == 1) {
            when (currentFragmentViewPager2Adapter) {
                FRAGMENT_HOME -> TITLE_HOME
                FRAGMENT_ONLINE_SONGS -> TITLE_ONLINE_SONGS
                FRAGMENT_FAVOURITE_SONGS -> TITLE_FAVOURITE_SONGS
                else -> TITLE_DEVICE_SONGS
            }
        } else {
            when (position) {
                0 -> TITLE_HOME
                1 -> TITLE_ONLINE_SONGS
                2 -> TITLE_FAVOURITE_SONGS
                else -> TITLE_DEVICE_SONGS
            }
        }
    }

    private fun openMusicPlayerView() {
        binding.apply {
            layoutMusicPlayer.visibility = View.VISIBLE
            mainUi.visibility = View.GONE
            appBar.visibility = View.GONE
        }
    }

    fun closeMusicPlayerView() {
        binding.apply {
            Log.e(MAIN_ACTIVITY_TAG, "closeMusicPlayerView")
            layoutMusicPlayer.visibility = View.GONE
            mainUi.visibility = View.VISIBLE
            appBar.visibility = View.VISIBLE
        }
    }

    fun openNavigation() {
        Log.e(MAIN_ACTIVITY_TAG, "openNavigation")
        binding.apply {
            contentFrame.visibility = View.VISIBLE
            mainUi.visibility = View.GONE
            appBar.visibility = View.VISIBLE
            layoutMusicPlayer.visibility = View.GONE
        }
    }

    private fun closeNavigation() {
        Log.e(MAIN_ACTIVITY_TAG, "closeNavigation")
        binding.apply {
            contentFrame.visibility = View.GONE
            mainUi.visibility = View.VISIBLE
            appBar.visibility = View.VISIBLE
        }
    }

    fun getBinding() = this.binding

    private fun checkServiceIsRunning() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
        for (serviceInfo in runningServices) {
            if (serviceInfo.service.className
                == "com.vuongvanduy.music.service.MusicService"
            ) {
                viewModel.sendDataToService(ACTION_RELOAD_DATA)
                break
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(MAIN_ACTIVITY_TAG, "onResume")
        viewModel.setUser(FirebaseAuth.getInstance().currentUser)
    }

    override fun onPause() {
        super.onPause()
        Log.e(MAIN_ACTIVITY_TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(MAIN_ACTIVITY_TAG, "onDestroy")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver)
        viewModel.getThemeMode()?.let { DataLocalManager.putStringThemeMode(it) }
    }

    override fun recreate() {
        Log.e(MAIN_ACTIVITY_TAG, "recreate")
        super.recreate()
    }
}