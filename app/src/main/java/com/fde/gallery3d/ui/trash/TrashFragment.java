package com.fde.gallery3d.ui.trash;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseMediaGridFragment;
import com.fde.gallery3d.ui.bean.Multimedia;

import java.util.ArrayList;
import java.util.List;

/**
 * 回收站页面：支持恢复和彻底删除。
 */
public class TrashFragment extends BaseMediaGridFragment {

    private final List<TrashItem> items = new ArrayList<>();

    public TrashFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 单击选中 + 底部操作栏：回收站只显示 全选 / 彻底删除 / 取消
        selectionEnabled = true;
        gridMode = GridMode.TRASH;
    }

    @Override
    protected void loadMedia(List<Multimedia> out) {
        items.clear();
        items.addAll(TrashStore.getAll(requireContext()));
        for (TrashItem item : items) {
            Multimedia media = new Multimedia();
            media.setPath(item.trashPath);
            media.setTitle(item.title);
            media.setMediaType(item.mediaType);
            media.setSize(item.size);
            media.setDateTaken(item.deletedTime / 1000L);
            out.add(media);
        }
    }

    @Override
    protected String getEmptyText() {
        return getString(R.string.empty_trash);
    }

    @Override
    protected void onDataLoaded(List<Multimedia> data) {
        boolean hasData = !data.isEmpty();
        showHeader(hasData,
                getString(R.string.trash_count, data.size()),
                hasData ? getString(R.string.action_clear) : null);
    }

    @Override
    protected void onHeaderAction() {
        confirmClear();
    }

    /**
     * 回收站底部“删除”按钮：彻底删除选中的项目。
     */
    @Override
    protected void onDeleteClicked() {
        final List<Multimedia> selected = getSelectedMedias();
        if (selected.isEmpty()) {
            toast(getString(R.string.can_not_choose_empty));
            return;
        }
        new AlertDialog.Builder(
                requireContext(), com.fde.imageeditlibrary.R.style.RoundedAlertDialog)
                .setTitle(R.string.delete_forever)
                .setMessage(getString(R.string.delete_forever_count_confirm, selected.size()))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int count = 0;
                        for (Multimedia media : selected) {
                            TrashItem item = findItem(media);
                            if (item != null && TrashManager.deleteForever(requireContext(), item)) {
                                count++;
                            }
                        }
                        toast(getString(R.string.deleted_forever_count, count));
                        // 重新从存储读取，保证 items 与 mediaList 一一对应
                        reload();
                        clearAllSelection();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private TrashItem findItem(Multimedia media) {
        if (media == null) {
            return null;
        }
        for (TrashItem item : items) {
            if (item.trashPath != null && item.trashPath.equals(media.getPath())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void onMediaLongClick(View anchor, Multimedia media, int position) {
        // 双击通过基类的 openMedia() 打开预览 / 播放；
        // 长按仍然弹出恢复 / 彻底删除菜单。
        showItemDialog(position);
    }

    private TrashItem getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    private void showItemDialog(final int position) {
        final TrashItem item = getItem(position);
        if (item == null) {
            return;
        }
        String title = TextUtils.isEmpty(item.title) ? item.path : item.title;
        AlertDialog.Builder builder = new AlertDialog.Builder(
                requireContext(), com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
        builder.setTitle(title);
        builder.setItems(new CharSequence[]{
                getString(R.string.restore),
                getString(R.string.delete_forever)
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    restore(item);
                } else {
                    confirmDelete(item);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void restore(TrashItem item) {
        boolean success = TrashManager.restore(requireContext(), item);
        toast(getString(success ? R.string.restore_success : R.string.restore_failed));
        reload();
    }

    private void confirmDelete(final TrashItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                requireContext(), com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
        builder.setTitle(R.string.delete_forever);
        builder.setMessage(R.string.delete_forever_confirm);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TrashManager.deleteForever(requireContext(), item);
                reload();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void confirmClear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                requireContext(), com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
        builder.setTitle(R.string.action_clear);
        builder.setMessage(R.string.clear_trash_confirm);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TrashManager.clear(requireContext());
                reload();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
