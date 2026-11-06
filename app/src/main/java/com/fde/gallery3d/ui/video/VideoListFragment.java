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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
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
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.utils.LogTools;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoListFragment extends BaseFragment {
    VideoListPersenter videoListPersenter;
    Context context;
    List<Multimedia> list;
    boolean isInit = false ;
    ContentObserver mediaObserver;

    /**
     * 缓存的视频数据：Fragment 实例在视图因左侧导航切换被销毁后仍然保留。
     * 重新进入视频页时复用该缓存，配合 DiffUtil 只更新变化项，避免缩略图重复加载。
     */
    private List<Multimedia> cachedVideos = new ArrayList<>();

    public List<Multimedia> getCachedVideos() {
        return cachedVideos;
    }

    public VideoListFragment() {
    }

    public static VideoListFragment newInstance(String param1, String param2) {
        VideoListFragment fragment = new VideoListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        videoListPersenter = new VideoListPersenter(this, view);
        videoListPersenter.initView();

        context.getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (videoListPersenter != null) {
                                videoListPersenter.getAllVideos(context);
                            }
                        }, 1000);
                    }
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从左侧导航切换回视频页时也会刷新数据。
        // getAllVideos 内部通过 DiffUtil 只更新真正变化的条目，未变化的项不会重新绑定，
        // 因此切换页面 / 重复刷新不会导致视频缩略图被重复加载。
        if (videoListPersenter != null) {
            context = getActivity();
            videoListPersenter.getAllVideos(context);
        }
    }

    @Override
    protected void onFragmentVisibleChange(boolean isVisible) {
        super.onFragmentVisibleChange(isVisible);
        // 不在这里刷新：避免与 onResume 重复触发整表刷新导致缩略图反复加载
    }

    @Override
    public void onDestroyView() {
        if (mediaObserver != null && context != null) {
            try {
                context.getContentResolver().unregisterContentObserver(mediaObserver);
            } catch (Exception ignored) {
            }
            mediaObserver = null;
        }
        if (videoListPersenter != null) {
            videoListPersenter.release();
            videoListPersenter = null;
        }
        super.onDestroyView();
    }

    public  boolean hideBottomBtn() {
        if(videoListPersenter != null){
            return videoListPersenter.hideBottomBtn();
        }
        return  false;
    }

    @Override
    protected void onFragmentFirstVisible() {
        super.onFragmentFirstVisible();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        videoListPersenter.deleteVideo();
    }
}