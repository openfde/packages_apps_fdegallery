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

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fde.imageeditlibrary.BaseActivity;
import com.fde.imageeditlibrary.R;
import com.fde.imageeditlibrary.editimage.EditImageActivity;
import com.fde.imageeditlibrary.editimage.ModuleConfig;
import com.fde.imageeditlibrary.editimage.segmentation.PersonSegmenter;
import com.fde.imageeditlibrary.editimage.view.CheckerBoardDrawable;
import com.fde.imageeditlibrary.editimage.view.imagezoom.ImageViewTouchBase;

import java.lang.ref.WeakReference;

/**
 * 人物抠图功能：一键识别人物并去除背景（透明背景），支持棋盘格预览。
 *
 * <p>交互（一键抠图 + 预览）：
 * <ul>
 *     <li>点击"抠图"：后台跑 TFLite 人物分割，完成后在主图区以棋盘格背景预览结果</li>
 *     <li>点击顶部"应用"：把抠图结果提交为主图，可继续叠加滤镜/文字等再保存</li>
 *     <li>点击"返回"或"恢复原图"：撤销预览，回到原图</li>
 * </ul>
 *
 * @author openfde
 */
public class CutoutFragment extends BaseEditFragment implements OnClickListener {
    public static final String TAG = CutoutFragment.class.getName();

    public static final int INDEX = ModuleConfig.INDEX_CUTOUT;

    private View mainView;
    private View backToMenu;// 返回按钮
    private View mCutoutBtn;// 一键抠图
    private View mRestoreBtn;// 恢复原图
    private TextView mHint;

    private PersonSegmenter mSegmenter;
    private CutoutTask mCutoutTask;

    private WeakReference<Bitmap> mResultBitmapRef;
    private boolean mPreviewing = false;

    public static CutoutFragment newInstance() {
        CutoutFragment fragment = new CutoutFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_edit_image_cutout, null);
        return mainView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        backToMenu = mainView.findViewById(R.id.back_to_main);
        mCutoutBtn = mainView.findViewById(R.id.btn_cutout);
        mRestoreBtn = mainView.findViewById(R.id.btn_restore);
        mHint = (TextView) mainView.findViewById(R.id.cutout_hint);

        backToMenu.setOnClickListener(this);
        mCutoutBtn.setOnClickListener(this);
        mRestoreBtn.setOnClickListener(this);
    }

    @Override
    public void onShow() {
        activity.mode = EditImageActivity.MODE_CUTOUT;
        restoreBitmap();
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        activity.mainImage.setScaleEnabled(false);
        activity.bannerFlipper.showNext();
        mRestoreBtn.setVisibility(View.GONE);
        mCutoutBtn.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v == backToMenu) {
            backToMain();
        } else if (v == mCutoutBtn) {
            startCutout();
        } else if (v == mRestoreBtn) {
            restoreBitmap();
        }
    }

    /**
     * 一键抠图：后台执行 TFLite 人物分割
     */
    private void startCutout() {
        if (mCutoutTask != null && !mCutoutTask.isCancelled()) {
            return;
        }
        Bitmap src = activity.getMainBit();
        if (src == null) {
            return;
        }
        mCutoutBtn.setEnabled(false);
        mCutoutTask = new CutoutTask();
        mCutoutTask.execute(src);
    }

    /**
     * 应用抠图结果
     */
    public void applyCutout() {
        if (mResultBitmapRef != null && mResultBitmapRef.get() != null) {
            activity.changeMainBitmap(mResultBitmapRef.get(), true);
        }
        backToMain();
    }

    /**
     * 撤销预览，恢复原图
     */
    private void restoreBitmap() {
        if (mPreviewing) {
            activity.mainImage.setBackground(null);
            activity.mainImage.setImageBitmap(activity.getMainBit());
            mPreviewing = false;
        }
        mRestoreBtn.setVisibility(View.GONE);
        mResultBitmapRef = null;
    }

    /**
     * 返回主菜单
     */
    @Override
    public void backToMain() {
        if (mCutoutTask != null && !mCutoutTask.isCancelled()) {
            mCutoutTask.cancel(true);
        }
        restoreBitmap();

        activity.mode = EditImageActivity.MODE_NONE;
        activity.bottomGallery.setCurrentItem(MainMenuFragment.INDEX);
        activity.mainImage.setScaleEnabled(true);
        activity.bannerFlipper.showPrevious();
    }

    /**
     * 人物抠图任务
     */
    private final class CutoutTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = BaseActivity.getLoadingDialog(getActivity(), R.string.cutout_ing, false);
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            try {
                synchronized (CutoutFragment.this) {
                    if (mSegmenter == null) {
                        mSegmenter = PersonSegmenter.getInstance(getActivity());
                    }
                }
                return mSegmenter.cutout(params[0]);
            } catch (Throwable t) {
                // 模型缺失 / 设备不支持等场景不崩溃，统一反馈失败
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dialog.dismiss();
        }

        @Override
        protected void onCancelled(Bitmap result) {
            super.onCancelled(result);
            dialog.dismiss();
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            dialog.dismiss();
            mCutoutBtn.setEnabled(true);

            if (result == null) {
                Toast.makeText(getActivity(), R.string.cutout_fail, Toast.LENGTH_SHORT).show();
                return;
            }

            mResultBitmapRef = new WeakReference<Bitmap>(result);
            mPreviewing = true;

            // 棋子格背景用于标识透明区域
            int square = Math.round(8 * getResources().getDisplayMetrics().density);
            activity.mainImage.setBackground(new CheckerBoardDrawable(square));
            activity.mainImage.setImageBitmap(result);
            mRestoreBtn.setVisibility(View.VISIBLE);
            if (mHint != null) {
                mHint.setText(R.string.cutout_hint_done);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCutoutTask != null && !mCutoutTask.isCancelled()) {
            mCutoutTask.cancel(true);
        }
    }
}// end class
