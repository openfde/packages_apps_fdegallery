package com.fde.gallery3d.ui.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fde.gallery3d.R

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * timelines depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class TimelineFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pictureViewModel =
            ViewModelProvider(this).get(TimelineViewModel::class.java)

        val root = inflater.inflate(
            R.layout.fragment_timeline,
            container,
            false
        )

        val recyclerView: RecyclerView =
            root.findViewById(R.id.recyclerView)

        val adapter = timelineAdapter()
        recyclerView.adapter = adapter
        pictureViewModel.texts.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    class timelineAdapter :
        ListAdapter<String, timelineViewHolder>(object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }) {

        private val drawables = listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10,
            R.drawable.avatar_11,
            R.drawable.avatar_12,
            R.drawable.avatar_13,
            R.drawable.avatar_14,
            R.drawable.avatar_15,
            R.drawable.avatar_16,
        )


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): timelineViewHolder {

            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_timeline,
                parent,
                false
            )

            return timelineViewHolder(view)
        }

        override fun onBindViewHolder(holder: timelineViewHolder, position: Int) {
//            holder.textView.text = getItem(position)
//            holder.imageView.setImageDrawable(
//                ResourcesCompat.getDrawable(holder.imageView.resources, drawables[position], null)
//            )
        }
    }

    class timelineViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

//        val imageView: ImageView =
//            itemView.findViewById(R.id.image_view_item_timeline)
//
//        val textView: TextView =
//            itemView.findViewById(R.id.text_view_item_timeline)
    }


}