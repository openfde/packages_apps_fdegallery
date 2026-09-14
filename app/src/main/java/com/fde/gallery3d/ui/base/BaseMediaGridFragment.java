package com.fde.gallery3d.ui.base;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.VideoPlayActivity;
import com.fde.gallery3d.ui.adapter.MediaGridAdapter;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索 / 收藏 / 回收站页面的公共基类：负责九宫格、空状态和布局自适应。
 */
public abstract class BaseMediaGridFragment extends Fragment implements MediaGridAdapter.Listener {

    protected RecyclerView recyclerView;
    protected LinearLayout headerBar;
    protected TextView txtHeaderInfo;
    protected TextView btnHeaderAction;
    protected MediaGridAdapter adapter;

    protected final List<Multimedia> mediaList = new ArrayList<>();

    private View emptyView;
    private TextView txtEmpty;
    private GridLayoutManager layoutManager;
    private View.OnLayoutChangeListener layoutChangeListener;

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
        recyclerView.setAdapter(adapter);
        RecyclerScrollBinder.bind(recyclerView, scrollBar);

        btnHeaderAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHeaderAction();
            }
        });

        onGridCreated(view);
        listenWindowResize();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
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
     * 重新加载数据并刷新界面
     */
    protected void reload() {
        if (adapter == null || txtEmpty == null) {
            return;
        }
        mediaList.clear();
        loadMedia(mediaList);
        adapter.setFavoritePaths(FavoriteStore.getPaths(requireContext()));
        adapter.notifyDataSetChanged();

        if (emptyView != null) {
            emptyView.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        txtEmpty.setText(getEmptyText());
        onDataLoaded(mediaList);
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
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (media.getMediaType() == Constant.MEDIA_VIDEO) {
            intent.putExtra("video_data", media);
            intent.setClass(requireContext(), VideoPlayActivity.class);
        } else {
            intent.putExtra("picture_data", media);
            intent.setClass(requireContext(), PicturePreviewActivity.class);
        }
        startActivity(intent);
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
}
