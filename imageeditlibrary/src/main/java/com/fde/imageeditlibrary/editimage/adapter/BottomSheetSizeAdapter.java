package com.fde.imageeditlibrary.editimage.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.fde.imageeditlibrary.R;
import com.fde.imageeditlibrary.editimage.utils.Utils;

import java.util.List;

public class BottomSheetSizeAdapter extends RecyclerView.Adapter<BottomSheetSizeAdapter.BottomSheetSizeViewHolder> {
    List<String> list;
    Context context;
    ItemClick itemClick;

    int color ;

    public interface ItemClick {
        void setOnItemClick(int pos);
    }

    public BottomSheetSizeAdapter(Context context, List<String> list,int color, ItemClick itemClick) {
        this.list = list;
        this.context = context;
        this.itemClick = itemClick;
        this.color = color;
    }

    @NonNull
    @Override
    public BottomSheetSizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bottom_sheet_size, parent, false);
        BottomSheetSizeViewHolder holder = new BottomSheetSizeViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull BottomSheetSizeViewHolder holder, int position) {
        VectorDrawableCompat vectorDrawableCompat = VectorDrawableCompat.create(context.getResources(), R.drawable.icon_point, null);
        holder.imageView.setImageDrawable(vectorDrawableCompat);
        holder.imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        float w = 10;
        float h = 10;

        switch (position) {
            case 0:
                holder.txtTitle.setText(context.getString(R.string.text_minimum));
                holder.txtTitle.setTextSize(10.0f);
                w = 10;
                h = 10;
                break;
            case 1:
                holder.txtTitle.setText(context.getString(R.string.text_small));
                holder.txtTitle.setTextSize(20.0f);
                w = 20;
                h = 20;
                break;
            case 2:
                holder.txtTitle.setText(context.getString(R.string.text_medium));
                holder.txtTitle.setTextSize(30.0f);
                w = 30;
                h = 30;
                break;
            case 3:
                holder.txtTitle.setText(context.getString(R.string.text_large));
                holder.txtTitle.setTextSize(40.0f);
                w = 40;
                h = 40;
                break;
            case 4:
                holder.txtTitle.setText(context.getString(R.string.text_largest));
                holder.txtTitle.setTextSize(50.0f);
                w = 50;
                h = 50;
                break;

            default:
                holder.txtTitle.setText(context.getString(R.string.text_medium));
                holder.txtTitle.setTextSize(32.0f);
                w = 30;
                h = 30;
                break;
        }
        holder.imageView.getLayoutParams().width = Utils.ToInt(w * 1.5);
        holder.imageView.getLayoutParams().height = Utils.ToInt(h * 1.5);
        holder.imageView.requestLayout(); //

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemClick.setOnItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class BottomSheetSizeViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        RelativeLayout rootView;

        public BottomSheetSizeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            rootView = itemView.findViewById(R.id.rootView);
        }
    }
}
