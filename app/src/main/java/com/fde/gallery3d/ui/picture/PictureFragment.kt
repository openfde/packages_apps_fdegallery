package com.fde.gallery3d.ui.picture

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fde.gallery3d.R
import com.fde.gallery3d.ui.adapter.PictureListAdapter
import com.fde.gallery3d.ui.bean.Multimedia
import com.fde.gallery3d.ui.event.ViewEvent
import com.fde.gallery3d.ui.utils.DeviceUtils
import com.fde.gallery3d.ui.utils.LogTools
import com.fde.gallery3d.ui.view.CustomScrollBarView
import com.fde.gallery3d.ui.view.RecyclerScrollBinder


class PictureFragment : Fragment() {


    val permissions: Array<String?> = arrayOf<String?>(
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )

    private lateinit var pictureListAdapter: PictureListAdapter
    private val viewModel: PictureViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

//        val pictureViewModel =
//            ViewModelProvider(this).get(PictureViewModel::class.java)

        val root = inflater.inflate(
            R.layout.fragment_picture,
            container,
            false
        )

        initView(root)
        observeViewModel()


        if (Build.VERSION.SDK_INT >= 23) {
            val iGetPerm = requireContext().checkSelfPermission(permissions[1]!!)
            if (iGetPerm == -1) {
                requestPermissions(permissions, 888)
            }
        }

//        val textView: TextView =
//            root.findViewById(R.id.text_picture)
//
//        pictureViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        return root
    }


    private fun initView(view: View) {

        val recyclerView = view.findViewById<RecyclerView>(
            R.id.recyclerView
        )

        val bar = view.findViewById<CustomScrollBarView>(
            R.id.scrollBar
        )

        val numberOfColumns = DeviceUtils.getShowCount(requireActivity())

        val gridLayoutManager = GridLayoutManager(
            requireContext(),
            numberOfColumns
        )

        recyclerView.layoutManager = gridLayoutManager
        recyclerView.isNestedScrollingEnabled = false

        pictureListAdapter = PictureListAdapter(
            requireContext(),
            ArrayList(),
            numberOfColumns,
            object : ViewEvent {

                override fun onSelectEvent(
                    pos: Int,
                    groupPos: Int,
                    isSelect: Boolean
                ) {
                    viewModel.selectItem(pos, isSelect)
                }

                override fun onRightEvent(
                    pos: Int,
                    groupPos: Int
                ) {
                    viewModel.toggleBottomBar()
                }

                override fun onJumpEvent(
                    picture: Multimedia
                ) {
//                    openPicture(picture)
                }
            }
        )

        pictureListAdapter.setHasStableIds(true)

        recyclerView.adapter = pictureListAdapter

        RecyclerScrollBinder.bind(recyclerView, bar)
    }

    private fun observeViewModel() {

        viewModel.pictureList.observe(viewLifecycleOwner) { images ->

//            pictureListAdapter.updateData(images)

        }

        viewModel.showBottomBar.observe(viewLifecycleOwner) { show ->

            val bottom = view?.findViewById<View>(R.id.layoutBottom)

            bottom?.visibility = if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.selectedCount.observe(viewLifecycleOwner) { count ->

            LogTools.i("selectedCount = $count")
        }

        viewModel.allSelected.observe(viewLifecycleOwner) { allSelected ->

            val txtAllSelected = view?.findViewById<TextView>(
                R.id.txtAllSelected
            ) ?: return@observe

            txtAllSelected.text = if (allSelected) {
                getString(R.string.deselect_all)
            } else {
                getString(R.string.select_all)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}