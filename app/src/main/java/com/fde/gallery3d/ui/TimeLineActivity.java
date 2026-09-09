package com.fde.gallery3d.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.RecoverableSecurityException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;
import com.fde.gallery3d.ui3d.ui.adapter.TimeLineAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TimeLineActivity extends BaseActivity implements View.OnClickListener, ViewEvent {
    Context context;
    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;
    com.fde.gallery3d.ui3d.ui.adapter.TimeLineAdapter timeLineAdapter;
    List<Multimedia> list;
    List<Multimedia> delList;

    View layoutBottomBtn;
    TextView txtShare;
    TextView txtDelete;
    TextView txtAllSelected;
    TextView txtCancel;
    boolean isAllSelected;
    boolean isShowBottomBtn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line);
        context = this;
        setTitle(getIntent().getStringExtra("title"));
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            list = (ArrayList<Multimedia>) bundle.getSerializable("picList");
        }
        initView();


    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerView);
        CustomScrollBarView bar = (CustomScrollBarView) findViewById(R.id.scrollBar);
        gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        listenWindowResize();
        timeLineAdapter = new TimeLineAdapter(this, list, this);
        timeLineAdapter.setHasStableIds(true);
        recyclerView.setAdapter(timeLineAdapter);
        RecyclerScrollBinder.bind(recyclerView, bar);

        layoutBottomBtn = (View) findViewById(R.id.layoutBottom);
        txtShare = (TextView) findViewById(R.id.txtShare);
        txtDelete = (TextView) findViewById(R.id.txtDelete);
        txtAllSelected = (TextView) findViewById(R.id.txtAllSelected);
        txtCancel = (TextView) findViewById(R.id.txtCancel);
        txtShare.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtAllSelected.setOnClickListener(this);
        txtCancel.setOnClickListener(this);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            private long touchDownTime = 0;

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    touchDownTime = System.currentTimeMillis();
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    long duration = System.currentTimeMillis() - touchDownTime;
                    if (duration >= ViewConfiguration.getLongPressTimeout()) {
                        View childView = rv.findChildViewUnder(e.getX(), e.getY());
                        if (childView != null) {
                            int position = rv.getChildAdapterPosition(childView);
                            onRightEvent(position, 0);
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });


        recyclerView.setOnContextClickListener(new View.OnContextClickListener() {

            @Override
            public boolean onContextClick(View v) {
                onRightEvent(0,0);
                return false;
            }
        });
    }

    private void listenWindowResize() {
        View root = getWindow().getDecorView();
        root.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, orr, ob) -> {
                    int newW = r - l;
                    int oldW = orr - ol;
                    if (newW != oldW) {
                        onWindowWidthChanged(newW);
                    }
                });
    }

    private void onWindowWidthChanged(int widthPx) {
        int minItemPx = DeviceUtils.dpToPx(context, 100);
        int span = Math.max(1, widthPx / minItemPx);
        gridLayoutManager.setSpanCount(span);
        int itemSize = widthPx / span;
        timeLineAdapter.updateItemSize(itemSize);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if(keyCode == KeyEvent.KEYCODE_BACK){
//            if(layoutBottomBtn.getVisibility() != View.GONE){
//                showOrHideBottomBtn(false);
//                return true;
//            }
//            return super.onKeyDown(keyCode, event);
//        }else {
            return super.onKeyDown(keyCode, event);
//        }
    }


    public void showOrHideBottomBtn(boolean isShow) {
        isShowBottomBtn = isShow;

        try {
            layoutBottomBtn.setVisibility(isShowBottomBtn ? View.VISIBLE : View.GONE);
            for (int i = 0; i < list.size(); i++) {
                Multimedia picture = list.get(i);
                picture.setShowCheckbox(isShowBottomBtn);
                list.set(i, picture);
            }
            timeLineAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isShow){
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 0));
        }else {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 50));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.txtShare) {
            try {
                ArrayList<Uri> imageUris = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).isSelected()) {
                        imageUris.add(
                                FileProvider.getUriForFile(
                                        context,
                                        Constant.PKG_PROVIDER,
                                        new File(list.get(i).getPath())
                                )
                        );
                    }
                }

                int size = imageUris.size();

                if (size < 1) {
                    showShortToast(
                            context.getString(R.string.can_not_choose_empty));
                    return;
                }

                if (size > 9) {
                    showShortToast(
                            context.getString(R.string.can_not_choose_too_more));
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setType("image/*");
                intent.putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        imageUris);

                startActivity(
                        Intent.createChooser(
                                intent,
                                context.getString(R.string.share)));

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (id == R.id.txtDelete) {

            List<Multimedia> tempList = new ArrayList<>();
            delList = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                Multimedia picture = list.get(i);

                if (picture.isSelected()) {
                    delList.add(picture);
                } else {
                    tempList.add(picture);
                }
            }

            if (delList == null || delList.isEmpty()) {
                showShortToast(
                        context.getString(R.string.can_not_choose_empty));
                return;
            }

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(
                            context,
                            com.fde.imageeditlibrary.R.style.RoundedAlertDialog);

            builder.setTitle(R.string.is_delete);

            builder.setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                                DialogInterface dialogInterface,
                                int pos) {

                            list.clear();
                            list.addAll(tempList);
                            deleteImage();
                        }
                    });

            builder.setNegativeButton(R.string.cancel, null);
            builder.show();

        } else if (id == R.id.txtAllSelected) {

            isAllSelected = !isAllSelected;

            for (int i = 0; i < list.size(); i++) {
                Multimedia picture = list.get(i);
                picture.setSelected(isAllSelected);
                list.set(i, picture);
            }

            timeLineAdapter.notifyDataSetChanged();

            txtAllSelected.setText(
                    isAllSelected
                            ? context.getString(R.string.deselect_all)
                            : context.getString(R.string.select_all));

            Drawable drawableTop =
                    isAllSelected
                            ? context.getDrawable(R.drawable.ic_main_not_selected)
                            : context.getDrawable(R.drawable.ic_main_all_selected);

            drawableTop.setBounds(
                    0,
                    0,
                    drawableTop.getIntrinsicWidth(),
                    drawableTop.getIntrinsicHeight());

            txtAllSelected.setCompoundDrawables(
                    null,
                    drawableTop,
                    null,
                    null);

        } else if (id == R.id.txtCancel) {

            isShowBottomBtn = !isShowBottomBtn;
            showOrHideBottomBtn(isShowBottomBtn);
        }
    }

    @Override
    public void onRightEvent(int pos, int groupPos) {
        isShowBottomBtn = !isShowBottomBtn;
        showOrHideBottomBtn(isShowBottomBtn);
    }

    @Override
    public void onSelectEvent(int pos, int groupPos, boolean isSelect) {
        try {
            Multimedia picture = list.get(pos);
            picture.setSelected(isSelect);
            list.set(pos, picture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJumpEvent(Multimedia multimedia) {
        SPUtils.putUserInfo(context, "curPicPath", multimedia.getPath());
        Intent intent = new Intent();
        intent.putExtra("picture_data", multimedia);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, PicturePreviewActivity.class);
        startActivityForResult(intent, Constant.REQUEST_DELETE_PHOTO);
    }

    @SuppressLint("NewApi")
    public void deleteImage() {
        if (delList != null) {
            try {
                // 你的删除或修改文件的代码
                for (Multimedia picture : delList) {
                    FileUtils.deleteImage(context, picture.getPath());
                }
                timeLineAdapter.notifyDataSetChanged();
                isShowBottomBtn = !isShowBottomBtn;
                showOrHideBottomBtn(isShowBottomBtn);
            } catch (RecoverableSecurityException e) {
                requestConfirmDialog(e);
            }
        }
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        setResult(Constant.ACTION_REQUEST_UPDATE);
//        finish();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        deleteImage();
    }

}