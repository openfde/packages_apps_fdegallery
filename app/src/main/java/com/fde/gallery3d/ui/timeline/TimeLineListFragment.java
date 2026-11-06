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
import android.os.Bundle;
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
import com.fde.gallery3d.ui.utils.LogTools;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimeLineListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimeLineListFragment extends BaseFragment {
    TimeLineListPersenter timeLinePersenter;
    Context context;

    /**
     * 缓存的时间轴数据：Fragment 实例在视图因左侧导航切换被销毁后仍然保留。
     * 重新进入时间轴页时复用缓存，不再重复查询媒体库、重复加载缩略图。
     */
    private final List<Multimedia> cachedList = new ArrayList<>();
    private final List<MultGroup> cachedGroup = new ArrayList<>();
    private boolean isDataLoaded = false;

    public List<Multimedia> getCachedList() {
        return cachedList;
    }

    public List<MultGroup> getCachedGroup() {
        return cachedGroup;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);
        timeLinePersenter = new TimeLineListPersenter(this, view);
        timeLinePersenter.initView();
        return view;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogTools.i("onActivityResult requestCode: "+requestCode +" ,resultCode:  "+resultCode);
        if(requestCode == Constant.REQUEST_DELETE_PHOTO ||requestCode == Constant.ACTION_REQUEST_UPDATE){
            timeLinePersenter.getAllMedia(context);
        }else {
            timeLinePersenter.deleteMultiMedia();
        }
    }
}