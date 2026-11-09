package com.fde.gallery3d.ui.category;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.ai.AiCategory;
import com.fde.gallery3d.ui.ai.AiCategoryStore;
import com.fde.gallery3d.ui.ai.AiMediaClassifier;
import com.fde.gallery3d.ui.base.BaseMediaGridFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.common.MediaRepository;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.trash.TrashManager;

import java.util.Collections;
import java.util.List;

/**
 * AI 分类页面：左侧导航选中某分类后，在右侧展示该分类下的图片，
 * 列表样式与收藏页一致，未分类的图片会在后台自动识别并实时补入。
 */
public class CategoryFragment extends BaseMediaGridFragment implements AiMediaClassifier.Listener {

    private static final String ARG_CATEGORY = "category";

    private AiCategory category = AiCategory.OTHER;

    public CategoryFragment() {
    }

    public static CategoryFragment newInstance(AiCategory category) {
        CategoryFragment fragment = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = AiCategory.fromId(getArguments().getString(ARG_CATEGORY));
        }
    }

    @Override
    protected void loadMedia(List<Multimedia> out) {
        List<Multimedia> all = MediaRepository.loadAll(requireContext());
        for (Multimedia media : all) {
            if (media.getMediaType() != Constant.MEDIA_PIC) {
                continue;
            }
            if (category == AiCategoryStore.get(requireContext(), media.getPath())) {
                out.add(media);
            }
        }
    }

    @Override
    protected String getEmptyText() {
        return getString(R.string.empty_category);
    }

    @Override
    protected void onDataLoaded(List<Multimedia> data) {
        showHeader(!data.isEmpty(),
                getString(R.string.category_count, data.size()),
                null);
    }

    @Override
    public void onResume() {
        super.onResume();
        AiMediaClassifier.getInstance(requireContext()).addListener(this);
    }

    @Override
    public void onDestroyView() {
        AiMediaClassifier.getInstance(requireContext()).removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onClassified(Multimedia media, AiCategory result) {
        if (category != result || media == null || mediaList == null || adapter == null) {
            return;
        }
        if (containsPath(media.getPath())) {
            return;
        }
        mediaList.add(0, media);
        adapter.notifyItemInserted(0);
        if (txtEmpty != null) {
            txtEmpty.setText(getEmptyText());
        }
        if (emptyView != null) {
            emptyView.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        onDataLoaded(mediaList);
    }

    @Override
    public void onFinished() {
        reload();
    }

    private boolean containsPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        for (Multimedia media : mediaList) {
            if (path.equals(media.getPath())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMediaLongClick(View anchor, Multimedia media, int position) {
        if (media == null || TextUtils.isEmpty(media.getPath())) {
            return;
        }
        boolean favorite = FavoriteStore.isFavorite(requireContext(), media.getPath());
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 1, 0, favorite ? R.string.unfavorite : R.string.favorite);
        popupMenu.getMenu().add(0, 2, 1, R.string.move_to_trash);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    boolean nowFavorite = FavoriteStore.toggle(requireContext(), media);
                    toast(getString(nowFavorite ? R.string.favorite_added
                            : R.string.favorite_removed));
                    adapter.setFavoritePaths(FavoriteStore.getPaths(requireContext()));
                    // 只重新绑定当前项，刷新收藏图标，其它项的缩略图不受影响
                    if (position >= 0 && position < adapter.getItemCount()) {
                        adapter.notifyItemChanged(position);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                } else if (item.getItemId() == 2) {
                    boolean moved = false;
                    try {
                        moved = TrashManager.moveToTrash(requireContext(), media);
                    } catch (SecurityException e) {
                        // 无删除权限（如 MANAGE_EXTERNAL_STORAGE 未授权）：不会崩溃，提示失败
                        toast(getString(R.string.move_to_trash_failed));
                    }
                    toast(getString(moved ? R.string.moved_to_trash_count
                            : R.string.move_to_trash_failed, moved ? 1 : 0));
                    if (moved) {
                        // 局部移除，不整表刷新，避免其它项缩略图重复加载
                        removeMediaItems(Collections.singletonList(media));
                    }
                }
                return true;
            }
        });
        popupMenu.show();
    }
}
