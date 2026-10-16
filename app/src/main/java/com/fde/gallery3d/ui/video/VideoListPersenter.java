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
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.trash.TrashManager;
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
    TextView txtCollect;
    List<Multimedia> list;
    List<Multimedia> delList;
    int numberOfColumns = 3;
    GridLayoutManager gridLayoutManager;
    boolean isAllSelected;

    boolean isShowBottomBtn = false;

    /** Shift 范围选择的锚点（最后一次普通单击 / Ctrl 单击的位置） */
    private int anchorPos = -1;

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
        txtCollect = (TextView) view.findViewById(R.id.txtCollect);
        CustomScrollBarView bar = (CustomScrollBarView) view.findViewById(R.id.scrollBar);
        txtShare.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtAllSelected.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        txtCollect.setOnClickListener(this);
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isShow){
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 0));
            updateCollectButtonState();
        }else {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 50));
        }
    }

    @Override
    public void onJumpEvent(Multimedia multimedia) {

    }

    @Override
    public void onSelectEvent(int pos, int groupPos, boolean isSelect) {
        onSelectEvent(pos, groupPos, isSelect, false, false);
    }

    @Override
    public void onSelectEvent(int pos, int groupPos, boolean isSelect,
                              boolean ctrlPressed, boolean shiftPressed) {
        try {
            if (list == null || pos < 0 || pos >= list.size()) {
                return;
            }
            if (ctrlPressed) {
                // Ctrl 单击：只追加/取消当前项，不影响其它已选项
                Multimedia video = list.get(pos);
                video.setSelected(!video.isSelected());
                list.set(pos, video);
                videoListAdapter.notifyItemChanged(pos);
            } else if (shiftPressed) {
                // Shift 单击：选中上次锚点到当前位置之间的所有项（范围选择）
                int anchor = (anchorPos >= 0 && anchorPos < list.size()) ? anchorPos : pos;
                int start = Math.min(anchor, pos);
                int end = Math.max(anchor, pos);
                boolean changed = false;
                for (int i = start; i <= end; i++) {
                    Multimedia video = list.get(i);
                    if (!video.isSelected()) {
                        video.setSelected(true);
                        list.set(i, video);
                        changed = true;
                    }
                }
                if (changed) {
                    videoListAdapter.notifyItemRangeChanged(start, end - start + 1);
                }
            } else {
                // 普通单击：取消其它所有选择，只选中当前项
                boolean changed = false;
                for (int i = 0; i < list.size(); i++) {
                    Multimedia video = list.get(i);
                    boolean shouldSelect = (i == pos);
                    if (video.isSelected() != shouldSelect) {
                        video.setSelected(shouldSelect);
                        list.set(i, video);
                        changed = true;
                    }
                }
                if (changed) {
                    videoListAdapter.notifyDataSetChanged();
                }
            }
            // 普通单击与 Ctrl 单击会重置 Shift 锚点，Shift 选择不移动锚点
            if (!shiftPressed) {
                anchorPos = pos;
            }
            updateBottomBarBySelection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBottomBarBySelection() {
        boolean anySelected = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isSelected()) {
                anySelected = true;
                break;
            }
        }
        if (anySelected != isShowBottomBtn) {
            showOrHideBottomBtn(anySelected);
        }
    }

    /**
     * 选中项都已收藏时，收藏按钮显示已收藏图标
     */
    private void updateCollectButtonState() {
        if (txtCollect == null) {
            return;
        }
        boolean hasSelected = false;
        boolean allFavorite = true;
        for (int i = 0; i < list.size(); i++) {
            Multimedia video = list.get(i);
            if (video.isSelected()) {
                hasSelected = true;
                if (!FavoriteStore.isFavorite(context, video.getPath())) {
                    allFavorite = false;
                    break;
                }
            }
        }
        boolean favorite = hasSelected && allFavorite;
        Drawable drawableTop = context.getDrawable(
                favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        drawableTop.setBounds(
                0,
                0,
                drawableTop.getIntrinsicWidth(),
                drawableTop.getIntrinsicHeight());
        txtCollect.setCompoundDrawables(
                null,
                drawableTop,
                null,
                null);
    }

    private void clearAllSelection() {
        isAllSelected = false;
        for (int i = 0; i < list.size(); i++) {
            Multimedia video = list.get(i);
            if (video.isSelected()) {
                video.setSelected(false);
                list.set(i, video);
            }
        }
        videoListAdapter.notifyDataSetChanged();
        txtAllSelected.setText(context.getString(R.string.select_all));
        Drawable drawableTop = context.getDrawable(R.drawable.ic_main_all_selected);
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
        showOrHideBottomBtn(false);
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
                                        Constant.getProviderAuthority(context),
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

            builder.setTitle(R.string.is_move_to_trash);

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

        } else if (id == R.id.txtCollect) {

            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                Multimedia video = list.get(i);
                if (video.isSelected() && FavoriteStore.add(context, video)) {
                    count++;
                }
            }

            if (count < 1) {
                baseFragment.showShortToast(
                        context.getString(R.string.can_not_choose_empty));
                return;
            }

            baseFragment.showShortToast(
                    context.getString(R.string.favorite_added_count, count));

            updateCollectButtonState();

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

            updateBottomBarBySelection();
            updateCollectButtonState();

        } else if (id == R.id.txtCancel) {

            clearAllSelection();
        }
    }

    @SuppressLint("NewApi")
    public void deleteVideo() {
        if (delList != null && !delList.isEmpty()) {
            // 移入回收站，代替直接删除
            int count = TrashManager.moveToTrash(context, delList);
            if (count > 0) {
                baseFragment.showShortToast(
                        context.getString(R.string.moved_to_trash_count, count));
            } else {
                baseFragment.showShortToast(
                        context.getString(R.string.move_to_trash_failed));
            }
            videoListAdapter.notifyDataSetChanged();
            updateBottomBarBySelection();
        }
    }
}
