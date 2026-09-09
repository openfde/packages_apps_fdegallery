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
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.utils.LogTools;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimeLineListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimeLineListFragment extends BaseFragment {
    TimeLineListPersenter timeLinePersenter;
    Context context;

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
    protected void onFragmentVisibleChange(boolean isVisible) {
        super.onFragmentVisibleChange(isVisible);
        if (isVisible) {
            LogTools.i("-----time------onFragmentVisibleChange-------");
            if (isVisible) {
                timeLinePersenter.getAllMedia(context);
            }
        }
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