package com.fde.gallery3d.ui3d.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.VideoPlayActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.utils.StringUtils;

import java.util.List;

public class TimeLineAdapter extends RecyclerView.Adapter<TimeLineAdapter.TimeLineListViewHolder> {
    Context context;
    List<Multimedia> list;

    int groupPos;
    int numberOfColumns;

    ViewEvent viewEvent;
    int itemSizePx;
    static final int PAYLOAD_SIZE = 1;

    public TimeLineAdapter(Context context, List<Multimedia> list, ViewEvent viewEvent) {
        this.context = context;
        this.list = list;
        this.viewEvent = viewEvent;
    }

    @NonNull
    @Override
    public TimeLineListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline, parent, false);
        TimeLineAdapter.TimeLineListViewHolder holder = new TimeLineAdapter.TimeLineListViewHolder(view);
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
    public void onBindViewHolder(@NonNull TimeLineListViewHolder holder,@SuppressLint("RecyclerView")  final  int position) {
        Multimedia multimedia = list.get(position);
        holder.bind(multimedia, itemSizePx);

        holder.checkBox.setVisibility(multimedia.isShowCheckbox() ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(multimedia.isSelected());
        holder.txtDate.setText(StringUtils.conversionTime(1000* multimedia.getDateTaken()));

        GestureDetector detector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true; // ⚠️ 必须返回 true
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        Multimedia multimedia = list.get(position);
                        Intent intent = new Intent();
                        if (multimedia.getMediaType() == Constant.MEDIA_PIC) {
                            SPUtils.putUserInfo(context,"curPicPath",multimedia.getPath());
                            intent.putExtra("picture_data", multimedia);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setClass(context, PicturePreviewActivity.class);
                        } else {
                            intent.putExtra("video_data", multimedia);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setClass(context, VideoPlayActivity.class);
                        }
                        context.startActivity(intent);
                        return true;
                    }
                });

        holder.rootView.setOnTouchListener((v, event) -> {
            return detector.onTouchEvent(event);
        });

//        holder.rootView.setOnContextClickListener(new View.OnContextClickListener() {
//            @Override
//            public boolean onContextClick(View view) {
////                holder.checkBox.setVisibility(View.VISIBLE);
//                viewEvent.onRightEvent(position,groupPos);
//                return false;
//            }
//        });

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                viewEvent.onSelectEvent(position,groupPos, b);
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return  list.get(position).getId();
    }

    public void updateItemSize(int sizePx) {
        if (itemSizePx != sizePx) {
            itemSizePx = sizePx;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SIZE);
        }
    }

    class TimeLineListViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        RelativeLayout rootView;

        TextView txtDate;
        CheckBox checkBox;
        String path;

        public TimeLineListViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = (RelativeLayout) itemView.findViewById(R.id.rootView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
            txtDate =(TextView) itemView.findViewById(R.id.txtDate);
        }

        void bind(Multimedia item, int sizePx) {
            updateSize(sizePx);

            if (!item.getPath().equals(path)) {
                path = item.getPath();

                if(path.toLowerCase().endsWith(".flv")){
                    Glide.with(context).load(FileUtils.getFlvBitmap(path)).into(imageView);
                }else {
                    Glide.with(context)
                            .load(item.getPath())
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .error(R.mipmap.ic_launcher)
                            .frame(1000000)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop() // 裁剪图片以适应ImageView的大小
                            .dontTransform() // 禁用任何额外的转换
                            .dontAnimate()
                            .into(imageView);
                }

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
