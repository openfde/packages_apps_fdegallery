package com.fde.gallery3d.ui.favorite;

import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseMediaGridFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.MediaRepository;
import com.fde.gallery3d.ui.trash.TrashManager;

import java.util.Collections;
import java.util.List;

/**
 * 收藏页面。
 */
public class FavoriteFragment extends BaseMediaGridFragment {

    public FavoriteFragment() {
    }

    @Override
    protected void loadMedia(List<Multimedia> out) {
        out.addAll(MediaRepository.loadFavorites(requireContext()));
    }

    @Override
    protected String getEmptyText() {
        return getString(R.string.empty_favorite);
    }

    @Override
    protected void onDataLoaded(List<Multimedia> data) {
        showHeader(!data.isEmpty(),
                getString(R.string.favorite_count, data.size()),
                null);
    }

    @Override
    public void onMediaLongClick(View anchor, Multimedia media, int position) {
        showItemMenu(anchor, media);
    }

    private void showItemMenu(View anchor, final Multimedia media) {
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
                    toast(getString(nowFavorite ? R.string.favorite_added : R.string.favorite_removed));
                    if (!nowFavorite) {
                        // 取消收藏后直接从列表移除，后面的项向前挪，前面的保持不动
                        removeMediaItems(Collections.singletonList(media));
                    } else {
                        reload();
                    }
                } else if (item.getItemId() == 2) {
                    boolean moved = TrashManager.moveToTrash(requireContext(), media);
                    toast(getString(moved ? R.string.moved_to_trash_count : R.string.move_to_trash_failed,
                            moved ? 1 : 0));
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
