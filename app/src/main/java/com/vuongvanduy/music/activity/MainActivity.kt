package com.vuongvanduy.music.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    var currentFragment: Int = 0
    private var currentFragmentViewPager2Adapter: Int = 0

    lateinit var viewModel: MainViewModel

    private lateinit var dataViewModel: DataViewModel

    private var selectedMenuItem: MenuItem? = null

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                viewModel.receiveDataFromReceiver(intent)
            }
        }
    }

    private val currentTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                viewModel.receiveCurrentTime(intent)

            }
        }

    }

    private val handler = Looper.myLooper()

    inner class UpdateSeekBar : Runnable {
        override fun run() {
            val currentTime = viewModel.currentTime
            Log.e(MAIN_ACTIVITY_TAG, viewModel.currentTime.toString())
            binding.progressBar.progress = currentTime
            binding.progressBar.isEnabled = false
            Log.e(MAIN_ACTIVITY_TAG, binding.progressBar.progress.toString())

            handler.let {
                if (it != null) {
                    Handler(it).postDelayed(this, 1000)
                }
            }
        }
    }

    private val activityResultLauncherNotification =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                playMusic()
            } else {
                Log.e("FRAGMENT_NAME", "Permission denied")
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
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(currentTimeReceiver, IntentFilter(SEND_CURRENT_TIME))

        checkServiceIsRunning()

        setToolbarAndDrawerLayout()

        initUINavigationHeader()

        setViewPager2Adapter()

        setBottomNavigation()

        initListenerMiniPlayer()

        onBackPressCallBack()
    }

    private fun observerAction() {
        viewModel.apply {
            getPlaying().observe(this@MainActivity) {
                binding.imgPlay.apply {
                    if (it) {
                        setImageResource(R.drawable.ic_pause)
                    } else {
                        setImageResource(R.drawable.ic_play)
                    }
                }
            }
            actionMusic.observe(this@MainActivity) { value ->
                when (value) {
                    ACTION_START, ACTION_NEXT, ACTION_PREVIOUS -> viewModel.currentSong?.let {
                        setLayoutMiniPlayer(it)
                    }

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
                    }

                    else -> {}
                }
            }
            finalTime.observe(this@MainActivity) {
                val final = viewModel.finalTime.value
                if (final != null) {
                    binding.progressBar.max = final
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

        val updateSeekBar = UpdateSeekBar()
        handler.let {
            if (it != null) {
                Handler(it).post(updateSeekBar)
            }
        }
    }

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

    private fun getDataFromHomeFragment() {
        dataViewModel = ViewModelProvider(this)[DataViewModel::class.java]
        dataViewModel.getListSongsOnline().observe(this) {
            viewModel.setOnlineSongs(it)
        }
        dataViewModel.getListSongsFavourite().observe(this) {
            viewModel.setFavouriteSongs(it)
        }
        dataViewModel.getListSongsDevice().observe(this) {
            viewModel.setDeviceSongs(it)
        }
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

    private fun setOnClickButtonPlayAll() {
        customButton = LayoutInflater.from(this)
            .inflate(R.layout.custom_buttom, binding.toolBar, false)
        binding.toolBar.addView(customButton)
        customButton.visibility = View.GONE

        // click button play all
        customButton.setOnClickListener {
            requestPermissionPostNotification()
        }
    }

    private fun requestPermissionPostNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                playMusic()
            } else {
                activityResultLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            playMusic()
        }
    }

    private fun playMusic() {
        when (currentFragmentViewPager2Adapter) {
            FRAGMENT_ONLINE_SONGS -> {
                viewModel.apply {
                    currentListName = TITLE_ONLINE_SONGS
                    getOnlineSongs().apply {
                        if (value != null) {
                            currentSong = value!![0]
                            sendListSongToService(value!!)
                            sendDataToService(ACTION_START)
                            binding.miniPlayer.visibility = View.VISIBLE
                            this@MainActivity.openMusicPlayer()
                        }
                    }
                }
            }

            FRAGMENT_FAVOURITE_SONGS -> {
                viewModel.apply {
                    currentListName = TITLE_FAVOURITE_SONGS
                    getFavouriteSongs().apply {
                        if (value != null) {
                            currentSong = value!![0]
                            sendListSongToService(value!!)
                            sendDataToService(ACTION_START)
                            binding.miniPlayer.visibility = View.VISIBLE
                            this@MainActivity.openMusicPlayer()
                        }
                    }
                }
            }

            FRAGMENT_DEVICE_SONGS -> {
                viewModel.apply {
                    currentListName = TITLE_DEVICE_SONGS
                    getDeviceSongs().apply {
                        if (value != null) {
                            currentSong = value!![0]
                            sendListSongToService(value!!)
                            sendDataToService(ACTION_START)
                            binding.miniPlayer.visibility = View.VISIBLE
                            this@MainActivity.openMusicPlayer()
                        }
                    }
                }
            }
        }
    }

    fun openMusicPlayer() {
        viewModel.onClickMiniPlayer()
        openMusicPlayerView()
        hideKeyboard()
    }

    @SuppressLint("SetTextI18n")
    private fun initUINavigationHeader() {
        viewModel.setUser(FirebaseAuth.getInstance().currentUser)
        viewModel.getUser().observe(this) {
            if (it != null) {
                // reload favourite songs
                dataViewModel.setUser(it)
                layoutHeaderBinding.apply {
                    Glide.with(this@MainActivity).load(it.photoUrl)
                        .error(R.drawable.img_avatar_error)
                        .into(imgAvatar)
                    tvUserName.text = it.displayName
                    tvEmail.text = it.email
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
                        visibility = if (currentFragmentViewPager2Adapter == FRAGMENT_HOME) {
                            View.GONE
                        } else {
                            View.VISIBLE
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

    private fun onBackPressCallBack() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.apply {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        // drawer is opening
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else if (binding.layoutMusicPlayer.visibility == View.VISIBLE) {
                        // music player is opening
                        closeMusicPlayerView()

                        val fragment =
                            supportFragmentManager.findFragmentById(R.id.layout_music_player)
                        if (fragment != null) {
                            supportFragmentManager.beginTransaction().remove(fragment).commit()
                            supportFragmentManager.popBackStack()
                        }
                    } else if (mainUi.visibility == View.GONE && contentFrame.visibility == View.VISIBLE) {
                        // main is gone and content frame is opening
                        if (currentFragment == FRAGMENT_CHANGE_PASSWORD) {
                            supportFragmentManager.popBackStack()
                            currentFragment = FRAGMENT_ACCOUNT
                        } else {
                            currentFragment = 0
                            setTitleForToolbar(0, 1)
                            if (selectedMenuItem == null) {
                                selectedMenuItem =
                                    binding.navigationView.menu.findItem(R.id.nav_appearance)
                            }
                            selectedMenuItem!!.isChecked = false
                            closeNavigation()
                            if (currentFragmentViewPager2Adapter != FRAGMENT_HOME) {
                                customButton.visibility = View.VISIBLE
                            }
                        }
                    } else if (currentFragmentViewPager2Adapter != FRAGMENT_HOME) {
                        bottomNavigation.currentItem = 0
                    } else {
                        Log.e(MAIN_ACTIVITY_TAG, "Close app")
                        finish()
                    }
                }
            }
        }
    }

    fun setTitleForToolbar(position: Int, mode: Int) {
        binding.toolBarTitle.text = if (mode == 1) {
            when (currentFragmentViewPager2Adapter) {
                FRAGMENT_HOME -> TITLE_HOME
                FRAGMENT_ONLINE_SONGS -> TITLE_ONLINE_SONGS
                FRAGMENT_FAVOURITE_SONGS -> TITLE_FAVOURITE_SONGS
                FRAGMENT_DEVICE_SONGS -> TITLE_DEVICE_SONGS
                else -> {
                    ""
                }
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
            layoutMusicPlayer.visibility = View.GONE
            mainUi.visibility = View.VISIBLE
            appBar.visibility = View.VISIBLE
        }
    }

    fun openNavigation() {
        binding.apply {
            contentFrame.visibility = View.VISIBLE
            mainUi.visibility = View.GONE
            appBar.visibility = View.VISIBLE
            layoutMusicPlayer.visibility = View.GONE
        }
    }

    private fun closeNavigation() {
        binding.apply {
            contentFrame.visibility = View.GONE
            mainUi.visibility = View.VISIBLE
            appBar.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getBinding() = this.binding

    override fun onResume() {
        super.onResume()
        Log.e(MAIN_ACTIVITY_TAG, "onResume")
        viewModel.setUser(FirebaseAuth.getInstance().currentUser)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onPause() {
        super.onPause()
        Log.e(MAIN_ACTIVITY_TAG, "onPause")
        onBackPressedCallback.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(MAIN_ACTIVITY_TAG, "onDestroy")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(currentTimeReceiver)
        viewModel.getThemeMode()?.let { DataLocalManager.putStringThemeMode(it) }
    }
}