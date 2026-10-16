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
package com.fde.gallery3d.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.LogTools;

import java.io.File;
import java.util.List;

public class PictureListAdapter extends RecyclerView.Adapter<PictureListAdapter.PictureListViewHolder> {
    List<Multimedia> list;
    Context context;
    int numberOfColumns;
    ViewEvent viewEvent;
    int itemSizePx;
    static final int PAYLOAD_SIZE = 1;
    public PictureListAdapter(Context context, List<Multimedia> list, int numberOfColumns, ViewEvent viewEvent) {
        this.list = list;
        this.context = context;
        this.numberOfColumns = numberOfColumns;
        this.viewEvent = viewEvent;
    }

    @NonNull
    @Override
    public PictureListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_picture_list, parent, false);
        return new PictureListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PictureListViewHolder holder,
            int position,
            @NonNull List<Object> payloads) {

        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_SIZE)) {
            holder.updateSize(itemSizePx);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureListViewHolder holder, @SuppressLint("RecyclerView")  final int position) {
//        holder.imageView.setImageURI(Uri.parse(list.get(position).getPath()));
        Multimedia picture = list.get(position);
        holder.bind(picture, itemSizePx);


        GestureDetector detector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true; // ⚠️ 必须返回 true
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        // FDE 平台没有 InputEvent.isCtrlPressed()/isShiftPressed()，
                        // 改用 metaState + KeyEvent 掩码判断修饰键
                        int metaState = e.getMetaState();
                        boolean ctrlPressed =
                                (metaState & KeyEvent.META_CTRL_MASK) != 0;
                        boolean shiftPressed =
                                (metaState & KeyEvent.META_SHIFT_MASK) != 0;
                        boolean targetState;
                        if (ctrlPressed) {
                            // Ctrl 单击：在当前选中状态上取反
                            targetState = !picture.isSelected();
                        } else {
                            // 普通单击 / Shift 单击：选中该项
                            targetState = true;
                        }
                        viewEvent.onSelectEvent(position, 0, targetState, ctrlPressed, shiftPressed);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        viewEvent.onJumpEvent(picture);
                        return true;
                    }
                });

        holder.rootView.setOnTouchListener((v, event) -> {
            return detector.onTouchEvent(event);
        });

        holder.selectBorder.setVisibility(picture.isSelected() ? View.VISIBLE : View.GONE);

        holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    @Override
    public long getItemId(int position) {
        return list.get(position).getId();
    }

    public void updateItemSize(int sizePx) {
        if (itemSizePx != sizePx) {
            itemSizePx = sizePx;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SIZE);
        }
    }

    class PictureListViewHolder extends RecyclerView.ViewHolder {
        FrameLayout rootView;
        ImageView imageView;
        View selectBorder;
        String path;
        long lastModified;

        public PictureListViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = (FrameLayout) itemView.findViewById(R.id.rootView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            selectBorder = itemView.findViewById(R.id.selectBorder);
        }

        void bind(Multimedia item, int sizePx) {
            updateSize(sizePx);

            long modified = 0L;
            try {
                modified = new File(item.getPath()).lastModified();
            } catch (Exception ignored) {
            }
            // 路径相同但文件被重新保存（例如预览页旋转）时也要重新加载
            if (!item.getPath().equals(path) || modified != lastModified) {
                path = item.getPath();
                lastModified = modified;
                Glide.with(imageView)
                        .load(item.getPath())
                        .error(R.mipmap.ic_launcher)
                        .thumbnail(0.1f) // ⭐ 首帧快
                        .dontAnimate()
                        .centerCrop()
                        .into(imageView);
            }
        }

        void updateSize(int sizePx) {
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            if (lp.width != sizePx) {
                lp.width = sizePx;
                lp.height = (int) (sizePx * 0.8f);
                imageView.setLayoutParams(lp);
            }
        }
    }

}
