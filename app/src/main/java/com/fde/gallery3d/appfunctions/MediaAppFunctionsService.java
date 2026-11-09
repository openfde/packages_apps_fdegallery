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
package com.fde.gallery3d.appfunctions;

import android.app.appfunctions.AppFunctionException;
import android.app.appfunctions.AppFunctionService;
import android.app.appfunctions.ExecuteAppFunctionRequest;
import android.app.appfunctions.ExecuteAppFunctionResponse;
import android.app.appsearch.GenericDocument;
import android.content.Intent;
import android.content.pm.SigningInfo;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.VideoPlayActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.imageeditlibrary.editimage.utils.BitmapUtils;
import com.xinlan.imageeditlibrary.editimage.fliter.PhotoProcessing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通过 App Functions 暴露"打开图片预览 / 播放视频 / 图片编辑滤镜"能力，供系统智能体（Agent）
 * 或其他调用方使用 {@link android.app.appfunctions.AppFunctionManager#executeAppFunction} 调用。
 *
 * <p>支持的功能：
 * <ul>
 *     <li>{@code openImage}：参数 {@code path}（必填）、{@code title}（可选），打开图片预览。</li>
 *     <li>{@code playVideo}：参数 {@code path}（必填）、{@code title}（可选），播放视频。</li>
 *     <li>{@code editImage}：参数 {@code path}（必填）、{@code filter}（可选，缺省黑白）、
 *         {@code outputPath}（可选），给图片添加滤镜并保存，返回 {@code outputPath}。</li>
 * </ul>
 *
 * <p>功能列表在 {@code assets/appfunctions.xml} 中声明，AndroidManifest 中通过
 * {@code <property android:name="android.app.appfunctions" android:value="appfunctions.xml"/>} 关联。
 */
public class MediaAppFunctionsService extends AppFunctionService {
    private static final String TAG = "MediaAppFunctionsService";

    public static final String FUNCTION_OPEN_IMAGE = "openImage";
    public static final String FUNCTION_PLAY_VIDEO = "playVideo";
    public static final String FUNCTION_EDIT_IMAGE = "editImage";

    /** 参数名（调用方通过 ExecuteAppFunctionRequest 的参数 GenericDocument 传入）。 */
    private static final String PARAM_PATH = "path";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_FILTER = "filter";
    private static final String PARAM_OUTPUT_PATH = "outputPath";

    private static final String RESULT_SCHEMA = "MediaResult";
    private static final String RESULT_KEY_OUTPUT_PATH = "outputPath";
    private static final String RESULT_KEY_FILTER = "filter";

    /** 视频列表跳转播放页时传入的 extra（与 VideoPlayActivity 读取的键保持一致）。 */
    private static final String EXTRA_VIDEO_DATA = "video_data";

    /** 结果文档中表示执行成功的字段值。 */
    private static final String RESULT_OK = "ok";

    /** onExecuteFunction 运行在主线程，滤镜属于耗时操作，放到独立后台线程执行。 */
    private static final ExecutorService FILTER_EXECUTOR = Executors.newSingleThreadExecutor();

    /** 图片编辑采样上限（边长像素），限制 Bitmap 与 native 缓冲内存占用。 */
    private static final int MAX_EDIT_SIDE = 4096;

    /** editImage 缺省滤镜：黑白（参考 FilterListFragment 的黑白效果）。 */
    private static final String DEFAULT_FILTER = "black_white";

    /**
     * 滤镜名称 → {@link PhotoProcessing#filterPhoto(Bitmap, int)} 的位置参数。
     *
     * <p>位置参数与 FilterListFragment 中 {@code R.array.filters} 的下标一致：
     * <pre>
     *   0  origin(原始)     1  soft(软化/Instafix)  2  ansel(黑白UI项/Ansel)
     *   3  classic(经典)    4  gorgeous(华丽)        5  retro(复古)
     *   6  black_white(黑白/grace, nativeApplyBW)   7  sepia(电影/film)
     *   8  memory(回忆)     9  yogurt(优格)         10  fleeting(流年)
     *  11  shine(发光/HDR)  12  mosaic(马赛克)
     * </pre>
     */
    private static final Map<String, Integer> FILTER_NAME_TO_POSITION = new HashMap<>();

    static {
        // 原图（不处理，仅另存副本）
        FILTER_NAME_TO_POSITION.put("origin", 0);
        FILTER_NAME_TO_POSITION.put("original", 0);
        // 软化
        FILTER_NAME_TO_POSITION.put("soft", 1);
        FILTER_NAME_TO_POSITION.put("instafix", 1);
        // Ansel（UI 上的“黑白”项实际为 Ansel 滤镜）
        FILTER_NAME_TO_POSITION.put("ansel", 2);
        // 经典
        FILTER_NAME_TO_POSITION.put("classic", 3);
        FILTER_NAME_TO_POSITION.put("testino", 3);
        // 华丽
        FILTER_NAME_TO_POSITION.put("gorgeous", 4);
        FILTER_NAME_TO_POSITION.put("xpro", 4);
        // 复古
        FILTER_NAME_TO_POSITION.put("retro", 5);
        // 黑白（nativeApplyBW，真正的黑白滤镜）
        FILTER_NAME_TO_POSITION.put("black_white", 6);
        FILTER_NAME_TO_POSITION.put("bw", 6);
        FILTER_NAME_TO_POSITION.put("gray", 6);
        // 电影 / 怀旧
        FILTER_NAME_TO_POSITION.put("sepia", 7);
        FILTER_NAME_TO_POSITION.put("film", 7);
        // 回忆
        FILTER_NAME_TO_POSITION.put("memory", 8);
        FILTER_NAME_TO_POSITION.put("cyano", 8);
        // 优格
        FILTER_NAME_TO_POSITION.put("yogurt", 9);
        FILTER_NAME_TO_POSITION.put("georgia", 9);
        // 流年
        FILTER_NAME_TO_POSITION.put("fleeting", 10);
        FILTER_NAME_TO_POSITION.put("sahara", 10);
        // 发光
        FILTER_NAME_TO_POSITION.put("shine", 11);
        FILTER_NAME_TO_POSITION.put("hdr", 11);
        // 马赛克
        FILTER_NAME_TO_POSITION.put("mosaic", 12);
    }

    @Override
    public void onExecuteFunction(
            @NonNull ExecuteAppFunctionRequest request,
            @NonNull String callingPackage,
            @NonNull SigningInfo callingPackageSigningInfo,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String functionId = request.getFunctionIdentifier();
        Log.d(TAG, "onExecuteFunction: " + functionId + " from " + callingPackage);

        final boolean[] cancelled = {false};
        cancellationSignal.setOnCancelListener(() -> cancelled[0] = true);

        try {
            switch (String.valueOf(functionId)) {
                case FUNCTION_OPEN_IMAGE:
                case FUNCTION_PLAY_VIDEO:
                    if (cancelled[0]) {
                        callback.onError(new AppFunctionException(
                                AppFunctionException.ERROR_CANCELLED, "Execution cancelled"));
                        return;
                    }
                    openMedia(functionId, request.getParameters(), callback);
                    break;
                case FUNCTION_EDIT_IMAGE:
                    editImage(request.getParameters(), cancelled, callback);
                    break;
                default:
                    callback.onError(new AppFunctionException(
                            AppFunctionException.ERROR_FUNCTION_NOT_FOUND,
                            "Unknown function: " + functionId));
            }
        } catch (AppFunctionException e) {
            Log.e(TAG, "onExecuteFunction failed for " + functionId, e);
            callback.onError(e);
        } catch (Exception e) {
            Log.e(TAG, "onExecuteFunction failed for " + functionId, e);
            callback.onError(new AppFunctionException(
                    AppFunctionException.ERROR_SYSTEM_ERROR,
                    "Failed to execute function: " + e.getMessage()));
        }
    }

    /**
     * 打开图片预览或播放视频。
     *
     * <p>onExecuteFunction 运行在主线程，这里只做轻量的参数解析 + 启动 Activity，
     * 不会产生阻塞主线程的耗时操作。
     */
    private void openMedia(String functionId, GenericDocument parameters,
            OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback)
            throws AppFunctionException {
        String path = parameters == null ? null : parameters.getPropertyString(PARAM_PATH);
        if (TextUtils.isEmpty(path)) {
            throw new AppFunctionException(AppFunctionException.ERROR_INVALID_ARGUMENT,
                    "Missing required parameter: " + PARAM_PATH);
        }
        String title = parameters == null || parameters.getPropertyString(PARAM_TITLE) == null
                ? null : parameters.getPropertyString(PARAM_TITLE);

        Multimedia media = new Multimedia();
        media.setPath(path);
        media.setTitle(title == null ? "" : title);

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (FUNCTION_OPEN_IMAGE.equals(functionId)) {
                media.setMediaType(Constant.MEDIA_PIC);
                intent.setClass(this, PicturePreviewActivity.class);
                intent.putExtra(PicturePreviewFragment.ARG_PICTURE, media);
            } else {
                media.setMediaType(Constant.MEDIA_VIDEO);
                intent.setClass(this, VideoPlayActivity.class);
                intent.putExtra(EXTRA_VIDEO_DATA, media);
            }
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "failed to start activity for " + functionId, e);
            throw new AppFunctionException(AppFunctionException.ERROR_SYSTEM_ERROR,
                    "Failed to launch: " + e.getMessage());
        }

        GenericDocument resultDocument = new GenericDocument.Builder<>(
                getPackageName(), functionId + "_result", RESULT_SCHEMA)
                .setPropertyString("status", RESULT_OK)
                .build();
        callback.onResult(new ExecuteAppFunctionResponse(resultDocument));
    }

    /**
     * 图片编辑：给指定图片添加滤镜并保存。
     *
     * <p>具体流程参考 {@code FilterListFragment}：
     * <ol>
     *     <li>用 {@link BitmapUtils#getSampledBitmap} 采样加载原图（限制最大边长，防 OOM）；</li>
     *     <li>复制为 ARGB_8888 可变位图（与 FilterListFragment 的 ProcessingImage 一致）；</li>
     *     <li>调用 {@link PhotoProcessing#filterPhoto} 生成滤镜图（如黑白）；</li>
     *     <li>保存到 {@code outputPath}（缺省 Pictures/fde 目录）并触发相册扫描。</li>
     * </ol>
     *
     * <p>滤镜处理在后台线程执行，避免阻塞主线程。
     */
    private void editImage(GenericDocument parameters,
            boolean[] cancelled,
            OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback)
            throws AppFunctionException {
        String path = parameters == null ? null : parameters.getPropertyString(PARAM_PATH);
        if (TextUtils.isEmpty(path)) {
            throw new AppFunctionException(AppFunctionException.ERROR_INVALID_ARGUMENT,
                    "Missing required parameter: " + PARAM_PATH);
        }
        String filterName = parameters == null
                ? null : parameters.getPropertyString(PARAM_FILTER);
        if (TextUtils.isEmpty(filterName)) {
            filterName = DEFAULT_FILTER;
        }
        final int position = parseFilterPosition(filterName);
        if (position < 0) {
            throw new AppFunctionException(AppFunctionException.ERROR_INVALID_ARGUMENT,
                    "Unknown filter: " + filterName);
        }
        final String effectiveFilter = filterName;
        final String outputPath = parameters == null
                ? null : parameters.getPropertyString(PARAM_OUTPUT_PATH);

        FILTER_EXECUTOR.execute(() -> {
            try {
                if (cancelled[0]) {
                    callback.onError(new AppFunctionException(
                            AppFunctionException.ERROR_CANCELLED, "Execution cancelled"));
                    return;
                }
                String resultPath = applyFilterAndSave(path, position, effectiveFilter, outputPath);
                if (cancelled[0]) {
                    callback.onError(new AppFunctionException(
                            AppFunctionException.ERROR_CANCELLED, "Execution cancelled"));
                    return;
                }
                if (resultPath == null) {
                    callback.onError(new AppFunctionException(
                            AppFunctionException.ERROR_SYSTEM_ERROR, "Failed to edit image"));
                    return;
                }
                Log.d(TAG, "editImage done, filter=" + effectiveFilter + ", output=" + resultPath);
                GenericDocument resultDocument = new GenericDocument.Builder<>(
                        getPackageName(), FUNCTION_EDIT_IMAGE + "_result", RESULT_SCHEMA)
                        .setPropertyString("status", RESULT_OK)
                        .setPropertyString(RESULT_KEY_OUTPUT_PATH, resultPath)
                        .setPropertyString(RESULT_KEY_FILTER, effectiveFilter)
                        .build();
                callback.onResult(new ExecuteAppFunctionResponse(resultDocument));
            } catch (Exception e) {
                Log.e(TAG, "editImage failed for " + path, e);
                callback.onError(new AppFunctionException(
                        AppFunctionException.ERROR_SYSTEM_ERROR,
                        "Failed to edit image: " + e.getMessage()));
            }
        });
    }

    /**
     * 滤镜名称解析 → {@link PhotoProcessing#filterPhoto} 的位置参数。
     *
     * @return 合法滤镜位置（0~12），未知滤镜返回 -1
     */
    private static int parseFilterPosition(String filterName) {
        if (TextUtils.isEmpty(filterName)) {
            return -1;
        }
        Integer position = FILTER_NAME_TO_POSITION.get(
                filterName.trim().toLowerCase(Locale.ROOT));
        return position == null ? -1 : position;
    }

    /**
     * 参考 FilterListFragment 的 ProcessingImage 流程：采样加载 → 复制可变位图 →
     * PhotoProcessing 滤镜处理 → 保存。返回保存后的文件路径，失败返回 null。
     */
    private String applyFilterAndSave(String srcPath, int position,
            String filterName, String outputPath) {
        Bitmap sampled = null;
        Bitmap copy = null;
        Bitmap filtered = null;
        try {
            // FilterListFragment.LoadImageTask：BitmapUtils.getSampledBitmap 采样加载
            sampled = BitmapUtils.getSampledBitmap(srcPath, MAX_EDIT_SIDE, MAX_EDIT_SIDE);
            if (sampled == null) {
                Log.w(TAG, "failed to decode image: " + srcPath);
                return null;
            }
            // FilterListFragment.ProcessingImage：复制为 ARGB_8888 可变位图
            copy = sampled.copy(Bitmap.Config.ARGB_8888, true);
            if (copy == null) {
                return null;
            }
            filtered = PhotoProcessing.filterPhoto(copy, position);
            if (filtered == null) {
                Log.w(TAG, "filterPhoto returned null, filter=" + filterName);
                return null;
            }

            String target = outputPath;
            if (TextUtils.isEmpty(target)) {
                File defaultOut = FileUtils.genEditFile();
                if (defaultOut == null) {
                    return null;
                }
                target = defaultOut.getAbsolutePath();
            }
            return saveBitmap(target, filtered) ? target : null;
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "out of memory while editing image", oom);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "applyFilterAndSave failed", e);
            return null;
        } finally {
            // filtered 可能复用 copy（原生缓冲原位填充），也可能新建（如马赛克），去重释放
            recycleQuietly(filtered);
            if (copy != null && copy != filtered) {
                recycleQuietly(copy);
            }
            recycleQuietly(sampled);
        }
    }

    private static void recycleQuietly(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            try {
                bitmap.recycle();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 保存 Bitmap 到指定路径（PNG，高质量），并触发相册扫描。
     */
    private boolean saveBitmap(String filePath, Bitmap bitmap) {
        if (TextUtils.isEmpty(filePath) || bitmap == null) {
            return false;
        }
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Log.w(TAG, "failed to create parent dir: " + parent);
                return false;
            }
            try (OutputStream out = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return false;
                }
                out.flush();
            }
            // 通知相册/图库更新
            try {
                MediaScannerConnection.scanFile(this,
                        new String[]{filePath}, new String[]{"image/png"}, null);
            } catch (Exception e) {
                Log.w(TAG, "media scan failed for " + filePath, e);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap failed: " + filePath, e);
            return false;
        }
    }
}
