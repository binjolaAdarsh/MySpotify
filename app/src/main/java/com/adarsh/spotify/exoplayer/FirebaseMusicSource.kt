package com.adarsh.spotify.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.adarsh.spotify.data.remote.MusicDatabase
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class FirebaseMusicSource @Inject constructor(private val musicDatabase: MusicDatabase) {
    val TAG = "FirebaseMusicSource"

    var songs = emptyList<MediaMetadataCompat>()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    // if the new value to be set in STATE_INITIALIZED or STATE_ERROR than we update the list of boolean ready listeners
    //  if( the state is STATE_INITAILIZED then we pass true else false
    private var state: State = State.STATE_CREATED
        set(value) { // this will gets called when we set value of the state variable
            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }

        }


    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = State.STATE_INITIALIZING

        val allSongs= musicDatabase.getAllSongs()

        // all songs is of Song type we need to change it to the mediaMetaDataCompat
        songs = allSongs.map { song->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.subTitle)
                .putString(METADATA_KEY_MEDIA_ID,song.mediaId.toString())
                .putString(METADATA_KEY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subTitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subTitle)
                .build()
        }
         state = State.STATE_INITIALIZED
    }
    // we want to have to play the first then second song and so on
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory) : ConcatenatingMediaSource {
        val concatenatingMediaSource= ConcatenatingMediaSource()

        songs.forEach {song->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    // list of mediaItems single song or playlist

    fun asMediaItem()= songs.map {song->
        val desc= MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }.toMutableList()


     fun whenReady (action : (Boolean) -> Unit):Boolean{
         return if(state == State.STATE_CREATED || state == State.STATE_INITIALIZING){
             onReadyListeners += action
             false
         }else{
             action(state == State.STATE_INITIALIZED)
             true
         }
     }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}