package com.adarsh.spotify.data.remote

import android.util.Log
import com.adarsh.spotify.data.entities.Song
import com.adarsh.spotify.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MusicDatabase {

    val TAG = "MusicDatabase"
    private val fireStore = FirebaseFirestore.getInstance()

    private val songCollection = fireStore.collection(SONG_COLLECTION)


    // get the songs from the firestore database
    suspend fun getAllSongs(): List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e: Exception) {
            Log.d(TAG, "getAllSongs: error occured ${e.localizedMessage}")
            emptyList()
        }
    }
}