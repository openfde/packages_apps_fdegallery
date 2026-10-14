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
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import com.fde.imageeditlibrary.editimage.EditImageActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PicturePreviewPersenter {
    PicturePreviewFragment fragment;
    Multimedia picture;
    Context context;

    List<Multimedia> list;

    int curPos = -1;

    /** 壁纸显示方式：拉伸 */
    public static final int WALLPAPER_SCALE_STRETCH = 0;
    /** 壁纸显示方式：居中（不缩放，原图大小居中，超出部分裁掉） */
    public static final int WALLPAPER_SCALE_CENTER = 1;
    /** 壁纸显示方式：居中裁剪（等比放大填满，居中裁掉多余） */
    public static final int WALLPAPER_SCALE_CENTER_CROP = 2;
    /** 壁纸显示方式：等比适应（等比缩小完整显示，居中留黑边） */
    public static final int WALLPAPER_SCALE_FIT = 3;
    /** 壁纸显示方式：平铺（原图大小重复平铺填满） */
    public static final int WALLPAPER_SCALE_TILE = 4;


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
     * 按指定显示方式生成壁纸并设置。
     *
     * @param type      1 = 主屏幕，2 = 锁屏，其它 = 两者都设置
     * @param scaleMode {@link #WALLPAPER_SCALE_STRETCH} 等 5 种显示方式之一
     */
    public void setWallpage(final int type, final int scaleMode) {
        try {
            Multimedia pic = list.get(curPos);
            if (pic == null || TextUtils.isEmpty(pic.getPath())) {
                return;
            }
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            // 目标壁纸尺寸：优先使用系统期望的壁纸尺寸（默认即屏幕尺寸），取不到再用屏幕尺寸
            int targetW = wallpaperManager.getDesiredMinimumWidth();
            int targetH = wallpaperManager.getDesiredMinimumHeight();
            if (targetW <= 0 || targetH <= 0) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                targetW = dm.widthPixels;
                targetH = dm.heightPixels;
            }

            Bitmap src = decodeSampledBitmap(pic.getPath(), targetW, targetH);
            if (src == null) {
                fragment.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.set_wallpage_failed, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            Bitmap wallpaperBitmap = processWallpaperBitmap(src, scaleMode, targetW, targetH);
            if (wallpaperBitmap == null) {
                fragment.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.set_wallpage_failed, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            try {
                if (1 == type) {
                    wallpaperManager.setBitmap(wallpaperBitmap);
                } else if (2 == type) {
                    wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK);
                } else {
                    wallpaperManager.setBitmap(wallpaperBitmap);
                    wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK);
                }
                fragment.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.set_wallpage_success, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // WallpaperManager 内部已拷贝数据，这里可以安全回收中间生成的壁纸
                if (wallpaperBitmap != src) {
                    wallpaperBitmap.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解码图片，原图远大于目标尺寸时按 2 倍目标尺寸降采样，避免解码超大图内存溢出。
     */
    private Bitmap decodeSampledBitmap(String path, int reqW, int reqH) {
        if (TextUtils.isEmpty(path) || reqW <= 0 || reqH <= 0) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int srcW = options.outWidth;
        int srcH = options.outHeight;
        if (srcW <= 0 || srcH <= 0) {
            return null;
        }
        int inSampleSize = 1;
        while (srcW / (inSampleSize * 2) >= reqW * 2
                && srcH / (inSampleSize * 2) >= reqH * 2) {
            inSampleSize *= 2;
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * 按指定显示方式把原图处理成壁纸尺寸的位图。
     */
    private Bitmap processWallpaperBitmap(Bitmap src, int scaleMode, int targetW, int targetH) {
        if (src == null || targetW <= 0 || targetH <= 0) {
            return null;
        }
        Bitmap result;
        switch (scaleMode) {
            case WALLPAPER_SCALE_CENTER: { // 居中：不缩放，原图大小居中，超出屏幕部分被裁掉
                result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(src, (targetW - src.getWidth()) / 2f,
                        (targetH - src.getHeight()) / 2f, null);
                return result;
            }
            case WALLPAPER_SCALE_CENTER_CROP: { // 居中裁剪：等比放大填满屏幕，居中裁掉多余部分
                float scale = Math.max((float) targetW / src.getWidth(),
                        (float) targetH / src.getHeight());
                int newW = Math.round(src.getWidth() * scale);
                int newH = Math.round(src.getHeight() * scale);
                Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);
                int left = (newW - targetW) / 2;
                int top = (newH - targetH) / 2;
                result = Bitmap.createBitmap(scaled, left, top, targetW, targetH);
                if (scaled != result) {
                    scaled.recycle();
                }
                return result;
            }
            case WALLPAPER_SCALE_FIT: { // 等比适应：等比缩放完整显示，居中，两侧留黑边
                float scale = Math.min((float) targetW / src.getWidth(),
                        (float) targetH / src.getHeight());
                int newW = Math.max(1, Math.round(src.getWidth() * scale));
                int newH = Math.max(1, Math.round(src.getHeight() * scale));
                Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);
                result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(scaled, (targetW - newW) / 2f, (targetH - newH) / 2f, null);
                scaled.recycle();
                return result;
            }
            case WALLPAPER_SCALE_TILE: { // 平铺：原图大小重复平铺填满整个屏幕
                result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.drawColor(Color.BLACK);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                for (int y = 0; y < targetH; y += Math.max(1, src.getHeight())) {
                    for (int x = 0; x < targetW; x += Math.max(1, src.getWidth())) {
                        canvas.drawBitmap(src, x, y, paint);
                    }
                }
                return result;
            }
            case WALLPAPER_SCALE_STRETCH: // 拉伸：直接缩放填满屏幕（可能变形）
            default:
                return Bitmap.createScaledBitmap(src, targetW, targetH, true);
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
