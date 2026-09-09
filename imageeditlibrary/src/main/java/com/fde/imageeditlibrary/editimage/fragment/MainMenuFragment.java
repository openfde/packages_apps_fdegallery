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
package com.fde.imageeditlibrary.editimage.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.fde.imageeditlibrary.R;
import com.fde.imageeditlibrary.editimage.ModuleConfig;


/**
 * 工具栏主菜单
 *
 * @author openfde
 */
public class MainMenuFragment extends BaseEditFragment implements View.OnClickListener {
    public static final int INDEX = ModuleConfig.INDEX_MAIN;

    public static final String TAG = MainMenuFragment.class.getName();
    private View mainView;

    private View stickerBtn;// 贴图按钮
    private View fliterBtn;// 滤镜按钮
    private View cropBtn;// 剪裁按钮
    private View rotateBtn;// 旋转按钮
    private View mTextBtn;//文字型贴图添加
    private View mPaintBtn;//编辑按钮
    private View mBeautyBtn;//美颜按钮

    public static MainMenuFragment newInstance() {
        MainMenuFragment fragment = new MainMenuFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_edit_image_main_menu,
                null);
        return mainView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        stickerBtn = mainView.findViewById(R.id.btn_stickers);
        fliterBtn = mainView.findViewById(R.id.btn_filter);
        cropBtn = mainView.findViewById(R.id.btn_crop);
        rotateBtn = mainView.findViewById(R.id.btn_rotate);
        mTextBtn = mainView.findViewById(R.id.btn_text);
        mPaintBtn = mainView.findViewById(R.id.btn_paint);
        mBeautyBtn = mainView.findViewById(R.id.btn_beauty);

        stickerBtn.setOnClickListener(this);
        fliterBtn.setOnClickListener(this);
        cropBtn.setOnClickListener(this);
        rotateBtn.setOnClickListener(this);
        mTextBtn.setOnClickListener(this);
        mPaintBtn.setOnClickListener(this);
        mBeautyBtn.setOnClickListener(this);
    }

    @Override
    public void onShow() {
        // do nothing
    }
    @Override
    public void backToMain() {
        //do nothing
    }

    @Override
    public void onClick(View v) {
        activity.setSaveBtnShow(true);
        if (v == stickerBtn) {
//            onStickClick();
            activity.switchFragmentSticker();
        } else if (v == fliterBtn) {
//            onFilterClick();
            activity.switchFragmentFilter();
        } else if (v == cropBtn) {
            activity.switchFragmentCrop();
//            onCropClick();
        } else if (v == rotateBtn) {
            activity.bottomGallery.setCurrentItem(RotateFragment.INDEX);
            activity.switchFragmentRotate();
//            onRotateClick();
        } else if (v == mTextBtn) {
//            onAddTextClick();
            activity.switchFragmentText();
        } else if (v == mPaintBtn) {
            activity.switchFragmentPaint();
//            onPaintClick();
        }else if(v == mBeautyBtn){
            activity.switchFragmentBeauty();
//            onBeautyClick();
        }
    }


    /**
     * 贴图模式
     *
     * @author openfde
     */
    private void onStickClick() {
        activity.bottomGallery.setCurrentItem(StickerFragment.INDEX);
        activity.mStickerFragment.onShow();

    }

    /**
     * 滤镜模式
     *
     * @author openfde
     */
    private void onFilterClick() {
        activity.bottomGallery.setCurrentItem(FilterListFragment.INDEX);
        activity.mFilterListFragment.onShow();
    }

    /**
     * 裁剪模式
     *
     * @author openfde
     */
    private void onCropClick() {
        activity.bottomGallery.setCurrentItem(CropFragment.INDEX);
        activity.mCropFragment.onShow();
    }

    /**
     * 图片旋转模式
     *
     * @author openfde
     */
    private void onRotateClick() {
        activity.bottomGallery.setCurrentItem(RotateFragment.INDEX);
        activity.mRotateFragment.onShow();
    }

    /**
     * 插入文字模式
     *
     * @author openfde
     */
    private void onAddTextClick() {
        activity.bottomGallery.setCurrentItem(AddTextFragment.INDEX);
        activity.mAddTextFragment.onShow();
    }

    /**
     * 自由绘制模式
     */
    private void onPaintClick() {
        activity.bottomGallery.setCurrentItem(PaintFragment.INDEX);
        activity.mPaintFragment.onShow();
    }

    private void onBeautyClick(){
        activity.bottomGallery.setCurrentItem(BeautyFragment.INDEX);
        activity.mBeautyFragment.onShow();
    }

}// end class
