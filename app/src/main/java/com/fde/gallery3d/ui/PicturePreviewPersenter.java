/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fde.gallery3d.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.RecoverableSecurityException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;
import com.fde.gallery3d.ui.trash.TrashManager;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.utils.StringUtils;
import com.fde.gallery3d.ui.utils.WallpaperUtils;
import com.fde.imageeditlibrary.editimage.EditImageActivity;

import java.io.File;
import java.util.List;

public class PicturePreviewPersenter {
    PicturePreviewFragment fragment;
    Multimedia picture;
    Context context;

    List<Multimedia> list;

    int curPos = -1;

    /** 壁纸显示方式：拉伸（与 WallpaperUtils 共用一套逻辑） */
    public static final int WALLPAPER_SCALE_STRETCH = WallpaperUtils.WALLPAPER_SCALE_STRETCH;
    /** 壁纸显示方式：居中（不缩放，原图大小居中，超出部分裁掉） */
    public static final int WALLPAPER_SCALE_CENTER = WallpaperUtils.WALLPAPER_SCALE_CENTER;
    /** 壁纸显示方式：居中裁剪（等比放大填满，居中裁掉多余） */
    public static final int WALLPAPER_SCALE_CENTER_CROP = WallpaperUtils.WALLPAPER_SCALE_CENTER_CROP;
    /** 壁纸显示方式：等比适应（等比缩小完整显示，居中留黑边） */
    public static final int WALLPAPER_SCALE_FIT = WallpaperUtils.WALLPAPER_SCALE_FIT;
    /** 壁纸显示方式：平铺（原图大小重复平铺填满） */
    public static final int WALLPAPER_SCALE_TILE = WallpaperUtils.WALLPAPER_SCALE_TILE;


    public PicturePreviewPersenter(PicturePreviewFragment fragment, Multimedia picture) {
        this.fragment = fragment;
        context = fragment.getActivity();
        this.picture = picture;
        getPicList();
    }

    public void getPicList() {
        try {
            list = FileUtils.getAllImages(context);
            curPos = -1;
            if (picture == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                Multimedia pic = list.get(i);
                if (pic.getId() == picture.getId()) {
                    curPos = i;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public  Multimedia getCurPic() {
        try {
            Multimedia pic = list.get(curPos);
            SPUtils.putUserInfo(context, "curPicPath", pic.getPath());
            return pic;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  null;
    }

    public Multimedia getNextPic() {
        try {
            int maxLen = list.size();
            if (curPos >= 0 && curPos < (maxLen - 1)) {
                curPos++;
                Multimedia m = list.get(curPos);
                SPUtils.putUserInfo(context, "curPicPath", m.getPath());
                return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Multimedia getPrePic() {
        try {
            if (curPos > 0) {
                curPos--;
                Multimedia m = list.get(curPos);
                SPUtils.putUserInfo(context, "curPicPath", m.getPath());
                return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void showDetailsDlg() {
        try {
            Multimedia pic = list.get(curPos);
            long size = pic.getSize();
            String strSize = "";
            if(size > 0){
                strSize = "size:  " + (size / 1024) + "kb\n";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context,com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
            builder.setTitle(R.string.details);
            builder.setMessage("title:  " + pic.getTitle() + "\n"
                    + "width:  " + pic.getWidth() + "\n"
                    + "height:  " + pic.getHeight() + "\n"
                    + strSize
                    + "date:   " + StringUtils.conversionTime(1000 * pic.getDateTaken()) + "\n"
                    + "path:  " + pic.getPath() + "\n"
            );
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    public void deleteImage(Context context) {
        try {
            Multimedia pic = list.get(curPos);
            // 移入回收站，代替直接删除
            boolean moved = TrashManager.moveToTrash(context, pic);
            Toast.makeText(context,
                    context.getString(moved ? R.string.moved_to_trash_count
                            : R.string.move_to_trash_failed, moved ? 1 : 0),
                    Toast.LENGTH_SHORT).show();
            if (moved && pic != null) {
                fragment.notifyPreviewChanged(pic.getPath());
            }
            fragment.closePreview(Constant.REQUEST_DELETE_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showDelDlg() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
            builder.setTitle(R.string.is_move_to_trash);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    deleteImage(context);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


   public void editImageClick() {
        Multimedia pic = list.get(curPos);
        File outputFile = FileUtils.genEditFile();
        if (pic == null || outputFile == null) {
            return;
        }
        fragment.startEditImage(pic.getPath(), outputFile.getAbsolutePath(),
                Constant.ACTION_REQUEST_EDITIMAGE);
    }


    public void setWallpage(int type) {
        setWallpage(type, WALLPAPER_SCALE_STRETCH);
    }

    /**
     * 按指定显示方式生成壁纸并设置（复用 {@link WallpaperUtils} 的公共逻辑）。
     *
     * @param type      1 = 主屏幕，2 = 锁屏，其它 = 两者都设置
     * @param scaleMode {@link #WALLPAPER_SCALE_STRETCH} 等 5 种显示方式之一
     */
    public void setWallpage(final int type, final int scaleMode) {
        try {
            final Multimedia pic = list.get(curPos);
            if (pic == null || TextUtils.isEmpty(pic.getPath())) {
                return;
            }
            final boolean success = WallpaperUtils.setWallpaper(context, pic.getPath(), type, scaleMode);
            fragment.runOnUi(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,
                            success ? R.string.set_wallpage_success : R.string.set_wallpage_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleEditorImage(Intent data) {
        String oldPath = data.getStringExtra(EditImageActivity.FILE_PATH);
        String newFilePath = data.getStringExtra(EditImageActivity.EXTRA_OUTPUT);
        LogTools.i("oldPath "+oldPath +" ,newFilePath "+newFilePath);
        boolean isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false);

        if (isImageEdit){
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            Bitmap bitmap = BitmapFactory.decodeFile(newFilePath, options);
//            BitmapUtils.saveBitmap(context,bitmap, newFilePath);
            Toast.makeText(context, context.getString(R.string.save_path, newFilePath), Toast.LENGTH_LONG).show();
        }else{//未编辑  还是用原来的图片
            newFilePath = data.getStringExtra(EditImageActivity.FILE_PATH);;
        }
    }

}
