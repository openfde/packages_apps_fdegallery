package com.fde.gallery3d.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fde.gallery3d.R
import com.fde.gallery3d.ui.utils.DeviceUtils
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

        val version = root.findViewById<TextView>(R.id.txtVersion)
        version?.text = getString(
            R.string.about_version,
            DeviceUtils.getAppVersionName(requireContext())
        )

        return root
    }




    override fun onStart() {
        super.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}