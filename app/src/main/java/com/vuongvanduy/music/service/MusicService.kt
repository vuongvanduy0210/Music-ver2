package com.vuongvanduy.music.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.vuongvanduy.music.R
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.broadcast_receiver.MyReceiver
import com.vuongvanduy.music.model.Song
import com.vuongvanduy.music.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private var mediaPlayer: MediaPlayer? = null

    private var currentSong: Song? = null
    private var songs: MutableList<Song>? = null
    private var isPlaying: Boolean = false
    private var isLooping: Boolean = false
    private var isShuffling: Boolean = false
    private var isListShuffled: Boolean = false

    private var currentTime: Int = 0
    private var finalTime: Int = 0
    private var progressReceive: Int = 0

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val handler = Looper.myLooper()
    private var runnable = Runnable {
        if (mediaPlayer != null && isPlaying) {
            currentTime = mediaPlayer!!.currentPosition
            updateCurrentTime()
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
            // thuc hien hanh dong sau khi nhan action tu activity hoac fragment
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
        }
    }

    private fun startMusic() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setOnCompletionListener(this)
        mediaPlayer!!.setOnPreparedListener(this)
        mediaPlayer?.apply {
            val uri = Uri.parse(currentSong?.getResourceUri())
            setDataSource(this@MusicService, uri)
            prepare()
            finalTime = duration
        }
        isPlaying = true
        sendNotification()
        sendData(ACTION_START)

        if (isShuffling && isListSortedAscending(songs!!)) {
            songs?.shuffle()
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
        sendNotification()
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
        sendNotification()
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
            songs?.sortBy {
                it.getName().lowercase()
            }
        }
        isShuffling = !isShuffling
        sendData(ACTION_SHUFFLE)
    }

    private fun sendNotification() {

        val imageUri = Uri.parse(currentSong?.getImageUri())
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_music)
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

        //set on click notification
        val intent = Intent(this, MainActivity::class.java)
        @SuppressLint("UnspecifiedImmutableFlag")
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setSound(null)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (currentSong?.getImageUri()?.contains("firebasestorage.googleapis.com")
            == true) {
            try {
                serviceScope.launch {
                    val url = URL(currentSong?.getImageUri())
                    val connection = withContext(Dispatchers.IO) {
                        url.openConnection()
                    } as HttpURLConnection
                    connection.doInput = true
                    withContext(Dispatchers.IO) {
                        connection.connect()
                    }
                    val input: InputStream = connection.inputStream
                    val bitmap: Bitmap = BitmapFactory.decodeStream(input)
                    notificationLayout.setImageViewBitmap(R.id.img_bg_noti, bitmap)
                    notification.setCustomContentView(notificationLayout)
                    val notificationBuilder: Notification = notification.build()
                    startForeground(1, notificationBuilder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            notificationLayout.setImageViewUri(R.id.img_bg_noti, imageUri)
        }
        notification.setCustomContentView(notificationLayout)
        val notificationBuilder: Notification = notification.build()
        startForeground(1, notificationBuilder)
    }

    private fun getPendingIntent(context: Context, action: Int): PendingIntent? {
        val intent = Intent(this, MyReceiver::class.java)
        intent.putExtra(ACTION_MUSIC_NAME, action)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            action, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
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
//        Log.e(MUSIC_SERVICE_TAG, "send current time success");
//        Log.e(MUSIC_SERVICE_TAG, currentTime.toString());
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
        for (i in 1 until list.size) {
            if (list[i].getName().lowercase() < list[i - 1].getName().lowercase()) {
                return false
            }
        }
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mediaPlayer == null) {
            return
        }
        if (isLooping) {
            mp!!.start()
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
        mp!!.start()
        updateCurrentTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
}