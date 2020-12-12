package com.adarsh.spotify.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.adarsh.spotify.exoplayer.callbacks.MusicPlaybackPreparer
import com.adarsh.spotify.exoplayer.callbacks.MusicPlayerEventListener
import com.adarsh.spotify.exoplayer.callbacks.MusicPlayerNotificationListener
import com.adarsh.spotify.other.Constants
import com.adarsh.spotify.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "musicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    companion object {
        var curSongDuration = 0L
            private set
        const val TAG = "MusicService"
    }

    var isForgroundService = false

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer


    // this is not added in service module class so how is this depedency is provided to this class
    // this is done by hilt actually  inthe FirebaseMusicSource class we have used the @Inject constructor
    // so  this object knows how to create itself so  hilt will get that and provide it here
    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // for interaction with mdeia keys
    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var musicNotificationManager: MusicNotificationManager
    private var currentPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialized = false
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }

        // for the notification thing
        val activityIntent: PendingIntent? =
            packageManager?.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(this, 0, it, 0)
            }
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this, mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {// this is called when current song switches
            curSongDuration = exoPlayer.duration
        }

        val musicPlayerPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            currentPlayingSong = it
            preparePlayer(firebaseMusicSource.songs, it, true)
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(musicPlayerPreparer)
            setQueueNavigator(MusicQueueNavigator())
            setPlayer(exoPlayer)
        }
        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val curSongIndex = if (currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(Constants.MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
                when(parentId){
                    Constants.MEDIA_ROOT_ID -> {
                        val resultSent = firebaseMusicSource.whenReady {  isInitiazed->
                            if(isInitiazed){
                                result.sendResult(firebaseMusicSource.asMediaItem())
                                if(!isPlayerInitialized  && firebaseMusicSource.songs.isNotEmpty()){
                                    preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                                    isPlayerInitialized = true
                                }
                            }else{
                                mediaSession .sendSessionEvent(NETWORK_ERROR,null)
                                result.sendResult(null)
                            }

                        }

                        if(!resultSent){
                            result.detach()
                        }
                    }
                }
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }

    }

}