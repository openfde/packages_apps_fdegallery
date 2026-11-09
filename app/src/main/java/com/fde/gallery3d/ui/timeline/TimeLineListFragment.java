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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.MultGroup;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;
import com.fde.gallery3d.ui.utils.LogTools;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间轴列表（仿小米相册）：顶部日期标题 + 该日期下所有图片网格，单层展示。
 * 不再需要二级 TimeLineDetailFragment 详情页，交互与图片页 PictureListFragment 一致。
 */
public class TimeLineListFragment extends BaseFragment {
    TimeLineListPersenter timeLinePersenter;
    Context context;
    ContentObserver mediaObserver;

    /**
     * 缓存的时间轴数据：Fragment 实例在视图因左侧导航切换被销毁后仍然保留。
     * 重新进入时间轴页时复用缓存，不再重复查询媒体库、重复加载缩略图。
     */
    private final List<Multimedia> cachedList = new ArrayList<>();
    private final List<MultGroup> cachedGroup = new ArrayList<>();
    /** 摊平后的列表：MultGroup(日期标题) / Multimedia(媒体)，直接交给适配器渲染 */
    private final List<Object> cachedItems = new ArrayList<>();
    private boolean isDataLoaded = false;

    public List<Multimedia> getCachedList() {
        return cachedList;
    }

    public List<MultGroup> getCachedGroup() {
        return cachedGroup;
    }

    public List<Object> getCachedItems() {
        return cachedItems;
    }

    public TimeLineListFragment() {
    }

    public static TimeLineListFragment newInstance(String param1, String param2) {
        TimeLineListFragment fragment = new TimeLineListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);
        timeLinePersenter = new TimeLineListPersenter(this, view);
        timeLinePersenter.initView();

        if (Build.VERSION.SDK_INT >= 23 && context != null) {
            int iGetPerm = context.checkSelfPermission(
                    "android.permission.READ_EXTERNAL_STORAGE");
            if (iGetPerm == -1) {
                requestPermissions(
                        new String[]{
                                "android.permission.WRITE_EXTERNAL_STORAGE",
                                "android.permission.READ_EXTERNAL_STORAGE",
                                "android.permission.MANAGE_EXTERNAL_STORAGE"},
                        888);
            }
        }

        if (context != null) {
            context.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mediaObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            if (timeLinePersenter != null) {
                                timeLinePersenter.getAllMedia(context);
                            }
                        }
                    }
            );
        }

        // 预览页覆盖在列表上方，退出时只有真正修改过的图片才需要刷新
        getActivity().getSupportFragmentManager().setFragmentResultListener(
                PicturePreviewFragment.RESULT_PREVIEW_CHANGED,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (timeLinePersenter != null) {
                        timeLinePersenter.getAllMedia(context);
                        timeLinePersenter.notifyImageChanged(
                                result.getString(PicturePreviewFragment.KEY_CHANGED_PATH));
                    }
                });

        return view;
    }

    @Override
    public void onDestroyView() {
        if (mediaObserver != null) {
            try {
                context.getContentResolver().unregisterContentObserver(mediaObserver);
            } catch (Exception ignored) {
            }
            mediaObserver = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 数据只在首次进入时加载；从左侧导航切换回时间轴页时复用缓存数据，
        // 避免重复查询媒体库、重复加载缩略图
        if (timeLinePersenter != null && !isDataLoaded) {
            isDataLoaded = true;
            context = getActivity();
            timeLinePersenter.getAllMedia(context);
        }
    }

    @Override
    protected void onFragmentVisibleChange(boolean isVisible) {
        super.onFragmentVisibleChange(isVisible);
        // 不在这里刷新：避免与 onResume 重复触发整表刷新导致缩略图反复加载
    }

    @Override
    protected void onFragmentFirstVisible() {
        super.onFragmentFirstVisible();
    }

    public boolean hideBottomBtn() {
        if (timeLinePersenter != null) {
            return timeLinePersenter.hideBottomBtn();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogTools.i("onActivityResult requestCode: " + requestCode + " ,resultCode:  " + resultCode);
        if (requestCode == Constant.REQUEST_DELETE_PHOTO
                || requestCode == Constant.ACTION_REQUEST_UPDATE) {
            timeLinePersenter.getAllMedia(context);
        } else {
            timeLinePersenter.deleteMultiMedia();
        }
    }
}
