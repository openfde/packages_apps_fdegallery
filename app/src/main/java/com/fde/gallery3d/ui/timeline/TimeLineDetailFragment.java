package com.fde.gallery3d.ui.timeline;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fde.gallery3d.MainActivity;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.adapter.TimeLineAdapter;
import com.fde.gallery3d.ui.base.BaseFragment;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.trash.TrashManager;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.view.CustomScrollBarView;
import com.fde.gallery3d.ui.view.RecyclerScrollBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间轴分组详情页（Fragment 版，替代原来的 TimeLineActivity）。
 * 双击时间轴分组后以 Fragment 形式内嵌在 MainActivity 中展示该分组的媒体网格，
 * 支持选中、全选、分享、移入回收站。
 */
public class TimeLineDetailFragment extends BaseFragment implements View.OnClickListener, ViewEvent {

    /** 详情页内删除过媒体、返回时需要时间轴列表刷新时的监听 Key */
    public static final String KEY_TIMELINE_CHANGED = "timeline_changed";
    public static final String KEY_CHANGED = "changed";

    Context context;
    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;
    TimeLineAdapter timeLineAdapter;
    List<Multimedia> list;
    List<Multimedia> delList;
    String title;

    View layoutBottomBtn;
    TextView txtShare;
    TextView txtDelete;
    TextView txtAllSelected;
    TextView txtCancel;
    boolean isAllSelected;
    boolean isShowBottomBtn = false;
    /** 详情页中是否真正执行了删除（移入回收站），用于返回时提示时间轴列表刷新 */
    boolean isChanged = false;

    public TimeLineDetailFragment() {
    }

    public static TimeLineDetailFragment newInstance(String title, ArrayList<Multimedia> list) {
        TimeLineDetailFragment fragment = new TimeLineDetailFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putSerializable("picList", list);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        context = getActivity();
        View view = inflater.inflate(R.layout.fragment_time_line_detail, container, false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            title = bundle.getString("title");
            list = (ArrayList<Multimedia>) bundle.getSerializable("picList");
        }
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 详情页顶部标题显示分组月份（替代原 TimeLineActivity 的 setTitle）
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setCaptionTitle(
                    title == null ? getString(R.string.menu_timeline) : title);
        }
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        CustomScrollBarView bar = view.findViewById(R.id.scrollBar);
        gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        if (list == null) {
            list = new ArrayList<>();
        }
        timeLineAdapter = new TimeLineAdapter(context, list, this);
        timeLineAdapter.setHasStableIds(true);
        recyclerView.setAdapter(timeLineAdapter);
        RecyclerScrollBinder.bind(recyclerView, bar);
        listenWindowResize();

        layoutBottomBtn = view.findViewById(R.id.layoutBottom);
        txtShare = view.findViewById(R.id.txtShare);
        txtDelete = view.findViewById(R.id.txtDelete);
        txtAllSelected = view.findViewById(R.id.txtAllSelected);
        txtCancel = view.findViewById(R.id.txtCancel);
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
    }

    private void listenWindowResize() {
        View root = getActivity().getWindow().getDecorView();
        root.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, orr, ob) -> {
                    int newW = r - l;
                    int oldW = orr - ol;
                    if (newW != oldW) {
                        onWindowWidthChanged(newW);
                    }
                });
        // Fragment 视图创建时窗口已完成首次布局，OnLayoutChangeListener 不会触发首帧回调，
        // 这里立即按当前窗口宽度计算一次，避免首帧 itemSize 为 0 导致列表空白。
        onWindowWidthChanged(root.getWidth());
    }

    private void onWindowWidthChanged(int widthPx) {
        if (widthPx <= 0 || gridLayoutManager == null || timeLineAdapter == null) {
            return;
        }
        int minItemPx = DeviceUtils.dpToPx(context, 100);
        int span = Math.max(1, widthPx / minItemPx);
        gridLayoutManager.setSpanCount(span);
        int itemSize = widthPx / span;
        timeLineAdapter.updateItemSize(itemSize);
    }

    public void showOrHideBottomBtn(boolean isShow) {
        isShowBottomBtn = isShow;

        try {
            layoutBottomBtn.setVisibility(isShowBottomBtn ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isShow) {
            DeviceUtils.setBottomMargin(recyclerView, DeviceUtils.dpToPx(context, 0));
        } else {
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
                                        Constant.getProviderAuthority(context),
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

            updateBottomBarBySelection();

        } else if (id == R.id.txtCancel) {

            clearAllSelection();
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
            if (pos >= 0 && pos < list.size()) {
                Multimedia picture = list.get(pos);
                picture.setSelected(isSelect);
                list.set(pos, picture);
                timeLineAdapter.notifyItemChanged(pos);
            }
            updateBottomBarBySelection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBottomBarBySelection() {
        boolean anySelected = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isSelected()) {
                anySelected = true;
                break;
            }
        }
        if (anySelected != isShowBottomBtn) {
            showOrHideBottomBtn(anySelected);
        }
    }

    private void clearAllSelection() {
        isAllSelected = false;
        for (int i = 0; i < list.size(); i++) {
            Multimedia picture = list.get(i);
            if (picture.isSelected()) {
                picture.setSelected(false);
                list.set(i, picture);
            }
        }
        timeLineAdapter.notifyDataSetChanged();
        txtAllSelected.setText(context.getString(R.string.select_all));
        Drawable drawableTop = context.getDrawable(R.drawable.ic_main_all_selected);
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
        showOrHideBottomBtn(false);
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
        if (delList != null && !delList.isEmpty()) {
            // 移入回收站，代替直接删除
            int count = TrashManager.moveToTrash(context, delList);
            if (count > 0) {
                isChanged = true;
                showShortToast(getString(R.string.moved_to_trash_count, count));
            } else {
                showShortToast(getString(R.string.move_to_trash_failed));
            }
            timeLineAdapter.notifyDataSetChanged();
            updateBottomBarBySelection();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        deleteImage();
    }

    @Override
    public void onDestroyView() {
        // 详情页内真正删除过媒体时，通知时间轴列表刷新
        if (isChanged) {
            Bundle result = new Bundle();
            result.putBoolean(KEY_CHANGED, true);
            FragmentManager fragmentManager = getParentFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.setFragmentResult(KEY_TIMELINE_CHANGED, result);
            }
        }
        super.onDestroyView();
    }
}
