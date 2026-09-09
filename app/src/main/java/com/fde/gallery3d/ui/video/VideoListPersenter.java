/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fde.gallery3d.ui.video;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.RecoverableSecurityException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.adapter.VideoListAdapter;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoListPersenter implements ViewEvent, View.OnClickListener {
    Context context;
    View view;
    VideoListAdapter videoListAdapter;
    View layoutBottomBtn;
    RecyclerView recyclerView;
    TextView txtShare;
    TextView txtDelete;
    TextView txtAllSelected;
    TextView txtCancel;
    List<Multimedia> list;
    List<Multimedia> delList;
    int numberOfColumns = 3;
    GridLayoutManager gridLayoutManager;
    boolean isAllSelected;

    boolean isShowBottomBtn = false;

    BaseFragment baseFragment;

    public VideoListPersenter(BaseFragment baseFragment, View view) {
        this.baseFragment = baseFragment;
        this.view = view;
        context = baseFragment.getActivity();
    }

    public boolean initView() {
        numberOfColumns = DeviceUtils.getShowCount(baseFragment.getActivity());
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        layoutBottomBtn = (View) view.findViewById(R.id.layoutBottom);
        txtShare = (TextView) view.findViewById(R.id.txtShare);
        txtDelete = (TextView) view.findViewById(R.id.txtDelete);
        txtAllSelected = (TextView) view.findViewById(R.id.txtAllSelected);
        txtCancel = (TextView) view.findViewById(R.id.txtCancel);
        CustomScrollBarView bar = (CustomScrollBarView) view.findViewById(R.id.scrollBar);
        txtShare.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtAllSelected.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        gridLayoutManager = new GridLayoutManager(context, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        listenWindowResize();
//        recyclerView.addItemDecoration(new SpacesItemDecoration(2));  // Here 16 is the space size
        list = new ArrayList<>();
        videoListAdapter = new VideoListAdapter(context, list, numberOfColumns, this);
        videoListAdapter.setHasStableIds(true);
        recyclerView.setAdapter(videoListAdapter);
        recyclerView.setNestedScrollingEnabled(false);
        RecyclerScrollBinder.bind(recyclerView, bar);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            private long touchDownTime = 0;

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    touchDownTime = System.currentTimeMillis();
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    long duration = System.currentTimeMillis() - touchDownTime;
                    if (duration >= ViewConfiguration.getLongPressTimeout()) {
                        View childView = rv.findChildViewUnder(e.getX(), e.getY());
                        if (childView != null) {
                            int position = rv.getChildAdapterPosition(childView);
                            onRightEvent(position, 0);
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });


        recyclerView.setOnContextClickListener(new View.OnContextClickListener() {

            @Override
            public boolean onContextClick(View v) {
                onRightEvent(0,0);
                return false;
            }
        });

        return true;
    }

    private void listenWindowResize() {
        View root = baseFragment.getActivity().getWindow().getDecorView();
        root.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, orr, ob) -> {
                    int newW = r - l;
                    int oldW = orr - ol;
//                    if (newW != oldW) {
                        onWindowWidthChanged(newW);
//                    }
                });
    }

    private void onWindowWidthChanged(int widthPx) {
        int minItemPx = DeviceUtils.dpToPx(context, 160);
        int span = Math.max(1, widthPx / minItemPx);
        gridLayoutManager.setSpanCount(span);
        int itemSize = widthPx / span;
        videoListAdapter.updateItemSize(itemSize);
    }

    /**
     * get all video
     *
     * @param context
     */
    public void getAllVideos(Context context) {
        if (list != null) {
            list.clear();
        }
        list.addAll(FileUtils.getAllVideos(context));
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRightEvent(int pos, int groupPos) {
        isShowBottomBtn = !isShowBottomBtn;
        showOrHideBottomBtn(isShowBottomBtn);
    }

    public boolean hideBottomBtn() {
        if(layoutBottomBtn.getVisibility() == View.GONE){
            return false;
        }
        if(layoutBottomBtn !=null){
            showOrHideBottomBtn(false);
        }
        return true;
    }

    public void showOrHideBottomBtn(boolean isShow) {
        isShowBottomBtn = isShow;

        try {
            layoutBottomBtn.setVisibility(isShowBottomBtn ? View.VISIBLE : View.GONE);
            for (int i = 0; i < list.size(); i++) {
                Multimedia picture = list.get(i);
                picture.setShowCheckbox(isShowBottomBtn);
                list.set(i, picture);
            }
            videoListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isShow){
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 0));
        }else {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 50));
        }
    }

    @Override
    public void onJumpEvent(Multimedia multimedia) {

    }

    @Override
    public void onSelectEvent(int pos, int groupPos, boolean isSelect) {
        try {
            Multimedia video = list.get(pos);
            video.setSelected(isSelect);
            list.set(pos, video);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.txtShare) {
            try {
                ArrayList<Uri> imageUris = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).isSelected()) {
                        imageUris.add(
                                FileProvider.getUriForFile(
                                        context,
                                        Constant.PKG_PROVIDER,
                                        new File(list.get(i).getPath())
                                )
                        );
                    }
                }

                int size = imageUris.size();

                if (size < 1) {
                    baseFragment.showShortToast(
                            context.getString(R.string.can_not_choose_empty));
                    return;
                }

                if (size > 9) {
                    baseFragment.showShortToast(
                            context.getString(R.string.can_not_choose_too_more));
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setType("*/*");
                intent.putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        imageUris);

                baseFragment.getActivity().startActivity(
                        Intent.createChooser(
                                intent,
                                context.getString(R.string.share)));

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (id == R.id.txtDelete) {

            List<Multimedia> tempList = new ArrayList<>();
            delList = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                Multimedia video = list.get(i);

                if (video.isSelected()) {
                    delList.add(video);
                } else {
                    tempList.add(video);
                }
            }

            if (delList.isEmpty()) {
                baseFragment.showShortToast(
                        context.getString(R.string.can_not_choose_empty));
                return;
            }

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(
                            context,
                            com.fde.imageeditlibrary.R.style.RoundedAlertDialog);

            builder.setTitle(R.string.is_delete);

            builder.setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                                DialogInterface dialogInterface,
                                int pos) {

                            list.clear();
                            list.addAll(tempList);
                            deleteVideo();
                        }
                    });

            builder.setNegativeButton(R.string.cancel, null);
            builder.show();

        } else if (id == R.id.txtAllSelected) {

            isAllSelected = !isAllSelected;

            for (int i = 0; i < list.size(); i++) {
                Multimedia video = list.get(i);
                video.setSelected(isAllSelected);
                list.set(i, video);
            }

            videoListAdapter.notifyDataSetChanged();

            txtAllSelected.setText(
                    isAllSelected
                            ? context.getString(R.string.deselect_all)
                            : context.getString(R.string.select_all));

            Drawable drawableTop =
                    isAllSelected
                            ? context.getDrawable(
                            R.drawable.ic_main_not_selected)
                            : context.getDrawable(
                            R.drawable.ic_main_all_selected);

            drawableTop.setBounds(
                    0,
                    0,
                    drawableTop.getIntrinsicWidth(),
                    drawableTop.getIntrinsicHeight());

            txtAllSelected.setCompoundDrawables(
                    null,
                    drawableTop,
                    null,
                    null);

        } else if (id == R.id.txtCancel) {

            isShowBottomBtn = !isShowBottomBtn;
            showOrHideBottomBtn(isShowBottomBtn);
        }
    }

    @SuppressLint("NewApi")
    public void deleteVideo() {
        if (delList != null) {
            try {
                // 你的删除或修改文件的代码
                for (Multimedia video : delList) {
                    FileUtils.deleteVideo(context, video.getPath());
                }
                videoListAdapter.notifyDataSetChanged();
                isShowBottomBtn = !isShowBottomBtn;
                showOrHideBottomBtn(isShowBottomBtn);
            } catch (RecoverableSecurityException e) {
                baseFragment.requestConfirmDialog(e);
            }
        }
    }
}
