package com.vuongvanduy.music.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.vuongvanduy.music.R
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.databinding.FragmentMusicPlayerBinding
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.service.MusicService
import com.vuongvanduy.music.util.*
import com.vuongvanduy.music.viewmodel.MusicPlayerViewModel


class MusicPlayerFragment : Fragment() {

    private lateinit var binding: FragmentMusicPlayerBinding

    private lateinit var activity: MainActivity

    private lateinit var viewModel: MusicPlayerViewModel

    private val handler = Looper.myLooper()
    inner class UpdateSeekBar : Runnable {
        override fun run() {
            val currentTime = viewModel.currentTime
            binding.seekBarMusic.progress = currentTime
            val minutes: Int = currentTime / 1000 / 60
            val seconds: Int = currentTime / 1000 % 60
            @SuppressLint("DefaultLocale")
            val str = String.format("%02d:%02d", minutes, seconds)
            binding.tvCurrentTime.text = str
            handler.let {
                if (it != null) {
                    Handler(it).postDelayed(this, 1000)
                }
            }
        }
    }

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                viewModel.receiveDataFromServiceReceiver(intent)
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[MusicPlayerViewModel::class.java]
        LocalBroadcastManager.getInstance(activity)
            .registerReceiver(serviceReceiver, IntentFilter(SEND_DATA))
        LocalBroadcastManager.getInstance(activity)
            .registerReceiver(currentTimeReceiver, IntentFilter(SEND_CURRENT_TIME))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observerAction()
        setListenerButton()
        setListenerToolbar()
    }

    private fun setListenerToolbar() {
        binding.imgBack.setOnClickListener {
            activity.apply {
                supportFragmentManager.beginTransaction()
                    .remove(this@MusicPlayerFragment).commit()
                if (getBinding().layoutMusicPlayer.visibility == View.VISIBLE) {
                    closeMusicPlayerView()
                }
            }
        }
    }

    private fun setListenerButton() {
        binding.apply {
            imgPrevious.setOnClickListener {
                activity.viewModel.sendDataToService(ACTION_PREVIOUS)
            }
            imgPlay.setOnClickListener {
                if (viewModel.isPlaying.value == true) {
                    activity.viewModel.sendDataToService(ACTION_PAUSE)
                } else {
                    activity.viewModel.sendDataToService(ACTION_RESUME)
                }
            }

            imgNext.setOnClickListener {
                activity.viewModel.sendDataToService(ACTION_NEXT)
            }

            btLoop.setOnClickListener {
                activity.viewModel.sendDataToService(ACTION_LOOP)
            }

            btShuffle.setOnClickListener {
                activity.viewModel.sendDataToService(ACTION_SHUFFLE)
            }
        }
    }

    private fun observerAction() {
        viewModel.action.observe(activity) {
            when(it) {
                ACTION_CLEAR -> {
                    activity.apply {
                        if (getBinding().layoutMusicPlayer.visibility == View.VISIBLE) {
                            closeMusicPlayerView()
                        }
                    }
                }
                ACTION_OPEN_MUSIC_PLAYER -> {
                    setLayoutForMusicPlayer(viewModel.currentSong)
                }
                else -> setLayoutForMusicPlayer(viewModel.currentSong)
            }
        }
        viewModel.isPlaying.observe(activity) {
            if (it) {
                binding.imgPlay.setImageResource(R.drawable.ic_pause)
                startAnimation()
            } else {
                binding.imgPlay.setImageResource(R.drawable.ic_play)
                stopAnimation()
            }
        }
        viewModel.isLooping.observe(activity) {
            if (it) {
                binding.btLoop.setImageResource(R.drawable.ic_is_looping)
            } else {
                binding.btLoop.setImageResource(R.drawable.ic_loop)
            }
        }
        viewModel.isShuffling.observe(activity) {
            if (it) {
                binding.btShuffle.setImageResource(R.drawable.ic_is_shuffling)
            } else {
                binding.btShuffle.setImageResource(R.drawable.ic_shuffle)
            }
        }
        viewModel.finalTime.observe(activity) {
            val final = viewModel.finalTime.value
            if (final != null) {
                binding.seekBarMusic.max = final
                val minutes: Int = final / 1000 / 60
                val seconds: Int = final / 1000 % 60
                @SuppressLint("DefaultLocale")
                val str = String.format("%02d:%02d", minutes, seconds)
                binding.tvFinalTime.text = str
            }
        }
    }

    private fun setSeekBarStatus() {
        binding.seekBarMusic.apply {

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        // gui current time lai cho service
                        sendActionToService(progress)
                        if (viewModel.isPlaying.value == false) {
                            activity.viewModel.sendDataToService(ACTION_RESUME)
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        val updateSeekBar = UpdateSeekBar()
        handler.let {
            if (it != null) {
                Handler(it).post(updateSeekBar)
            }
        }
    }

    private fun sendActionToService(progress: Int) {
        val intentActivity = Intent(activity, MusicService::class.java)
        intentActivity.putExtra(KEY_ACTION, ACTION_CONTROL_SEEK_BAR)
        intentActivity.putExtra(KEY_PROGRESS, progress)
        activity.startService(intentActivity)
    }

    private fun setLayoutForMusicPlayer(song: Song) {
        binding.apply {
            val imageUri = Uri.parse(song.getImageUri())
            Glide.with(activity).load(imageUri).into(circleImageView)
            tvMusicName.text = song.getName()
            tvSinger.text = song.getSinger()
            Glide.with(activity).load(imageUri).into(imgBackGround)
        }
        setSeekBarStatus()
    }

    private fun startAnimation() {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                binding.circleImageView.animate()
                    .rotationBy(360f).withEndAction(this).setDuration(10000)
                    .setInterpolator(LinearInterpolator()).start()
            }
        }
        binding.circleImageView.animate()
            .rotationBy(360f).withEndAction(runnable).setDuration(10000)
            .setInterpolator(LinearInterpolator()).start()
    }

    private fun stopAnimation() {
        binding.circleImageView.animate().cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(serviceReceiver)
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(currentTimeReceiver)
    }
}