package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import com.vuongvanduy.music.R
import com.vuongvanduy.music.adapter.FragmentViewPager2Adapter
import com.vuongvanduy.music.databinding.ActivityMainBinding
import com.vuongvanduy.music.fragment.*
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.transformer.ZoomOutPageTransformer
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.MainViewModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var currentFragment: Int = 0
    private var currentFragmentViewPager2Adapter: Int = 0

    lateinit var viewModel: MainViewModel

    private lateinit var selectedMenuItem: MenuItem

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                viewModel.receiveDataFromReceiver(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.setData(this)
        observerAction()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(serviceReceiver, IntentFilter(SEND_DATA))

        setToolbarAndDrawerLayout()

        setViewPager2Adapter()

        setBottomNavigation()

        setListenerForMiniPlayer()
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

    private fun setListenerForMiniPlayer() {
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
        currentFragment = FRAGMENT_MUSIC_PLAYER
        binding.appBar.visibility = View.GONE
        openNavigationFragment()
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
            when(value) {
                ACTION_START -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_NEXT -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_PREVIOUS -> viewModel.currentSong?.let { setLayoutMiniPlayer(it) }
                ACTION_CLEAR -> {
                    viewModel.getPlaying().value = false
                    binding.miniPlayer.visibility = View.GONE
                }
            }
        }
    }

    private fun setToolbarAndDrawerLayout() {

        binding.toolBar.title = ""
        setSupportActionBar(binding.toolBar)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolBar,
            R.string.nav_drawer_open, R.string.nav_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.drawerLayout.addDrawerListener(object: DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) { hideKeyboard() }
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        selectedMenuItem = item

        when(item.itemId) {
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
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @SuppressLint("CommitTransaction")
    fun replaceFragment(fragment: Fragment, current: Int, title: String) {
        if (currentFragment != current) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.content_frame, fragment)
            transaction.commit()
            currentFragment = current
            binding.toolBarTitle.text = title
            openNavigationFragment()
        }
    }

    private fun setViewPager2Adapter() {
        val adapter = FragmentViewPager2Adapter(this)

        binding.viewPager2.apply {
            setAdapter(adapter)
            isUserInputEnabled = false
            setPageTransformer(ZoomOutPageTransformer())

            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                @SuppressLint("LongLogTag")
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.bottomNavigation.currentItem = position
                    currentFragmentViewPager2Adapter = position + 1
                    setTitleForToolbar(position, 2)
                }
            })
        }
    }

    private fun setBottomNavigation() {
        val itemAll = AHBottomNavigationItem(
            R.string.title_all, R.drawable.ic_all, R.color.teal_200
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        binding.apply {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else if (currentFragment == FRAGMENT_MUSIC_PLAYER) {
                closeNavigationFragment()
                val fragment = supportFragmentManager.findFragmentById(R.id.content_frame)
                if (fragment != null) {
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                }
                binding.appBar.visibility = View.VISIBLE
                currentFragment = 0
            } else {
                mainUi.apply {
                    if (visibility == View.GONE) {
                        visibility = View.VISIBLE
                    }
                    else {
                        super.onBackPressed()
                    }
                }
                currentFragment = 0
                setTitleForToolbar(0, 1)
                selectedMenuItem.isChecked = false
                closeNavigationFragment()
            }
        }
    }

    private fun setTitleForToolbar(position: Int, mode: Int) {
        binding.toolBarTitle.text = if (mode == 1) {
            when(currentFragmentViewPager2Adapter) {
                FRAGMENT_HOME -> TITLE_HOME
                FRAGMENT_ALL_SONGS -> TITLE_ALL_SONGS
                FRAGMENT_FAVOURITE_SONGS -> TITLE_FAVOURITE_SONGS
                else -> TITLE_DEVICE_SONGS
            }
        } else {
            when(position) {
                0 -> TITLE_HOME
                1 -> TITLE_ALL_SONGS
                2 -> TITLE_FAVOURITE_SONGS
                else -> TITLE_DEVICE_SONGS
            }
        }
    }

    private fun openNavigationFragment() {
        binding.contentFrame.apply {
            if (visibility == View.GONE) { visibility = View.VISIBLE }
        }
        binding.mainUi.apply {
            if (visibility == View.VISIBLE) { visibility = View.GONE }
        }
    }

    private fun closeNavigationFragment() {
        binding.contentFrame.apply {
            if (visibility == View.VISIBLE) { visibility = View.GONE }
        }
        binding.mainUi.apply {
            if (visibility == View.GONE) { visibility = View.VISIBLE }
        }
    }

    fun getBinding() = this.binding

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver)
    }
}