package com.fde.gallery3d.ui.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fde.gallery3d.R

class VideoFragment : Fragment() {


    // This property is only valid between onCreateView and
    // onDestroyView.

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val pictureViewModel =
            ViewModelProvider(this).get(VideoViewModel::class.java)

        val root = inflater.inflate(
            R.layout.fragment_video,
            container,
            false
        )

//        val textView: TextView =
//            root.findViewById(R.id.text_video)
//
//        pictureViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}