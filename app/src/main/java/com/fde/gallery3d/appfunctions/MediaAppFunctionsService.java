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

/**
 * 通过 App Functions 暴露"打开图片预览 / 播放视频"能力，供系统智能体（Agent）
 * 或其他调用方使用 {@link android.app.appfunctions.AppFunctionManager#executeAppFunction} 打开指定媒体。
 *
 * <p>支持的功能：
 * <ul>
 *     <li>{@code openImage}：参数 {@code path}（必填）、{@code title}（可选），打开图片预览。</li>
 *     <li>{@code playVideo}：参数 {@code path}（必填）、{@code title}（可选），播放视频。</li>
 * </ul>
 *
 * <p>功能列表在 {@code assets/appfunctions.xml} 中声明，AndroidManifest 中通过
 * {@code <property android:name="android.app.appfunctions" android:value="appfunctions.xml"/>} 关联。
 */
public class MediaAppFunctionsService extends AppFunctionService {
    private static final String TAG = "MediaAppFunctionsService";

    public static final String FUNCTION_OPEN_IMAGE = "openImage";
    public static final String FUNCTION_PLAY_VIDEO = "playVideo";

    /** 参数名（调用方通过 ExecuteAppFunctionRequest 的参数 GenericDocument 传入）。 */
    private static final String PARAM_PATH = "path";
    private static final String PARAM_TITLE = "title";

    private static final String RESULT_SCHEMA = "MediaResult";

    /** 视频列表跳转播放页时传入的 extra（与 VideoPlayActivity 读取的键保持一致）。 */
    private static final String EXTRA_VIDEO_DATA = "video_data";

    /** 结果文档中表示执行成功的字段值。 */
    private static final String RESULT_OK = "ok";

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
}
