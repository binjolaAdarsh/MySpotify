package com.adarsh.spotify.ui.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adarsh.spotify.R
import com.adarsh.spotify.ui.viewmodels.MainViewModel
import com.adarsh.spotify.adapters.SongAdapter
import com.adarsh.spotify.other.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment:Fragment(R.layout.fragment_home) {
    lateinit var  mainViewModel: MainViewModel
//    private  val mainViewModel : MainViewModel by viewModels()

    val TAG="HomeFragment"
    @Inject
    lateinit var  songAdapter: SongAdapter
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel=ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
         setupRecycclerView()
        subscribeToObservers()
        songAdapter.setItemCLickListener {song ->
            Log.d(TAG, " item clicked ")
            mainViewModel.playOrToggleSong(song)
        }
    }
    private  fun setupRecycclerView ()= rvAllSongs.apply {
         adapter= songAdapter
        layoutManager= LinearLayoutManager(requireContext())
    }

     private  fun subscribeToObservers(){
         mainViewModel.mediaItems.observe(viewLifecycleOwner){ result->
             when(result.status){
                 Status.SUCCESS -> {
                     allSongsProgressBar.isVisible = false
                     result.data?.let { songs ->
                         Log.d(TAG, "subscribeToObservers: songs are ${songs.toString()}")
                         songAdapter.songs = songs
                     }
                 }
                 Status.ERROR -> Unit
                 Status.LOADING -> {
                     allSongsProgressBar.isVisible= true
                 }
             }
         }
     }
}