package com.vuongvanduy.music.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.vuongvanduy.music.R
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.broadcast_receiver.MyReceiver
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.Collator
import java.util.Locale


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener {

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    private var currentSong: Song? = null
    private var songs: MutableList<Song>? = null
    private var isPlaying: Boolean = false
    private var isLooping: Boolean = false
    private var isShuffling: Boolean = false

    private var currentTime: Int = 0
    private var finalTime: Int = 0
    private var progressReceive: Int = 0

    private val notificationScope = CoroutineScope(Dispatchers.Default)

    private val handler = Looper.myLooper()
    private var runnable = Runnable {
        if (mediaPlayer != null && isPlaying) {
            currentTime = mediaPlayer!!.currentPosition
            updateCurrentTime()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        checkOtherMusic()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkOtherMusic() {
        audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setOnAudioFocusChangeListener {focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // Lấy được quyền truy cập âm thanh
                        resumeMusic()
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // Mất quyền truy cập âm thanh
                        pauseMusic()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // Mất quyền truy cập âm thanh tạm thời
                        pauseMusic()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Mất quyền truy cập âm thanh tạm thời và có thể giảm âm lượng
                        resumeMusic()
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(MUSIC_SERVICE_TAG, "Request audio focus success")
        } else {
            Log.e(MUSIC_SERVICE_TAG, "Request audio focus fail")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val bundle = intent?.extras
        if (bundle != null) {
            //receive list songs
            val listReceive = bundle.getSerializable(KEY_LIST_SONGS)
            if (listReceive != null) {
                songs = listReceive as MutableList<Song>
            }

            //receive song
            val songReceive = bundle.getSerializable(KEY_SONG)
            if (songReceive != null) {
                currentSong = songReceive as Song?
            }
        }

        //receive progress
        progressReceive = intent!!.getIntExtra(KEY_PROGRESS, 0)
        if (progressReceive != 0) {
            handleActionMusic(ACTION_CONTROL_SEEK_BAR)
        }

        //receive action music
        val actionReceive = intent.getIntExtra(KEY_ACTION, 0)
        if (actionReceive != 0) {
            handleActionMusic(actionReceive)
        }

        return START_NOT_STICKY
    }

    private fun handleActionMusic(actionReceive: Int?) {
        when (actionReceive) {
            ACTION_START -> startMusic()
            ACTION_PREVIOUS -> previousMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_NEXT -> nextMusic()
            ACTION_CLEAR -> clearMusic()
            ACTION_OPEN_MUSIC_PLAYER -> sendData(ACTION_OPEN_MUSIC_PLAYER)
            ACTION_LOOP -> loopMusic()
            ACTION_SHUFFLE -> shuffleMusic()
            ACTION_CONTROL_SEEK_BAR -> mediaPlayer?.seekTo(progressReceive)
            ACTION_RELOAD_DATA -> sendData(ACTION_RELOAD_DATA)
        }
    }

    private fun startMusic() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer?.apply {
            setOnCompletionListener(this@MusicService)
            setOnPreparedListener(this@MusicService)
            setOnErrorListener(this@MusicService)

            val uri = Uri.parse(currentSong?.getResourceUri())
            try {
                setDataSource(this@MusicService, uri)
                this@MusicService.isPlaying = false
                sendNotification()
                sendData(ACTION_START)
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun previousMusic() {
        var index = currentSong?.let { getIndexFromListSong(it) }
        if (index == -1) {
            return
        }
        if (index == 0) {
            index = songs!!.size
        }
        currentSong = songs!![index!! - 1]
        startMusic()
        sendData(ACTION_PREVIOUS)
    }

    private fun pauseMusic() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer!!.pause()
            isPlaying = false
            sendNotification()
            sendData(ACTION_PAUSE)
        }
    }

    private fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer!!.start()
            updateCurrentTime()
            isPlaying = true
            sendNotification()
            sendData(ACTION_RESUME)
        }
    }

    private fun nextMusic() {
        var index = getIndexFromListSong(currentSong!!)

        if (index == -1) {
            return
        }
        if (index == songs!!.size - 1) {
            index = -1
        }
        currentSong = songs!![index + 1]
        startMusic()
        sendData(ACTION_NEXT)
    }

    private fun clearMusic() {
        stopSelf()
        isPlaying = false
        sendData(ACTION_CLEAR)
    }

    private fun loopMusic() {
        isLooping = !isLooping
        sendData(ACTION_LOOP)
    }

    private fun shuffleMusic() {
        if (!isShuffling) {
            songs?.shuffle()
        } else {
            val collator = Collator.getInstance(Locale("vi"))
            songs?.sortWith { obj1, obj2 ->
                collator.compare(obj1.getName()?.lowercase(), obj2.getName()?.lowercase())
            }
        }
        isShuffling = !isShuffling
        sendData(ACTION_SHUFFLE)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.e(MUSIC_SERVICE_TAG, "onCompletion")
        if (mp == null) {
            return
        }
        if (isLooping) {
            mp.start()
        } else {
            Looper.myLooper().let {
                if (it != null) {
                    Handler(it).postDelayed({ nextMusic() }, 1500)
                }
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.e(MUSIC_SERVICE_TAG, "onPrepared")
        isPlaying = true
        if (mp != null) {
            finalTime = mp.duration
        }
        sendNotification()
        sendData(ACTION_START)

        if (isShuffling && isListSortedAscending(songs!!)) {
            songs?.shuffle()
        }
        mp!!.start()
        updateCurrentTime()
    }

    private fun sendNotification() {

        val imageUri = Uri.parse(currentSong?.getImageUri())
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_music)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)

        notificationScope.launch {
            notificationLayout.apply {
                //set layout for notification
                setTextViewText(R.id.tv_music_name_in_notification, currentSong?.getName())
                setTextViewText(R.id.tv_singer_in_notification, currentSong?.getSinger())

                //// set on click button in notification
                if (isPlaying) {
                    setOnClickPendingIntent(
                        R.id.img_play_in_notification,
                        getPendingIntent(this@MusicService, ACTION_PAUSE)
                    )
                    setImageViewResource(R.id.img_play_in_notification, R.drawable.ic_pause)
                } else {
                    setOnClickPendingIntent(
                        R.id.img_play_in_notification,
                        getPendingIntent(this@MusicService, ACTION_RESUME)
                    )
                    setImageViewResource(R.id.img_play_in_notification, R.drawable.ic_play)
                }
                setOnClickPendingIntent(
                    R.id.img_clear_in_notification,
                    getPendingIntent(this@MusicService, ACTION_CLEAR)
                )
                setOnClickPendingIntent(
                    R.id.img_previous_in_notification,
                    getPendingIntent(this@MusicService, ACTION_PREVIOUS)
                )
                setOnClickPendingIntent(
                    R.id.img_next_in_notification,
                    getPendingIntent(this@MusicService, ACTION_NEXT)
                )
            }

            notificationBuilder.setSmallIcon(R.drawable.ic_music)
                .setSound(null)
                .setContentIntent(getPendingIntentClickNotification())
                .setOngoing(true)

            if (imageUri.toString().contains("firebasestorage.googleapis.com")) {

                Glide.with(this@MusicService).asBitmap().load(imageUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            // set background
                            notificationLayout.setImageViewBitmap(
                                R.id.img_bg_noti,
                                resource
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                notificationBuilder.setCustomBigContentView(notificationLayout)
                            } else {
                                notificationBuilder.setCustomContentView(notificationLayout)
                            }
                            val notification = notificationBuilder.build()
                            startForeground(1, notification)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            } else {
                notificationLayout.setImageViewUri(R.id.img_bg_noti, imageUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    notificationBuilder.setCustomBigContentView(notificationLayout)
                } else {
                    notificationBuilder.setCustomContentView(notificationLayout)
                }
                val notification = notificationBuilder.build()
                startForeground(1, notification)
            }
        }
    }

    private fun getPendingIntent(context: Context, action: Int): PendingIntent? {
        val intent = Intent(this, MyReceiver::class.java)
        intent.putExtra(ACTION_MUSIC_NAME, action)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context.applicationContext,
                action, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                context.applicationContext,
                action, intent, PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun getPendingIntentClickNotification(): PendingIntent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val intent = Intent(this, MainActivity::class.java)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            val intent = Intent(this, MainActivity::class.java)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun updateCurrentTime() {
        sendCurrentTime()
        handler.let {
            if (it != null) {
                Handler(it).postDelayed(runnable, 10)
            }
        }
    }

    private fun sendData(action: Int) {
        val intent = Intent(SEND_DATA)
        val bundle = Bundle()
        bundle.putInt(KEY_ACTION, action)
        bundle.putSerializable(KEY_SONG, currentSong)
        bundle.putInt(KEY_FINAL_TIME, finalTime)
        bundle.putBoolean(KEY_STATUS_MUSIC, isPlaying)
        bundle.putBoolean(KEY_STATUS_LOOP, isLooping)
        bundle.putBoolean(KEY_STATUS_SHUFFLE, isShuffling)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendCurrentTime() {
        val intent = Intent(SEND_CURRENT_TIME)
        val bundle = Bundle()
        bundle.putSerializable(KEY_CURRENT_TIME, currentTime)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun getIndexFromListSong(s: Song): Int {
        var index = -1
        if (songs != null) {
            for (i in songs!!.indices) {
                if (songs!![i].getName().equals(s.getName(), true) &&
                    songs!![i].getSinger().equals(s.getSinger(), true)
                ) {
                    index = i
                    break
                }
            }
        }
        return index
    }

    private fun isListSortedAscending(list: MutableList<Song>): Boolean {
        val collator = Collator.getInstance(Locale("vi"))
        for (i in 1 until list.size) {
            if (collator.compare(list[i].getName()!!, list[i - 1].getName()!!) < 0) {
                return false
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        notificationScope.cancel()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    @SuppressLint("ShowToast")
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(MUSIC_SERVICE_TAG, "MediaPlayer onError")
        if (isPlaying) {
            pauseMusic()
            Toast.makeText(
                this@MusicService,
                "Can't play this song. Check your internet connection or reload app.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }
}