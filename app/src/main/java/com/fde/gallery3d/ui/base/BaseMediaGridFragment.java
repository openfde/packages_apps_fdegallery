package com.fde.gallery3d.ui.base;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.VideoPlayActivity;
import com.fde.gallery3d.ui.adapter.MediaGridAdapter;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;
import com.fde.gallery3d.ui.trash.TrashManager;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 搜索 / 收藏 / 回收站 / 智能图册分类页面的公共基类：负责九宫格、空状态和布局自适应。
 * <p>
 * 各页面可通过 {@link #selectionEnabled} 与 {@link #gridMode} 开启“单击选中 + 底部操作栏”：
 * <ul>
 *     <li>NORMAL（智能图册等）：分享 / 收藏 / 移入回收站 / 全选 / 取消</li>
 *     <li>FAVORITE（收藏）：分享 / 取消收藏 / 移入回收站 / 全选 / 取消</li>
 *     <li>TRASH（回收站）：全选 / 彻底删除 / 取消</li>
 * </ul>
 */
public abstract class BaseMediaGridFragment extends Fragment
        implements MediaGridAdapter.Listener, View.OnClickListener {

    /** 底部操作栏按钮配置。 */
    public enum GridMode {
        /** 智能图册等普通列表：分享 / 收藏 / 移入回收站 / 全选 / 取消 */
        NORMAL,
        /** 收藏列表：分享 / 取消收藏 / 移入回收站 / 全选 / 取消 */
        FAVORITE,
        /** 回收站列表：全选 / 彻底删除 / 取消 */
        TRASH
    }

    /** 是否启用“单击条目即选中”（默认关闭，如搜索页保持双击预览）。 */
    protected boolean selectionEnabled = false;

    /** 底部操作栏按钮模式。 */
    protected GridMode gridMode = GridMode.NORMAL;

    protected RecyclerView recyclerView;
    protected LinearLayout headerBar;
    protected TextView txtHeaderInfo;
    protected TextView btnHeaderAction;
    protected MediaGridAdapter adapter;

    protected final List<Multimedia> mediaList = new ArrayList<>();

    protected View emptyView;
    protected TextView txtEmpty;
    private GridLayoutManager layoutManager;
    private View.OnLayoutChangeListener layoutChangeListener;

    // 底部操作栏
    private View layoutBottomBtn;
    private TextView txtShare;
    private TextView txtDelete;
    private TextView txtAllSelected;
    private TextView txtCancel;
    private TextView txtCollect;
    private boolean isShowBottomBtn;

    /** Shift 范围选择的锚点（最后一次普通 / Ctrl 单击的位置，越界时取当前选中项）。 */
    private int anchorPos = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_grid, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        CustomScrollBarView scrollBar = view.findViewById(R.id.scrollBar);
        emptyView = view.findViewById(R.id.emptyView);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        headerBar = view.findViewById(R.id.headerBar);
        txtHeaderInfo = view.findViewById(R.id.txtHeaderInfo);
        btnHeaderAction = view.findViewById(R.id.btnHeaderAction);

        layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MediaGridAdapter(requireContext(), mediaList, this);
        // 刷新统一走 DiffUtil 局部更新，关闭默认增删/移动动画，
        // 避免窗口宽度或列数变化时整屏条目来回移动造成抖动。
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
        RecyclerScrollBinder.bind(recyclerView, scrollBar);

        btnHeaderAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHeaderAction();
            }
        });

        initBottomBar(view);
        configureBottomBar();

        onGridCreated(view);
        listenWindowResize();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    /**
     * 重新加载数据并刷新界面。
     * 用 DiffUtil 只更新真正变化的条目：未变化的项不会重新绑定，
     * 因此从其它页面切换回来 / 重复刷新不会重复加载缩略图。
     */
    protected void reload() {
        if (adapter == null || txtEmpty == null) {
            return;
        }
        List<Multimedia> fresh = new ArrayList<>();
        loadMedia(fresh);
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new MediaGridDiffCallback(mediaList, fresh), true);
        mediaList.clear();
        mediaList.addAll(fresh);
        diffResult.dispatchUpdatesTo(adapter);
        updateEmptyAndHeader();
    }

    /**
     * 局部删除：只移除目标项，后面的项向前挪一位，前面的项保持不变，不整表刷新。
     *
     * @return 实际移除的条数
     */
    protected int removeMediaItems(List<Multimedia> toRemove) {
        if (adapter == null || toRemove == null || toRemove.isEmpty()) {
            return 0;
        }
        Set<String> removePaths = new HashSet<>();
        for (Multimedia media : toRemove) {
            if (media != null && !TextUtils.isEmpty(media.getPath())) {
                removePaths.add(media.getPath());
            }
        }
        if (removePaths.isEmpty()) {
            return 0;
        }
        List<Integer> removeIndexes = new ArrayList<>();
        for (int i = 0; i < mediaList.size(); i++) {
            if (removePaths.contains(mediaList.get(i).getPath())) {
                removeIndexes.add(i);
            }
        }
        // 从后往前删，索引不会错位
        Collections.sort(removeIndexes, Collections.reverseOrder());
        for (int index : removeIndexes) {
            mediaList.remove(index);
            adapter.notifyItemRemoved(index);
        }
        updateEmptyAndHeader();
        return removeIndexes.size();
    }

    private void updateEmptyAndHeader() {
        if (emptyView != null) {
            emptyView.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (txtEmpty != null) {
            txtEmpty.setText(getEmptyText());
        }
        onDataLoaded(mediaList);
    }

    /**
     * 比较新旧媒体列表：id / 路径判断是否同一条，路径判断内容是否变化。
     * 内容未变化的条目不会重新绑定，缩略图得以保留。
     */
    private static class MediaGridDiffCallback extends DiffUtil.Callback {

        private final List<Multimedia> oldList;
        private final List<Multimedia> newList;

        MediaGridDiffCallback(List<Multimedia> oldList, List<Multimedia> newList) {
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
            return equals(oldItem.getPath(), newItem.getPath());
        }

        private static boolean equals(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    @Override
    public void onDestroyView() {
        if (layoutChangeListener != null && getActivity() != null) {
            getActivity().getWindow().getDecorView()
                    .removeOnLayoutChangeListener(layoutChangeListener);
            layoutChangeListener = null;
        }
        super.onDestroyView();
        adapter = null;
        recyclerView = null;
        emptyView = null;
        txtEmpty = null;
        headerBar = null;
        txtHeaderInfo = null;
        btnHeaderAction = null;
        layoutManager = null;
        layoutBottomBtn = null;
        txtShare = null;
        txtCollect = null;
        txtDelete = null;
        txtAllSelected = null;
        txtCancel = null;
    }

    private void listenWindowResize() {
        final View root = requireActivity().getWindow().getDecorView();
        layoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                applyWidth(right - left);
            }
        };
        root.addOnLayoutChangeListener(layoutChangeListener);
        applyWidth(root.getWidth());
    }

    private void applyWidth(int widthPx) {
        if (layoutManager == null || adapter == null) {
            return;
        }
        if (widthPx <= 0) {
            widthPx = getResources().getDisplayMetrics().widthPixels;
        }
        if (widthPx <= 0) {
            return;
        }
        int minItemPx = DeviceUtils.dpToPx(requireContext(), 160);
        int span = Math.max(1, widthPx / minItemPx);
        layoutManager.setSpanCount(span);
        adapter.updateItemSize(widthPx / span);
    }

    /**
     * 顶部信息栏，用于回收站显示数量和清空入口
     */
    protected void showHeader(boolean show, String info, String action) {
        if (headerBar == null) {
            return;
        }
        headerBar.setVisibility(show ? View.VISIBLE : View.GONE);
        txtHeaderInfo.setText(info == null ? "" : info);
        if (TextUtils.isEmpty(action)) {
            btnHeaderAction.setVisibility(View.GONE);
        } else {
            btnHeaderAction.setVisibility(View.VISIBLE);
            btnHeaderAction.setText(action);
        }
    }

    @Override
    public void onMediaClick(Multimedia media, int position) {
        openMedia(media);
    }

    /**
     * 默认打开图片预览 / 视频播放
     */
    protected void openMedia(Multimedia media) {
        if (media == null || media.getPath() == null) {
            return;
        }

        if (media.getMediaType() == Constant.MEDIA_VIDEO) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("video_data", media);
            intent.setClass(requireContext(), VideoPlayActivity.class);
            startActivity(intent);
        } else {
            // 覆盖层方式打开预览，返回时列表不会被重建，滚动位置和缩略图得以保留
            PicturePreviewFragment.open(this, media);
        }
    }

    protected void toast(String text) {
        if (!TextUtils.isEmpty(text) && getActivity() != null) {
            Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onGridCreated(View root) {
    }

    protected void onDataLoaded(List<Multimedia> data) {
    }

    protected void onHeaderAction() {
    }

    protected abstract void loadMedia(List<Multimedia> out);

    protected abstract String getEmptyText();

    // ---------------------------------------------------------------------
    // 选中 + 底部操作栏
    // ---------------------------------------------------------------------

    private void initBottomBar(View root) {
        layoutBottomBtn = root.findViewById(R.id.layoutBottom);
        txtShare = root.findViewById(R.id.txtShare);
        txtCollect = root.findViewById(R.id.txtCollect);
        txtDelete = root.findViewById(R.id.txtDelete);
        txtAllSelected = root.findViewById(R.id.txtAllSelected);
        txtCancel = root.findViewById(R.id.txtCancel);
        if (txtShare != null) {
            txtShare.setOnClickListener(this);
        }
        if (txtCollect != null) {
            txtCollect.setOnClickListener(this);
        }
        if (txtDelete != null) {
            txtDelete.setOnClickListener(this);
        }
        if (txtAllSelected != null) {
            txtAllSelected.setOnClickListener(this);
        }
        if (txtCancel != null) {
            txtCancel.setOnClickListener(this);
        }
    }

    /**
     * 按页面模式决定底部栏显示哪些按钮（回收站隐藏分享 / 收藏）。
     */
    private void configureBottomBar() {
        if (txtShare == null || txtCollect == null) {
            return;
        }
        boolean isTrash = gridMode == GridMode.TRASH;
        txtShare.setVisibility(isTrash ? View.GONE : View.VISIBLE);
        txtCollect.setVisibility(isTrash ? View.GONE : View.VISIBLE);
        if (gridMode == GridMode.FAVORITE) {
            // 收藏页只提供“取消收藏”
            txtCollect.setText(R.string.unfavorite);
            setCollectIcon(true);
        } else {
            txtCollect.setText(R.string.favorite);
            setCollectIcon(false);
        }
    }

    /**
     * 单击条目：与图片列表一致，普通单击选中当前项、Ctrl 单击取反、Shift 范围选择。
     */
    @Override
    public void onMediaSingleTap(Multimedia media, int position,
                                 boolean ctrlPressed, boolean shiftPressed) {
        if (!selectionEnabled || mediaList == null
                || position < 0 || position >= mediaList.size() || adapter == null) {
            return;
        }
        if (ctrlPressed) {
            // Ctrl 单击：只取反当前项，不影响其它已选项
            Multimedia picture = mediaList.get(position);
            picture.setSelected(!picture.isSelected());
            adapter.notifyItemChanged(position);
        } else if (shiftPressed) {
            // Shift 单击：选中上次锚点到当前位置之间的所有项（范围选择）
            int anchor = (anchorPos >= 0 && anchorPos < mediaList.size()) ? anchorPos : position;
            int start = Math.min(anchor, position);
            int end = Math.max(anchor, position);
            boolean changed = false;
            for (int i = start; i <= end; i++) {
                if (!mediaList.get(i).isSelected()) {
                    mediaList.get(i).setSelected(true);
                    changed = true;
                }
            }
            if (changed) {
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        } else {
            // 普通单击：取消其它所有选择，只选中当前项
            boolean changed = false;
            for (int i = 0; i < mediaList.size(); i++) {
                boolean shouldSelect = (i == position);
                if (mediaList.get(i).isSelected() != shouldSelect) {
                    mediaList.get(i).setSelected(shouldSelect);
                    changed = true;
                }
            }
            if (changed) {
                adapter.notifyDataSetChanged();
            }
        }
        // 普通单击与 Ctrl 单击会重置 Shift 锚点，Shift 选择不移动锚点
        if (!shiftPressed) {
            anchorPos = position;
        }
        updateBottomBarBySelection();
    }

    protected List<Multimedia> getSelectedMedias() {
        List<Multimedia> selected = new ArrayList<>();
        if (mediaList == null) {
            return selected;
        }
        for (Multimedia media : mediaList) {
            if (media.isSelected()) {
                selected.add(media);
            }
        }
        return selected;
    }

    private void updateBottomBarBySelection() {
        boolean anySelected = false;
        boolean allSelected = !mediaList.isEmpty();
        for (Multimedia media : mediaList) {
            if (media.isSelected()) {
                anySelected = true;
            } else {
                allSelected = false;
            }
        }
        if (anySelected != isShowBottomBtn) {
            showOrHideBottomBtn(anySelected);
        }
        if (layoutBottomBtn != null && layoutBottomBtn.getVisibility() == View.VISIBLE) {
            updateAllSelectedButton(allSelected);
            updateCollectButtonState();
        }
    }

    private void showOrHideBottomBtn(boolean show) {
        isShowBottomBtn = show;
        if (layoutBottomBtn != null) {
            layoutBottomBtn.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean areAllSelected() {
        if (mediaList.isEmpty()) {
            return false;
        }
        for (Multimedia media : mediaList) {
            if (!media.isSelected()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 全选 / 全不选按钮的文案与图标
     */
    private void updateAllSelectedButton(boolean allSelected) {
        if (txtAllSelected == null) {
            return;
        }
        txtAllSelected.setText(getString(allSelected ? R.string.deselect_all : R.string.select_all));
        Drawable drawableTop = requireContext().getDrawable(
                allSelected ? R.drawable.ic_main_not_selected : R.drawable.ic_main_all_selected);
        drawableTop.setBounds(0, 0,
                drawableTop.getIntrinsicWidth(), drawableTop.getIntrinsicHeight());
        txtAllSelected.setCompoundDrawables(null, drawableTop, null, null);
    }

    /**
     * 收藏按钮状态：NORMAL 下已全部收藏时显示实心图标；收藏页固定为实心（取消收藏）。
     */
    private void updateCollectButtonState() {
        if (txtCollect == null) {
            return;
        }
        if (gridMode == GridMode.TRASH) {
            return;
        }
        if (gridMode == GridMode.FAVORITE) {
            setCollectIcon(true);
            return;
        }
        boolean hasSelected = false;
        boolean allFavorite = true;
        for (Multimedia media : mediaList) {
            if (media.isSelected()) {
                hasSelected = true;
                if (!FavoriteStore.isFavorite(requireContext(), media.getPath())) {
                    allFavorite = false;
                    break;
                }
            }
        }
        setCollectIcon(hasSelected && allFavorite);
    }

    private void setCollectIcon(boolean filled) {
        if (txtCollect == null) {
            return;
        }
        Drawable drawableTop = requireContext().getDrawable(
                filled ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        drawableTop.setBounds(0, 0,
                drawableTop.getIntrinsicWidth(), drawableTop.getIntrinsicHeight());
        txtCollect.setCompoundDrawables(null, drawableTop, null, null);
    }

    /**
     * 移除列表中的已删除项，并收起底部操作栏。
     */
    protected void removeSelectedMedias(List<Multimedia> removed) {
        if (removed != null && !removed.isEmpty()) {
            removeMediaItems(removed);
        }
        clearAllSelection();
    }

    protected void clearAllSelection() {
        if (!mediaList.isEmpty()) {
            for (Multimedia media : mediaList) {
                media.setSelected(false);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        isShowBottomBtn = false;
        if (layoutBottomBtn != null) {
            layoutBottomBtn.setVisibility(View.GONE);
        }
        updateAllSelectedButton(false);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.txtShare) {
            onShareClicked();
        } else if (id == R.id.txtCollect) {
            onCollectClicked();
        } else if (id == R.id.txtDelete) {
            onDeleteClicked();
        } else if (id == R.id.txtAllSelected) {
            onSelectAllClicked();
        } else if (id == R.id.txtCancel) {
            clearAllSelection();
        }
    }

    protected void onShareClicked() {
        try {
            ArrayList<Uri> imageUris = new ArrayList<>();
            for (Multimedia media : mediaList) {
                if (media.isSelected()) {
                    imageUris.add(FileProvider.getUriForFile(
                            requireContext(),
                            Constant.getProviderAuthority(requireContext()),
                            new File(media.getPath())));
                }
            }
            int size = imageUris.size();
            if (size < 1) {
                toast(getString(R.string.can_not_choose_empty));
                return;
            } else if (size > 9) {
                toast(getString(R.string.can_not_choose_too_more));
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setType("image/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收藏按钮：NORMAL 收藏选中项；FAVORITE 取消收藏选中项。
     */
    protected void onCollectClicked() {
        List<Multimedia> selected = getSelectedMedias();
        if (selected.isEmpty()) {
            toast(getString(R.string.can_not_choose_empty));
            return;
        }
        if (gridMode == GridMode.FAVORITE) {
            int count = 0;
            for (Multimedia media : selected) {
                if (FavoriteStore.remove(requireContext(), media.getPath())) {
                    count++;
                }
            }
            toast(getString(R.string.favorite_removed_count, count));
            removeSelectedMedias(selected);
        } else {
            int count = 0;
            for (Multimedia media : selected) {
                if (FavoriteStore.add(requireContext(), media)) {
                    count++;
                }
            }
            toast(getString(R.string.favorite_added_count, count));
            updateCollectButtonState();
        }
    }

    /**
     * 删除按钮：NORMAL / FAVORITE 移入回收站；TRASH 子类覆盖为彻底删除。
     */
    protected void onDeleteClicked() {
        final List<Multimedia> selected = getSelectedMedias();
        if (selected.isEmpty()) {
            toast(getString(R.string.can_not_choose_empty));
            return;
        }
        new AlertDialog.Builder(requireContext(),
                com.fde.imageeditlibrary.R.style.RoundedAlertDialog)
                .setTitle(R.string.is_move_to_trash)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performMoveToTrash(selected);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performMoveToTrash(List<Multimedia> selected) {
        List<Multimedia> removed = new ArrayList<>();
        int count = 0;
        int failed = 0;
        for (Multimedia media : selected) {
            try {
                if (TrashManager.moveToTrash(requireContext(), media)) {
                    count++;
                    removed.add(media);
                } else {
                    failed++;
                }
            } catch (SecurityException e) {
                // 无删除权限（如 MANAGE_EXTERNAL_STORAGE 未授权）：不崩溃，提示失败
                failed++;
            }
        }
        toast(getString(failed == 0
                ? R.string.moved_to_trash_count : R.string.move_to_trash_failed, count));
        removeSelectedMedias(removed);
    }

    /**
     * 全选按钮：切到全选 / 取消全选。
     */
    private void onSelectAllClicked() {
        boolean target = !areAllSelected();
        for (Multimedia media : mediaList) {
            media.setSelected(target);
        }
        adapter.notifyDataSetChanged();
        updateBottomBarBySelection();
    }
}
