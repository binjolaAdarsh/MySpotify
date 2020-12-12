package com.adarsh.spotify.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.adarsh.spotify.data.entities.Song


fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            it.mediaId?.toInt() ?: -1,
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString()
        )

    }
}