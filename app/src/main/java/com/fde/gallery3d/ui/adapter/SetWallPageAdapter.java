package com.fde.gallery3d.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.LogTools;

import java.io.File;
import java.util.List;

public class SetWallPageAdapter extends RecyclerView.Adapter<SetWallPageAdapter.SetWallPageHolder> {

    List<Multimedia> list;
    Context context;
    int numberOfColumns;
    ViewEvent viewEvent;

    public SetWallPageAdapter( Context context,List<Multimedia> list, int numberOfColumns, ViewEvent viewEvent) {
        this.list = list;
        this.context = context;
        this.numberOfColumns = numberOfColumns;
        this.viewEvent = viewEvent;
    }

    @NonNull
    @Override
    public SetWallPageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpage_list, parent, false);
        SetWallPageAdapter.SetWallPageHolder holder = new SetWallPageAdapter.SetWallPageHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SetWallPageHolder holder, int position) {
        Multimedia picture = list.get(position);
        Glide.with(context)
                .load(Uri.fromFile(new File(list.get(position).getPath())))
                .error(R.mipmap.ic_launcher)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewEvent.onSelectEvent(position,-1,false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull SetWallPageAdapter.SetWallPageHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.rootView.post(new Runnable() {
            @Override
            public void run() {
                if (holder.rootView.getParent() != null) {
                    int height  = DeviceUtils.getSreenHight(context);
//                    int width = ((RecyclerView) holder.rootView.getParent()).getWidth();
                    if (height != 0) {
                        // get RecyclerView width
                        // calc RecyclerView width and height
                        int size = height / 4;
                        // set item width and height
                        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
                        layoutParams.width = height / 4;
                        layoutParams.height = height / 5;
                        holder.itemView.setLayoutParams(layoutParams);
                    }
                }
            }
        });

    }

    class SetWallPageHolder extends RecyclerView.ViewHolder {
        FrameLayout rootView;
        ImageView imageView;

        public SetWallPageHolder(@NonNull View itemView) {
            super(itemView);
            rootView = (FrameLayout) itemView.findViewById(R.id.rootView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
        }
    }
}
