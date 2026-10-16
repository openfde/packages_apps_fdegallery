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
package com.fde.gallery3d.ui.picture;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.adapter.PictureListAdapter;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.trash.TrashManager;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureListPersenter implements ViewEvent, View.OnClickListener {
    Context context;
    View view;



    PictureListAdapter pictureListAdapter;
    RecyclerView recyclerView;
    List<Multimedia> list;
    List<Multimedia> delList;

    View layoutBottomBtn;
    TextView txtShare;
    TextView txtDelete;
    TextView txtAllSelected;
    TextView txtCancel;
    TextView txtCollect;
    boolean isAllSelected;
    boolean isShowBottomBtn = false;

    /** Shift 范围选择的锚点（最后一次普通单击 / Ctrl 单击的位置，越界时取当前选中项） */
    private int anchorPos = -1;

    int numberOfColumns = 3;


    BaseFragment baseFragment;

    GridLayoutManager gridLayoutManager;
    View.OnLayoutChangeListener windowLayoutListener;

    public PictureListPersenter(BaseFragment baseFragment, View view) {
        this.baseFragment = baseFragment;
        this.view = view;
        context = baseFragment.requireActivity();
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
        CustomScrollBarView bar = (CustomScrollBarView)view.findViewById(R.id.scrollBar);
        txtShare.setOnClickListener(this);
        txtAllSelected.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        txtCollect.setOnClickListener(this);
        gridLayoutManager = new GridLayoutManager(context, 3);

        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);

        listenWindowResize();
//        recyclerView.addItemDecoration(new SpacesItemDecoration(2));
        list = new ArrayList<>();
        pictureListAdapter = new PictureListAdapter(context, list, numberOfColumns, this);
        pictureListAdapter.setHasStableIds(true);
        recyclerView.setAdapter(pictureListAdapter);

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
        windowLayoutListener = (v, l, t, r, b, ol, ot, orr, ob) -> onWindowWidthChanged(r - l);
        root.addOnLayoutChangeListener(windowLayoutListener);
        // 立即按当前窗口宽度计算一次，避免首帧 itemSize 为 0
        onWindowWidthChanged(root.getWidth());
    }

    /**
     * 列表 Fragment 的 View 销毁时释放监听，避免多次创建后残留监听导致重复刷新
     */
    public void release() {
        if (windowLayoutListener != null && baseFragment.getActivity() != null) {
            try {
                baseFragment.getActivity().getWindow().getDecorView()
                        .removeOnLayoutChangeListener(windowLayoutListener);
            } catch (Exception ignored) {
            }
        }
        windowLayoutListener = null;
    }

    private void onWindowWidthChanged(int widthPx) {
        if (widthPx <= 0 || gridLayoutManager == null || pictureListAdapter == null) {
            return;
        }
        int minItemPx = DeviceUtils.dpToPx(context, 160);
        int span = Math.max(1, widthPx / minItemPx);

        gridLayoutManager.setSpanCount(span);

        int itemSize = widthPx / span;
        pictureListAdapter.updateItemSize(itemSize);
    }

    /***
     * get all picture
     * @param context
     */
    public void getAllImages(Context context) {
        if (list == null) {
            return;
        }
        List<Multimedia> images = FileUtils.getAllImages(context);
        if (pictureListAdapter == null) {
            LogTools.i("pictureListAdapter is null");
            return;
        }
        // 用 DiffUtil 只更新真正变化的条目，避免整表重新绑定/重新加载缩略图
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new MediaDiffCallback(list, images), true);
        list.clear();
        list.addAll(images);
        diffResult.dispatchUpdatesTo(pictureListAdapter);
    }

    /**
     * 预览页修改了某张图片后，强制只重新绑定这一条，让缩略图立即刷新
     */
    public void notifyImageChanged(String path) {
        if (path == null || pictureListAdapter == null || list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            if (path.equals(list.get(i).getPath())) {
                pictureListAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    /**
     * 比较新旧图片列表：id / 路径判断是否同一条，时间、大小、尺寸判断内容是否变化
     */
    private static class MediaDiffCallback extends DiffUtil.Callback {

        private final List<Multimedia> oldList;
        private final List<Multimedia> newList;

        MediaDiffCallback(List<Multimedia> oldList, List<Multimedia> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Multimedia oldItem = oldList.get(oldItemPosition);
            Multimedia newItem = newList.get(newItemPosition);
            if (oldItem.getId() != 0 && oldItem.getId() == newItem.getId()) {
                return true;
            }
            return oldItem.getPath() != null && oldItem.getPath().equals(newItem.getPath());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Multimedia oldItem = oldList.get(oldItemPosition);
            Multimedia newItem = newList.get(newItemPosition);
            return equals(oldItem.getPath(), newItem.getPath())
                    && oldItem.getDateTaken() == newItem.getDateTaken()
                    && oldItem.getSize() == newItem.getSize()
                    && oldItem.getWidth() == newItem.getWidth()
                    && oldItem.getHeight() == newItem.getHeight();
        }

        private static boolean equals(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    @Override
    public void onRightEvent(int pos, int groupPos) {
        isShowBottomBtn = !isShowBottomBtn;
        showOrHideBottomBtn(isShowBottomBtn);
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

    public boolean hideBottomBtn() {
        if(layoutBottomBtn.getVisibility() == View.GONE){
            return false;
        }
        if(layoutBottomBtn !=null){
            showOrHideBottomBtn(false);
        }
        return true;
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
                Multimedia picture = list.get(pos);
                picture.setSelected(!picture.isSelected());
                list.set(pos, picture);
                pictureListAdapter.notifyItemChanged(pos);
            } else if (shiftPressed) {
                // Shift 单击：选中上次锚点到当前位置之间的所有项（范围选择）
                int anchor = (anchorPos >= 0 && anchorPos < list.size()) ? anchorPos : pos;
                int start = Math.min(anchor, pos);
                int end = Math.max(anchor, pos);
                boolean changed = false;
                for (int i = start; i <= end; i++) {
                    Multimedia picture = list.get(i);
                    if (!picture.isSelected()) {
                        picture.setSelected(true);
                        list.set(i, picture);
                        changed = true;
                    }
                }
                if (changed) {
                    pictureListAdapter.notifyItemRangeChanged(start, end - start + 1);
                }
            } else {
                // 普通单击：取消其它所有选择，只选中当前项
                boolean changed = false;
                for (int i = 0; i < list.size(); i++) {
                    Multimedia picture = list.get(i);
                    boolean shouldSelect = (i == pos);
                    if (picture.isSelected() != shouldSelect) {
                        picture.setSelected(shouldSelect);
                        list.set(i, picture);
                        changed = true;
                    }
                }
                if (changed) {
                    pictureListAdapter.notifyDataSetChanged();
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
            Multimedia picture = list.get(i);
            if (picture.isSelected()) {
                hasSelected = true;
                if (!FavoriteStore.isFavorite(context, picture.getPath())) {
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
            Multimedia picture = list.get(i);
            if (picture.isSelected()) {
                picture.setSelected(false);
                list.set(i, picture);
            }
        }
        pictureListAdapter.notifyDataSetChanged();
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
    public void onJumpEvent(Multimedia picture) {
        SPUtils.putUserInfo(context, "curPicPath", picture.getPath());
        // 双击图片：以覆盖层方式打开预览，列表自身的 View / 滚动位置保持不变
        try {
            PicturePreviewFragment.open(baseFragment, picture);
        } catch (Exception e) {
            // 兜底：不在 Fragment 容器中时，仍然用 Activity 打开
            e.printStackTrace();
            Intent intent = new Intent();
            intent.putExtra(PicturePreviewFragment.ARG_PICTURE, picture);
            intent.setClass(context, PicturePreviewActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            baseFragment.getActivity().startActivity(intent);
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
                } else if (size > 9) {
                    baseFragment.showShortToast(
                            context.getString(R.string.can_not_choose_too_more));
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setType("image/*");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

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
                Multimedia picture = list.get(i);

                if (picture.isSelected()) {
                    delList.add(picture);
                } else {
                    tempList.add(picture);
                }
            }

            if (delList == null || delList.size() < 1) {
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
                            deleteImage();
                        }
                    });

            builder.setNegativeButton(R.string.cancel, null);
            builder.show();

        } else if (id == R.id.txtCollect) {

            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                Multimedia picture = list.get(i);
                if (picture.isSelected() && FavoriteStore.add(context, picture)) {
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
                Multimedia picture = list.get(i);
                picture.setSelected(isAllSelected);
                list.set(i, picture);
            }

            pictureListAdapter.notifyDataSetChanged();

            txtAllSelected.setText(
                    isAllSelected
                            ? context.getString(R.string.deselect_all)
                            : context.getString(R.string.select_all));

            Drawable drawableTop =
                    isAllSelected
                            ? context.getDrawable(R.drawable.ic_main_not_selected)
                            : context.getDrawable(R.drawable.ic_main_all_selected);

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
    public void deleteImage() {
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
            pictureListAdapter.notifyDataSetChanged();
            updateBottomBarBySelection();
        }
    }
}
