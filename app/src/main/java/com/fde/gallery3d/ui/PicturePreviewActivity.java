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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseActivity;
import com.fde.gallery3d.ui.picture.PicturePreviewFragment;

/**
 * 图片预览宿主 Activity。
 * 预览逻辑已经迁移到 {@link PicturePreviewFragment}，这里只负责承载 Fragment，
 * 用于外部 Intent（VIEW / EDIT 图片）以及时间轴等仍然以 Activity 方式打开预览的场景。
 */
public class PicturePreviewActivity extends BaseActivity {

    private static final String TAG_PREVIEW = "picture_preview";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preview_container, new PicturePreviewFragment(), TAG_PREVIEW)
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        PicturePreviewFragment fragment =
                (PicturePreviewFragment) getSupportFragmentManager().findFragmentByTag(TAG_PREVIEW);
        if (fragment != null) {
            fragment.reloadFromIntent();
        }
    }
}
