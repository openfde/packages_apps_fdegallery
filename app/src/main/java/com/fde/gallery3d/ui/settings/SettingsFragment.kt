package com.fde.gallery3d.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fde.gallery3d.R
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.RawResourceDataSource

class SettingsFragment : Fragment() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private var player: SimpleExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pictureViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        val root = inflater.inflate(
            R.layout.fragment_settings,
            container,
            false
        )

        val playerView: PlayerView =
            root.findViewById<PlayerView>(R.id.player_view)

        initializePlayer(playerView)

        return root
    }


    private fun initializePlayer(playerView: PlayerView) {
        player = ExoPlayerFactory.newSimpleInstance(activity)
        playerView.player = player

        val rawResourceDataSource = RawResourceDataSource(context)
        val rawResourceUri = RawResourceDataSource.buildRawResourceUri(R.raw.aa)
        try {
            rawResourceDataSource.open(
                DataSpec(rawResourceUri)
            )

            val dataSource = rawResourceDataSource

            val mediaSource = ExtractorMediaSource.Factory {
                dataSource
            }.createMediaSource(rawResourceUri)

            player?.prepare(mediaSource)
            player?.playWhenReady = true

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        player?.release()
        player = null
    }
}