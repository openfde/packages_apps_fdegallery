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
package com.fde.gallery3d.ui.common;

import android.content.Context;

public class Constant {
    public static final int MEDIA_PIC = 1;
    public static final int MEDIA_VIDEO = 2;
    public static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    public static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;
    public static final int REQUEST_SELECT_PHOTO = 1001;
    public static final int REQUEST_PERMISSION_DELETE_PHOTO = 600;
    public static final int REQUEST_DELETE_PHOTO = 601;

    public static final int ACTION_REQUEST_EDITIMAGE = 9;

    public static final int ACTION_REQUEST_UPDATE = 200;

    public static final String SP_STR = "open_fde";
    public static final String SP_MEDIA = "gallery_media_store";
    public static final String PKG_PROVIDER = "com.fde.gallery3d.fileprovider";

    /**
     * FileProvider 的 authority 必须与 AndroidManifest 中声明的一致，
     * debug 包名带 .debug 后缀，所以这里动态拼接，避免分享时崩溃。
     */
    public static String getProviderAuthority(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

}
