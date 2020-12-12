package com.adarsh.spotify.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.adarsh.spotify.R
import com.adarsh.spotify.data.entities.Song
import com.adarsh.spotify.exoplayer.isPlaying
import com.adarsh.spotify.exoplayer.toSong
import com.adarsh.spotify.other.Status
import com.adarsh.spotify.ui.viewmodels.MainViewModel
import com.adarsh.spotify.ui.viewmodels.SongViewModel
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_song.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {
    @Inject
    lateinit var glide: RequestManager
    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()
    private var curPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null
    private var shouldUpdateSeekbar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()

        ivPlayPauseDetail.setOnClickListener {
            curPlayingSong?.let { song ->
                mainViewModel.playOrToggleSong(song, true)
            }
        }
        ivSkipPrevious.setOnClickListener {
            mainViewModel.skipToPrevious()
        }
        ivSkip.setOnClickListener {
            mainViewModel.skipToNext()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    setCurPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar=true
                }
            }
        })
    }

    private fun updateTitleANdSongImage(song: Song) {
        val title = "${song.title} -  ${song.subTitle}"
        tvSongName.text = title
        glide.load(song.imageUrl).into(ivSongImage)
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (curPlayingSong == null && songs.isNotEmpty()) {
                                curPlayingSong = songs[0]
                                updateTitleANdSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            curPlayingSong = it.toSong()
            updateTitleANdSongImage(curPlayingSong!!)
        }


        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            seekBar.progress = it?.position?.toInt() ?: 0
        }


        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if (shouldUpdateSeekbar) {
                seekBar.progress = it.toInt()
                setCurPlayerTimeToTextView(it)
            }
        }

        // how long our song is
        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            tvSongDuration.text = dateFormat.format(it)
        }
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {

        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        tvCurTime.text = dateFormat.format(ms)

    }


}