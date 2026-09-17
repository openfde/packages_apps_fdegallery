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
        Multimedia pic = list.get(curPos);
        Bitmap wallpaperBitmap = BitmapFactory.decodeFile(pic.getPath());
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        int w = wallpaperBitmap.getWidth();
        int h = wallpaperBitmap.getHeight();

        if (w != 0 && h != 0) {
            if (w > h) {
                wallpaperManager.suggestDesiredDimensions(w, h);
            } else {
                wallpaperManager.suggestDesiredDimensions(h, w);
            }
        } else {
            wallpaperManager.suggestDesiredDimensions(1280, 1706);
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
            // wallpaperManager.setResource(R.drawable.your_image);
        } catch (IOException e) {
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
