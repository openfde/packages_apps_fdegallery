package com.fde.gallery3d.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.MultGroup;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.FileUtils;

import java.io.File;
import java.util.List;

/**
 * 时间轴分组网格：把若干月份分组摊平成一条列表，
 * 每组先渲染一个占满整行的日期标题 {@link #TYPE_HEADER}，
 * 再把该组内所有媒体按网格渲染 {@link #TYPE_PHOTO}
 * （条目样式与图片页 PictureListFragment 保持一致）。
 */
public class TimeLineListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    BaseFragment baseFragment;
    List<Object> items; // 每个元素为 MultGroup(日期标题) 或 Multimedia(具体媒体)
    int numberOfColumns;
    ViewEvent viewEvent;
    int itemSizePx;
    static final int PAYLOAD_SIZE = 1;

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_PHOTO = 1;

    public TimeLineListAdapter(Context context, BaseFragment baseFragment, List<Object> items,
                               int numberOfColumns, ViewEvent viewEvent) {
        this.context = context;
        this.baseFragment = baseFragment;
        this.items = items;
        this.numberOfColumns = numberOfColumns;
        this.viewEvent = viewEvent;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof MultGroup ? TYPE_HEADER : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_timeline_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_timeline, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (holder instanceof PhotoViewHolder && !payloads.isEmpty()
                && payloads.contains(PAYLOAD_SIZE)) {
            ((PhotoViewHolder) holder).updateSize(itemSizePx);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 @SuppressLint("RecyclerView") final int position) {
        Object item = items.get(position);
        if (item instanceof MultGroup) {
            ((HeaderViewHolder) holder).bind(((MultGroup) item).getTitle());
            return;
        }

        final Multimedia picture = (Multimedia) item;
        PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
        photoHolder.bind(picture, itemSizePx);
        photoHolder.selectBorder.setVisibility(
                picture.isSelected() ? View.VISIBLE : View.GONE);

        // 与 PictureListFragment 交互一致：单击选中（支持 Ctrl / Shift），双击预览
        GestureDetector detector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true; // ⚠️ 必须返回 true
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        int metaState = e.getMetaState();
                        boolean ctrlPressed =
                                (metaState & KeyEvent.META_CTRL_MASK) != 0;
                        boolean shiftPressed =
                                (metaState & KeyEvent.META_SHIFT_MASK) != 0;
                        boolean targetState;
                        if (ctrlPressed) {
                            targetState = !picture.isSelected();
                        } else {
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

        photoHolder.rootView.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
        photoHolder.rootView.setOnLongClickListener(v -> false);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        Object item = items.get(position);
        // Header 用负数 id 避免与照片 id 冲突；照片用其媒体 id
        return item instanceof MultGroup ? -(position + 1) : ((Multimedia) item).getId();
    }

    public void updateItemSize(int sizePx) {
        if (itemSizePx != sizePx) {
            itemSizePx = sizePx;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SIZE);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtDateTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDateTitle = itemView.findViewById(R.id.txtDateTitle);
        }

        void bind(String title) {
            txtDateTitle.setText(title);
        }
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        FrameLayout rootView;
        ImageView imageView;
        View selectBorder;
        String path;
        long lastModified;

        PhotoViewHolder(@NonNull View itemView) {
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
            if (!item.getPath().equals(path) || modified != lastModified) {
                path = item.getPath();
                lastModified = modified;
                if (path.toLowerCase().endsWith(".flv")) {
                    Glide.with(context).load(FileUtils.getFlvBitmap(path)).into(imageView);
                } else {
                    Glide.with(imageView)
                            .load(item.getPath())
                            .error(R.mipmap.ic_launcher)
                            .thumbnail(0.1f) // ⭐ 首帧快
                            .centerCrop()
                            .into(imageView);
                }
            }
        }

        void updateSize(int sizePx) {
            if (imageView != null) {
                ViewGroup.LayoutParams lp = imageView.getLayoutParams();
                if (lp.width != sizePx) {
                    lp.width = sizePx;
                    lp.height = (int) (sizePx * 0.8f);
                    imageView.setLayoutParams(lp);
                }
            }
        }
    }
}
