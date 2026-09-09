package com.fde.gallery3d.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.TimeLineActivity;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.MultGroup;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.FileUtils;

import java.io.Serializable;
import java.util.List;

public class TimeLineListAdapter extends RecyclerView.Adapter<TimeLineListAdapter.TimeLineListViewHolder> {
    Context context;
    BaseFragment baseFragment;
    List<MultGroup> list;
    int numberOfColumns;
    ViewEvent viewEvent;
    int itemSizePx;
    static final int PAYLOAD_SIZE = 1;

    public TimeLineListAdapter(Context context, BaseFragment baseFragment, List<MultGroup> list, int numberOfColumns) {
        this.context = context;
        this.list = list;
        this.numberOfColumns = numberOfColumns;
//        this.viewEvent = viewEvent;
        this.baseFragment = baseFragment;
    }

    @NonNull
    @Override
    public TimeLineListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_list, parent, false);
        TimeLineListAdapter.TimeLineListViewHolder holder = new TimeLineListAdapter.TimeLineListViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineListViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_SIZE)) {
            holder.updateSize(itemSizePx);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineListViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        MultGroup multGroup = list.get(position);
        holder.bind(multGroup, itemSizePx);


        GestureDetector detector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true; // ⚠️ 必须返回 true
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        Intent intent = new Intent();
                        intent.putExtra("title", multGroup.getTitle());
                        Bundle b = new Bundle();
                        b.putSerializable("picList", (Serializable) multGroup.getList());
                        intent.putExtras(b);
                        intent.setClass(context, TimeLineActivity.class);
                        baseFragment.getActivity().startActivityFromFragment(baseFragment,intent, Constant.ACTION_REQUEST_UPDATE);
                        return true;
                    }
                });

        holder.rootView.setOnTouchListener((v, event) -> {
            return detector.onTouchEvent(event);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public void updateItemSize(int sizePx) {
        if (itemSizePx != sizePx) {
            itemSizePx = sizePx;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SIZE);
        }
    }

    class TimeLineListViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rootView;
        ImageView imageView;
        TextView txtTitle;
        String path;

        public TimeLineListViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = (RelativeLayout) itemView.findViewById(R.id.rootView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
        }

        void bind(MultGroup item, int sizePx) {
            updateSize(sizePx);

            if (!item.getList().get(0).getPath().equals(path)) {
                path = item.getList().get(0).getPath();
                if(path.toLowerCase().endsWith(".flv")){
                    Glide.with(context).load(FileUtils.getFlvBitmap(path)).into(imageView);
                }else {
                    Glide.with(imageView)
                            .load(path)
                            .error(R.mipmap.ic_launcher)
                            .thumbnail(0.1f) // ⭐ 首帧快
                            .dontAnimate()
                            .centerCrop()
                            .into(imageView);
                }
                txtTitle.setText(item.getTitle());
            }
        }

        void updateSize(int sizePx) {
            if(imageView !=null){
                ViewGroup.LayoutParams lp = imageView.getLayoutParams();
                if (lp.width != sizePx) {
                    lp.width = sizePx;
                    lp.height =  (int) (sizePx * 0.8f);
                    imageView.setLayoutParams(lp);
                }
            }
        }
    }
}
