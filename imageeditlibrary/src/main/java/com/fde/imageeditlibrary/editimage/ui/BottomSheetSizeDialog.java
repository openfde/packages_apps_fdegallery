package com.fde.imageeditlibrary.editimage.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.imageeditlibrary.R;
import com.fde.imageeditlibrary.editimage.adapter.BottomSheetSizeAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class BottomSheetSizeDialog extends BottomSheetDialog {
    Context context;

    List<String> list;

    RecyclerView recyclerView;

    BottomSheetSizeAdapter bottomSheetSizeAdapter;

    ItemClick itemClick;
    int color ;

    public  interface ItemClick {
        void setOnItemClick(int pos);
    }
    public BottomSheetSizeDialog(@NonNull Context context,int color,ItemClick itemClick) {
//        super(context);
        super(context,R.style.BottomSheetDialog);
        this.color = color;
        this.context = context;
        this.itemClick = itemClick;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bottom_sheet_size);
//        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            list.add("item " + i);
        }

        bottomSheetSizeAdapter = new BottomSheetSizeAdapter(context, list,color, new BottomSheetSizeAdapter.ItemClick() {
            @Override
            public void setOnItemClick(int pos) {
                itemClick.setOnItemClick(pos);
                dismiss();
            }
        });
        LinearLayoutManager gridLayoutManager = new LinearLayoutManager(context);
        gridLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(bottomSheetSizeAdapter);


    }


}
