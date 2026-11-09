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
package com.fde.gallery3d.ui.timeline;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.VideoPlayActivity;
import com.fde.gallery3d.ui.adapter.TimeLineListAdapter;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.MultGroup;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;
import com.fde.gallery3d.ui.trash.TrashManager;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间轴列表：按天分组（顶部日期标题 + 该日期下所有图片网格），仿小米相册。
 * 不再打开二级 TimeLineDetailFragment 详情页；交互与图片页 PictureListFragment 一致。
 */
public class TimeLineListPersenter implements ViewEvent, View.OnClickListener {
    Context context;
    View view;
    RecyclerView recyclerView;
    TimeLineListAdapter timeLineListAdapter;
    List<Multimedia> list;
    List<MultGroup> listGroup;

    /** 摊平后的列表：MultGroup(日期标题) / Multimedia(媒体)，直接交给适配器 */
    List<Object> items;
    List<Multimedia> delList;
    int numberOfColumns = 3;
    GridLayoutManager gridLayoutManager;

    View layoutBottomBtn;
    TextView txtShare;
    TextView txtDelete;
    TextView txtAllSelected;
    TextView txtCancel;
    TextView txtCollect;
    boolean isAllSelected;
    boolean isShowBottomBtn = false;

    /** Shift 范围选择的锚点 */
    private int anchorPos = -1;

    BaseFragment baseFragment;

    GridLayoutManager.SpanSizeLookup spanSizeLookup;

    public TimeLineListPersenter(BaseFragment baseFragment, View view) {
        this.baseFragment = baseFragment;
        this.view = view;
        Context ctx = baseFragment.getActivity();
        if (ctx != null) {
            context = ctx;
        }
    }

    public boolean initView() {
        // 复用 Fragment 实例保存的缓存数据，视图重建后无需重复查询媒体库
        TimeLineListFragment fragment = (TimeLineListFragment) baseFragment;
        list = fragment.getCachedList();
        listGroup = fragment.getCachedGroup();
        items = fragment.getCachedItems();

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
        txtAllSelected.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        txtCollect.setOnClickListener(this);

        timeLineListAdapter = new TimeLineListAdapter(
                context, baseFragment, items, numberOfColumns, this);
        timeLineListAdapter.setHasStableIds(true);

        gridLayoutManager = new GridLayoutManager(context, 3);
        // 日期标题占满整行，其余媒体按网格排列
        spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Object item = items.get(position);
                return item instanceof MultGroup ? gridLayoutManager.getSpanCount() : 1;
            }
        };
        gridLayoutManager.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(timeLineListAdapter);
        RecyclerScrollBinder.bind(recyclerView, bar);

        listenWindowResize();

        // 长按 / 右键切换底部按钮栏（与 PictureListFragment 一致）
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
                            if (position >= 0 && position < items.size()
                                    && items.get(position) instanceof Multimedia) {
                                onRightEvent(position, 0);
                                return true;
                            }
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
                (v, l, t, r, b, ol, ot, orr, ob) -> onWindowWidthChanged(r - l));
        onWindowWidthChanged(root.getWidth());
    }

    private void onWindowWidthChanged(int widthPx) {
        if (widthPx <= 0 || gridLayoutManager == null || timeLineListAdapter == null) {
            return;
        }
        int minItemPx = DeviceUtils.dpToPx(context, 160);
        int span = Math.max(1, widthPx / minItemPx);
        gridLayoutManager.setSpanCount(span);
        int itemSize = widthPx / span;
        timeLineListAdapter.updateItemSize(itemSize);
    }

    /**
     * 拉取全部图片与视频，按天分组并摊平成 日期标题 + 网格 的列表。
     */
    public void getAllMedia(Context context) {
        if (list == null) {
            return;
        }
        list.clear();
        listGroup.clear();
        list.addAll(FileUtils.getAllImages(context));
        list.addAll(FileUtils.getAllVideos(context));

        // 按天（yyyyMMdd）分组
        Map<String, List<Multimedia>> grouped = new HashMap<>();
        for (Multimedia m : list) {
            String dayKey = new SimpleDateFormat("yyyyMMdd")
                    .format(new Date(m.getDateTaken() * 1000L));
            List<Multimedia> dayList = grouped.get(dayKey);
            if (dayList == null) {
                dayList = new ArrayList<>();
                grouped.put(dayKey, dayList);
            }
            dayList.add(m);
        }

        // 组内按拍摄时间倒序
        for (List<Multimedia> dayList : grouped.values()) {
            Collections.sort(dayList,
                    (o1, o2) -> Long.compare(o2.getDateTaken(), o1.getDateTaken()));
        }

        // 组按日期倒序（最新在前）
        List<Map.Entry<String, List<Multimedia>>> entries =
                new ArrayList<>(grouped.entrySet());
        Collections.sort(entries, (e1, e2) -> e2.getKey().compareTo(e1.getKey()));

        items.clear();
        listGroup.clear();
        for (Map.Entry<String, List<Multimedia>> entry : entries) {
            MultGroup group = new MultGroup();
            group.setTitle(formatDayHeader(entry.getKey(), entry.getValue().get(0).getDateTaken()));
            group.setList(entry.getValue());
            listGroup.add(group);
            items.add(group);          // 日期标题（占满整行）
            items.addAll(entry.getValue()); // 该日期下所有图片
        }

        timeLineListAdapter.notifyDataSetChanged();
    }

    /**
     * 生成日期标题：今天 / 昨天 / MM月dd日。
     */
    private String formatDayHeader(String dayKey, long dateTaken) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date date = sdf.parse(dayKey);
            Calendar today = Calendar.getInstance();
            Calendar thatDay = Calendar.getInstance();
            thatDay.setTime(date);

            int todayDay = today.get(Calendar.DAY_OF_YEAR);
            int thatDayOfYear = thatDay.get(Calendar.DAY_OF_YEAR);
            int todayYear = today.get(Calendar.YEAR);
            int thatYear = thatDay.get(Calendar.YEAR);

            long diffDays = Math.abs(todayYear * 366L + todayDay
                    - (thatYear * 366L + thatDayOfYear));
            if (diffDays == 0) {
                return context.getString(R.string.today);
            } else if (diffDays == 1) {
                return context.getString(R.string.yesterday);
            }
            boolean isThisYear = todayYear == thatYear;
            SimpleDateFormat fmt = new SimpleDateFormat(isThisYear ? "MM月dd日" : "yyyy年MM月dd日");
            return fmt.format(date);
        } catch (Exception e) {
            return new SimpleDateFormat("yyyy年MM月dd日").format(new Date(dateTaken * 1000L));
        }
    }

    // ---------------------------------------------------------------------
    // ViewEvent
    // ---------------------------------------------------------------------

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
        if (isShow) {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 0));
            updateCollectButtonState();
        } else {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 50));
        }
    }

    public boolean hideBottomBtn() {
        if (layoutBottomBtn == null || layoutBottomBtn.getVisibility() == View.GONE) {
            return false;
        }
        showOrHideBottomBtn(false);
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
            if (items == null || pos < 0 || pos >= items.size()
                    || !(items.get(pos) instanceof Multimedia)) {
                return;
            }
            if (ctrlPressed) {
                Multimedia picture = (Multimedia) items.get(pos);
                picture.setSelected(!picture.isSelected());
                timeLineListAdapter.notifyItemChanged(pos);
            } else if (shiftPressed) {
                int anchor = (anchorPos >= 0 && anchorPos < items.size()) ? anchorPos : pos;
                int start = Math.min(anchor, pos);
                int end = Math.max(anchor, pos);
                boolean changed = false;
                for (int i = start; i <= end; i++) {
                    if (i >= 0 && i < items.size() && items.get(i) instanceof Multimedia) {
                        Multimedia picture = (Multimedia) items.get(i);
                        if (!picture.isSelected()) {
                            picture.setSelected(true);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    timeLineListAdapter.notifyItemRangeChanged(start, end - start + 1);
                }
            } else {
                boolean changed = false;
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof Multimedia) {
                        Multimedia picture = (Multimedia) items.get(i);
                        boolean shouldSelect = (i == pos);
                        if (picture.isSelected() != shouldSelect) {
                            picture.setSelected(shouldSelect);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    timeLineListAdapter.notifyDataSetChanged();
                }
            }
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
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Multimedia
                    && ((Multimedia) items.get(i)).isSelected()) {
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
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Multimedia) {
                Multimedia picture = (Multimedia) items.get(i);
                if (picture.isSelected()) {
                    hasSelected = true;
                    if (!FavoriteStore.isFavorite(context, picture.getPath())) {
                        allFavorite = false;
                        break;
                    }
                }
            }
        }
        boolean favorite = hasSelected && allFavorite;
        Drawable drawableTop = context.getDrawable(
                favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        drawableTop.setBounds(
                0, 0,
                drawableTop.getIntrinsicWidth(),
                drawableTop.getIntrinsicHeight());
        txtCollect.setCompoundDrawables(null, drawableTop, null, null);
    }

    private void clearAllSelection() {
        isAllSelected = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Multimedia) {
                Multimedia picture = (Multimedia) items.get(i);
                if (picture.isSelected()) {
                    picture.setSelected(false);
                }
            }
        }
        timeLineListAdapter.notifyDataSetChanged();
        txtAllSelected.setText(context.getString(R.string.select_all));
        Drawable drawableTop = context.getDrawable(R.drawable.ic_main_all_selected);
        drawableTop.setBounds(
                0, 0,
                drawableTop.getIntrinsicWidth(),
                drawableTop.getIntrinsicHeight());
        txtAllSelected.setCompoundDrawables(null, drawableTop, null, null);
        showOrHideBottomBtn(false);
    }

    @Override
    public void onJumpEvent(Multimedia picture) {
        if (picture == null || picture.getPath() == null) {
            return;
        }
        // 视频与图片分开预览：视频进入 VideoPlayActivity，图片走图片预览
        if (picture.getMediaType() == Constant.MEDIA_VIDEO) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("video_data", picture);
            intent.setClass(context, VideoPlayActivity.class);
            baseFragment.getActivity().startActivity(intent);
            return;
        }

        SPUtils.putUserInfo(context, "curPicPath", picture.getPath());
        try {
            PicturePreviewFragment.open(baseFragment, picture);
        } catch (Exception e) {
            e.printStackTrace();
            Intent intent = new Intent();
            intent.putExtra(PicturePreviewFragment.ARG_PICTURE, picture);
            intent.setClass(context, PicturePreviewActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            baseFragment.getActivity().startActivity(intent);
        }
    }

    // ---------------------------------------------------------------------
    // 底部按钮
    // ---------------------------------------------------------------------

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.txtShare) {
            try {
                ArrayList<Uri> imageUris = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof Multimedia
                            && ((Multimedia) items.get(i)).isSelected()) {
                        imageUris.add(
                                FileProvider.getUriForFile(
                                        context,
                                        Constant.getProviderAuthority(context),
                                        new File(((Multimedia) items.get(i)).getPath())
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
                        Intent.createChooser(intent, context.getString(R.string.share)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.txtDelete) {
            delList = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof Multimedia
                        && ((Multimedia) items.get(i)).isSelected()) {
                    delList.add((Multimedia) items.get(i));
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
                    (DialogInterface dialogInterface, int pos) -> deleteMultiMedia());
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        } else if (id == R.id.txtCollect) {
            int count = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof Multimedia) {
                    Multimedia picture = (Multimedia) items.get(i);
                    if (picture.isSelected() && FavoriteStore.add(context, picture)) {
                        count++;
                    }
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
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof Multimedia) {
                    Multimedia picture = (Multimedia) items.get(i);
                    picture.setSelected(isAllSelected);
                }
            }
            timeLineListAdapter.notifyDataSetChanged();
            txtAllSelected.setText(
                    isAllSelected
                            ? context.getString(R.string.deselect_all)
                            : context.getString(R.string.select_all));
            Drawable drawableTop =
                    isAllSelected
                            ? context.getDrawable(R.drawable.ic_main_not_selected)
                            : context.getDrawable(R.drawable.ic_main_all_selected);
            drawableTop.setBounds(
                    0, 0,
                    drawableTop.getIntrinsicWidth(),
                    drawableTop.getIntrinsicHeight());
            txtAllSelected.setCompoundDrawables(null, drawableTop, null, null);
            updateBottomBarBySelection();
            updateCollectButtonState();
        } else if (id == R.id.txtCancel) {
            clearAllSelection();
        }
    }

    @SuppressLint("NewApi")
    public void deleteMultiMedia() {
        if (delList != null && !delList.isEmpty()) {
            try {
                int count = TrashManager.moveToTrash(context, delList);
                if (count > 0) {
                    baseFragment.showShortToast(
                            context.getString(R.string.moved_to_trash_count, count));
                } else {
                    baseFragment.showShortToast(
                            context.getString(R.string.move_to_trash_failed));
                }
            } catch (RecoverableSecurityException e) {
                // 需要系统授权删除：文件已移回原位，弹窗请求系统授权；
                // 授权结果回到 onActivityResult 后重试。
                baseFragment.requestConfirmDialog(e);
                return;
            }
            // 重新查询以重建分组
            getAllMedia(context);
            updateBottomBarBySelection();
        }
    }

    /**
     * 预览页修改了某张图片后，只重新绑定这一项，让缩略图立即刷新
     */
    public void notifyImageChanged(String path) {
        if (path == null || timeLineListAdapter == null || items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Multimedia
                    && path.equals(((Multimedia) items.get(i)).getPath())) {
                timeLineListAdapter.notifyItemChanged(i);
                return;
            }
        }
    }
}
