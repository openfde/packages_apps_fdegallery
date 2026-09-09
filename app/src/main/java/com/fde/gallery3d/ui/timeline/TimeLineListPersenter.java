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
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.adapter.TimeLineListAdapter;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.MultGroup;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
//import com.fde.gallery3d.ui.ui.activity.PicturePreviewActivity;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.utils.StringUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeLineListPersenter {
    Context context;
    View view;
    RecyclerView recyclerView;
    TimeLineListPersenter timeLineListPersenter;
    TimeLineListAdapter timeLineListAdapter;
    List<Multimedia> list;

    List<MultGroup> listGroup;
    List<Multimedia> delList;
    int numberOfColumns = 3;
    GridLayoutManager gridLayoutManager;

    BaseFragment baseFragment;

    public TimeLineListPersenter(BaseFragment baseFragment, View view) {
        this.baseFragment = baseFragment;
        this.view = view;
        context = baseFragment.getActivity();
    }

    public boolean initView() {
        numberOfColumns = DeviceUtils.getShowCount(baseFragment.getActivity());
        list = new ArrayList<>();
        listGroup = new ArrayList<>();
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        CustomScrollBarView bar = (CustomScrollBarView) view.findViewById(R.id.scrollBar);
        timeLineListAdapter = new TimeLineListAdapter(context, baseFragment, listGroup, numberOfColumns);
        gridLayoutManager = new GridLayoutManager(context, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(timeLineListAdapter);
        RecyclerScrollBinder.bind(recyclerView, bar);
        listenWindowResize();
        return true;
    }

    private void listenWindowResize() {
        View root = baseFragment.getActivity().getWindow().getDecorView();
        root.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, orr, ob) -> {
                    int newW = r - l;
                    int oldW = orr - ol;
//                    if (newW != oldW) {
                        onWindowWidthChanged(newW);
//                    }
                });
    }

    private void onWindowWidthChanged(int widthPx) {
        int minItemPx = DeviceUtils.dpToPx(context, 200);
        int span = Math.max(1, widthPx / minItemPx);

        gridLayoutManager.setSpanCount(span);
        int itemSize = widthPx / span;
        timeLineListAdapter.updateItemSize(itemSize);
    }

    public void getAllMedia(Context context) {
        if (list != null) {
            list.clear();
            listGroup.clear();
        }
        list.addAll(FileUtils.getAllImages(context));
        list.addAll(FileUtils.getAllVideos(context));


        Map<String, List<Multimedia>> groupedMult = list.stream()
                .collect(Collectors.groupingBy(Multimedia::getDate));

        for (Map.Entry<String, List<Multimedia>> entry : groupedMult.entrySet()) {
//            LogTools.i("Group by date: " + entry.getKey());
            MultGroup multGroup = new MultGroup();
            List<Multimedia> tempList = new ArrayList<>();
            for (Multimedia data : entry.getValue()) {
                tempList.add(data);
            }

            Collections.sort(tempList, new Comparator<Multimedia>() {
                @Override
                public int compare(Multimedia o1, Multimedia o2) {
                    return Long.compare(o2.getDateTaken(), o1.getDateTaken());
                }
            });
            multGroup.setTitle(entry.getKey());
            multGroup.setList(tempList);
            listGroup.add(multGroup);
        }

        Collections.sort(listGroup, new Comparator<MultGroup>() {
            @Override
            public int compare(MultGroup o1, MultGroup o2) {
                return Long.compare(StringUtils.convertDateStringToLong(o2.getTitle(), "yyyy年MM月"), StringUtils.convertDateStringToLong(o1.getTitle(), "yyyy年MM月"));
            }
        });
        timeLineListAdapter.notifyDataSetChanged();
    }


//    @Override
//    public void onSelectEvent(int pos, int groupPos, boolean isSelect) {
//        LogTools.i("onSelectEvent " + pos + " ,groupPos " + groupPos);
//        try {
//            MultGroup multGroup = listGroup.get(groupPos);
//            List<Multimedia> tempList = multGroup.getList();
//            Multimedia multimedia = tempList.get(pos);
//            multimedia.setSelected(isSelect);
//            tempList.set(pos, multimedia);
//            multGroup.setList(tempList);
//            listGroup.set(groupPos, multGroup);

    /// /            timeLineListAdapter.notifyDataSetChanged();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onJumpEvent(Multimedia picture) {
//        SPUtils.putUserInfo(context,"curPicPath",picture.getPath());
//        Intent intent = new Intent();
//        intent.putExtra("picture_data", picture);
//        intent.setClass(context, PicturePreviewActivity.class);
//        baseFragment.getActivity().startActivityFromFragment(baseFragment, intent, Constant.REQUEST_DELETE_PHOTO);
//    }
    @SuppressLint("NewApi")
    public void deleteMultiMedia() {
        if (delList != null) {
            try {
                // 你的删除或修改文件的代码
                for (Multimedia multimedia : delList) {
                    if (multimedia.getMediaType() == Constant.MEDIA_PIC) {
                        FileUtils.deleteImage(context, multimedia.getPath());
                    } else {
                        FileUtils.deleteVideo(context, multimedia.getPath());
                    }
                }
                getAllMedia(context);
            } catch (RecoverableSecurityException e) {
                baseFragment.requestConfirmDialog(e);
            }
        }
    }
}
