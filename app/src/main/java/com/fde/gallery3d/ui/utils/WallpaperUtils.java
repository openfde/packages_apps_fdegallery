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
package com.fde.gallery3d.ui.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.IOException;

/**
 * 壁纸设置公共工具：拉伸、居中、居中裁剪、等比适应、平铺 5 种显示方式。
 * {@code SetWallPageActivity} 与 {@code PicturePreviewPersenter} 共用这一套逻辑。
 */
public class WallpaperUtils {

    private WallpaperUtils() {
    }

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

    /**
     * 按指定显示方式生成壁纸并设置（应放在后台线程调用）。
     *
     * @param context   上下文
     * @param path      原图路径
     * @param type      1 = 主屏幕，2 = 锁屏，其它 = 两者都设置
     * @param scaleMode {@link #WALLPAPER_SCALE_STRETCH} 等 5 种显示方式之一
     * @return 设置成功返回 true
     */
    public static boolean setWallpaper(Context context, String path, int type, int scaleMode) {
        if (context == null || TextUtils.isEmpty(path)) {
            return false;
        }
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            // 目标壁纸尺寸：优先使用系统期望的壁纸尺寸（默认即屏幕尺寸），取不到再用屏幕尺寸
            int targetW = wallpaperManager.getDesiredMinimumWidth();
            int targetH = wallpaperManager.getDesiredMinimumHeight();
            if (targetW <= 0 || targetH <= 0) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                targetW = dm.widthPixels;
                targetH = dm.heightPixels;
            }

            Bitmap src = decodeSampledBitmap(path, targetW, targetH);
            if (src == null) {
                return false;
            }

            Bitmap wallpaperBitmap = processWallpaperBitmap(src, scaleMode, targetW, targetH);
            if (wallpaperBitmap == null) {
                if (!src.isRecycled()) {
                    src.recycle();
                }
                return false;
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
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                // WallpaperManager 内部已拷贝数据，这里可以安全回收中间生成的壁纸
                if (wallpaperBitmap != null && wallpaperBitmap != src && !wallpaperBitmap.isRecycled()) {
                    wallpaperBitmap.recycle();
                }
                if (!src.isRecycled()) {
                    src.recycle();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 解码图片，原图远大于目标尺寸时按 2 倍目标尺寸降采样，避免解码超大图内存溢出。
     */
    private static Bitmap decodeSampledBitmap(String path, int reqW, int reqH) {
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
    private static Bitmap processWallpaperBitmap(Bitmap src, int scaleMode, int targetW, int targetH) {
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
}
