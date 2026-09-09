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
package com.fde.imageeditlibrary.editimage.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by openfde on 16/10/23.
 */
public class FileUtil {
    public static boolean checkFileExist(final String path) {
        if (TextUtils.isEmpty(path))
            return false;

        File file = new File(path);
        return file.exists();
    }

    // 获取文件扩展名
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 将图片文件加入到相册
     *
     * @param context
     * @param dstPath
     */
    public static void ablumUpdate(final Context context,Bitmap bitmap, final String dstPath) {
        try {
            if (TextUtils.isEmpty(dstPath) || context == null)
                return;

            String folder = Environment.DIRECTORY_PICTURES+"/fde/";
            ContentValues values  =  new ContentValues();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES +"/fde");
            }else {
                values.put(MediaStore.Images.Media.DATA, dstPath);
            }

            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "fde_" + System.currentTimeMillis() + ".png");


//            values.put(MediaStore.Images.Media.RELATIVE_PATH, folder);

            values.put(MediaStore.MediaColumns.WIDTH, 800);
            values.put(MediaStore.MediaColumns.HEIGHT, 800);
            Uri insertUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values );

            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(insertUri, "rw");

                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    Log.i("bella", "save success");
                } else {
                    Log.i("bella", "save fail");
                }
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
//        String res = MediaStore.Images.Media.insertImage(context.getContentResolver(), filePath, fileName, null);
//        Log.i("bella","res "+res);

            // 最后通知图库更新
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(dstPath)));
        } catch (Exception e) {
            Log.i("bella", "fail"+e.toString());
           e.printStackTrace();
        }
    }
}//end class
