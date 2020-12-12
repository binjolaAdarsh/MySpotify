package com.adarsh.spotify.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adarsh.spotify.data.entities.Song
import com.adarsh.spotify.exoplayer.MusicServiceConnection
import com.adarsh.spotify.exoplayer.isPlayEnabled
import com.adarsh.spotify.exoplayer.isPlaying
import com.adarsh.spotify.exoplayer.isPrepared
import com.adarsh.spotify.other.Constants.MEDIA_ROOT_ID
import com.adarsh.spotify.other.Resource

class MainViewModel @ViewModelInject constructor( private  val musicServiceConnection: MusicServiceConnection):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playBackState

    init {
    _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items= children.map {
                    Song( it.mediaId!!.toInt(),
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString(),)
                }

                _mediaItems.postValue(Resource.success(items))
            }
        })

    }
    fun skipToNext (){
        musicServiceConnection.transportControls.skipToNext()
    }
    fun skipToPrevious (){
        musicServiceConnection.transportControls.skipToPrevious()
    }
    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong (mediaItem:Song , toggle : Boolean = false){
        val isPrepared =playbackState.value?.isPrepared ?:false

        if(isPrepared && mediaItem.mediaId.toString() == curPlayingSong.value?.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID
            )){
            playbackState.value?.let {playBackState ->
                when{
                    playBackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
                    playBackState.isPlayEnabled ->  musicServiceConnection.transportControls.play()
                    else -> Unit
                }

            }
        }else{
            // play new song
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId.toString(),null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unSubscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){})
    }

}