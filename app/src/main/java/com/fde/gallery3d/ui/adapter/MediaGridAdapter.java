package com.fde.gallery3d.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 搜索 / 收藏 / 回收站通用的媒体九宫格适配器。
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.MediaViewHolder> {

    public interface Listener {
        void onMediaClick(Multimedia media, int position);

        void onMediaLongClick(View anchor, Multimedia media, int position);
    }

    private static final int PAYLOAD_SIZE = 1;

    private final Context context;
    private final List<Multimedia> list;
    private final Listener listener;
    private final Set<String> favoritePaths = new HashSet<>();

    private boolean showTitle;
    private int itemSizePx;

    public MediaGridAdapter(Context context, List<Multimedia> list, Listener listener) {
        this.context = context;
        this.list = list == null ? new ArrayList<Multimedia>() : list;
        this.listener = listener;
    }

    public void setFavoritePaths(Set<String> paths) {
        favoritePaths.clear();
        if (paths != null) {
            favoritePaths.addAll(paths);
        }
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
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
        holder.bind(media);
        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onMediaClick(media, holder.getBindingAdapterPosition());
                }
            }
        });
        holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onMediaLongClick(v, media, holder.getBindingAdapterPosition());
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MediaViewHolder extends RecyclerView.ViewHolder {
        FrameLayout rootView;
        ImageView imageView;
        ImageView imgType;
        ImageView imgFavorite;
        TextView txtTitle;
        String path;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.rootView);
            imageView = itemView.findViewById(R.id.imageView);
            imgType = itemView.findViewById(R.id.imgType);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }

        void bind(Multimedia item) {
            updateSize(itemSizePx);

            final String itemPath = item.getPath();
            if (!TextUtils.isEmpty(itemPath) && !itemPath.equals(path)) {
                path = itemPath;
                if (itemPath.toLowerCase().endsWith(".flv")) {
                    Glide.with(context)
                            .load(FileUtils.getFlvBitmap(itemPath))
                            .error(R.mipmap.ic_launcher)
                            .centerCrop()
                            .into(imageView);
                } else {
                    Glide.with(context)
                            .load(itemPath)
                            .error(R.mipmap.ic_launcher)
                            .thumbnail(0.1f)
                            .dontAnimate()
                            .centerCrop()
                            .into(imageView);
                }
            }

            imgType.setVisibility(item.getMediaType() == Constant.MEDIA_VIDEO ? View.VISIBLE : View.GONE);
            imgFavorite.setVisibility(favoritePaths.contains(itemPath) ? View.VISIBLE : View.GONE);

            if (showTitle) {
                txtTitle.setVisibility(View.VISIBLE);
                String title = item.getTitle();
                if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(itemPath)) {
                    title = new File(itemPath).getName();
                }
                txtTitle.setText(title == null ? "" : title);
            } else {
                txtTitle.setVisibility(View.GONE);
            }
        }

        void updateSize(int sizePx) {
            int height = (int) (sizePx * 0.78f);
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            if (lp.width != sizePx || lp.height != height) {
                lp.width = sizePx;
                lp.height = height;
                imageView.setLayoutParams(lp);
            }
        }
    }
}
