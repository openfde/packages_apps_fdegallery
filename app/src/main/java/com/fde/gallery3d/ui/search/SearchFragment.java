package com.fde.gallery3d.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseMediaGridFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.MediaRepository;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.trash.TrashManager;

import java.util.List;

/**
 * 搜索结果页面，数据来自左侧搜索框。
 */
public class SearchFragment extends BaseMediaGridFragment implements SearchState.Listener {

    private String query = "";
    private List<Multimedia> allMedia;

    public SearchFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        query = SearchState.getInstance().getQuery();
    }

    @Override
    public void onResume() {
        allMedia = null;
        SearchState.getInstance().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        SearchState.getInstance().unregister(this);
    }

    @Override
    public void onSearchQueryChanged(String query) {
        this.query = query;
        reload();
    }

    @Override
    protected void loadMedia(List<Multimedia> out) {
        if (allMedia == null) {
            allMedia = MediaRepository.loadAll(requireContext());
        }
        if (TextUtils.isEmpty(query)) {
            return;
        }
        out.addAll(MediaRepository.filter(allMedia, query));
    }

    @Override
    protected String getEmptyText() {
        return getString(R.string.search_empty);
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
                    reload();
                } else if (item.getItemId() == 2) {
                    boolean moved = TrashManager.moveToTrash(requireContext(), media);
                    toast(moved
                            ? getString(R.string.moved_to_trash_count, 1)
                            : getString(R.string.move_to_trash_failed));
                    if (moved) {
                        allMedia = null;
                        reload();
                    }
                }
                return true;
            }
        });
        popupMenu.show();
    }
}
