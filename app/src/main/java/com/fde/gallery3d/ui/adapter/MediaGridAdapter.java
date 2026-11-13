package com.fde.gallery3d.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索 / 收藏 / 回收站通用的媒体九宫格适配器。
 * <p>
 * 布局与样式对齐 {@link PictureListAdapter}（item_picture_list）：
 * 仅保留图片 + 选中边框，宽高比 0.8、Glide 交叉淡入、按文件修改时间重新加载。
 * 交互保持：双击预览 / 播放，长按弹出菜单。
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.MediaViewHolder> {

    public interface Listener {
        /**
         * 单击条目（带修饰键信息，用于选中 / 范围选择）。
         */
        void onMediaSingleTap(Multimedia media, int position,
                              boolean ctrlPressed, boolean shiftPressed);

        void onMediaClick(Multimedia media, int position);

        void onMediaLongClick(View anchor, Multimedia media, int position);
    }

    private static final int PAYLOAD_SIZE = 1;

    private final Context context;
    private final List<Multimedia> list;
    private final Listener listener;

    private int itemSizePx;

    public MediaGridAdapter(Context context, List<Multimedia> list, Listener listener) {
        this.context = context;
        this.list = list == null ? new ArrayList<Multimedia>() : list;
        this.listener = listener;
    }

    public void updateItemSize(int sizePx) {
        if (itemSizePx != sizePx) {
            itemSizePx = sizePx;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SIZE);
        }
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_grid, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_SIZE)) {
            holder.updateSize(itemSizePx);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        final Multimedia media = list.get(position);
        holder.bind(media, itemSizePx);
        holder.selectBorder.setVisibility(media.isSelected() ? View.VISIBLE : View.GONE);

        GestureDetector detector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true; // ⚠️ 必须返回 true 才能收到 onDoubleTap
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        // 单击触发选中（FDE 平台用 metaState + KeyEvent 掩码判断修饰键），
                        // 是否真正进入选中态由页面（BaseMediaGridFragment）决定。
                        if (listener != null) {
                            int metaState = e.getMetaState();
                            boolean ctrlPressed =
                                    (metaState & KeyEvent.META_CTRL_MASK) != 0;
                            boolean shiftPressed =
                                    (metaState & KeyEvent.META_SHIFT_MASK) != 0;
                            listener.onMediaSingleTap(media, holder.getBindingAdapterPosition(),
                                    ctrlPressed, shiftPressed);
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (listener != null) {
                            listener.onMediaClick(media, holder.getBindingAdapterPosition());
                        }
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        // if (listener != null) {
                        //     listener.onMediaLongClick(holder.rootView, media,
                        //             holder.getBindingAdapterPosition());
                        // }
                    }
                });

        // 单击选中、双击打开预览/播放；长按弹出菜单。
        // 不用 setOnClickListener，否则单击也会触发（onSingleTapConfirmed 走不到）。
        holder.rootView.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).getId();
    }

    class MediaViewHolder extends RecyclerView.ViewHolder {
        FrameLayout rootView;
        ImageView imageView;
        View selectBorder;
        String path;
        long lastModified;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.rootView);
            imageView = itemView.findViewById(R.id.imageView);
            selectBorder = itemView.findViewById(R.id.selectBorder);
        }

        void bind(Multimedia item, int sizePx) {
            updateSize(sizePx);

            final String itemPath = item.getPath();
            if (TextUtils.isEmpty(itemPath)) {
                return;
            }
            long modified = 0L;
            try {
                modified = new File(itemPath).lastModified();
            } catch (Exception ignored) {
            }
            // 路径相同但文件被重新保存（例如预览页旋转）时也要重新加载
            if (!itemPath.equals(path) || modified != lastModified) {
                path = itemPath;
                lastModified = modified;
                if (itemPath.toLowerCase().endsWith(".flv")) {
                    Glide.with(imageView)
                            .load(FileUtils.getFlvBitmap(itemPath))
                            .error(R.mipmap.ic_launcher)
                            .transition(DrawableTransitionOptions.withCrossFade(0))
                            .centerCrop()
                            .into(imageView);
                } else {
                    Glide.with(imageView)
                            .load(itemPath)
                            .error(R.mipmap.ic_launcher)
                            .thumbnail(0.1f) // ⭐ 首帧快
                            .transition(DrawableTransitionOptions.withCrossFade(0))
                            .centerCrop()
                            .into(imageView);
                }
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
