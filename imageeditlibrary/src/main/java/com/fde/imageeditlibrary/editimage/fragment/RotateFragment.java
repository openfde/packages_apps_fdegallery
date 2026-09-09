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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.fde.imageeditlibrary.R;
import com.fde.imageeditlibrary.editimage.EditImageActivity;
import com.fde.imageeditlibrary.editimage.ModuleConfig;
import com.fde.imageeditlibrary.editimage.view.RotateImageView;
import com.fde.imageeditlibrary.editimage.view.imagezoom.ImageViewTouchBase;


/**
 * 图片旋转Fragment
 *
 * @author openfde
 */
public class RotateFragment extends BaseEditFragment {
    public static final int INDEX = ModuleConfig.INDEX_ROTATE;
    public static final String TAG = RotateFragment.class.getName();
    private View mainView;
    private View backToMenu;// backToMenu
    public SeekBar mSeekBar;// 角度设定
    private RotateImageView mRotatePanel;// 旋转效果展示控件

    public static RotateFragment newInstance() {
        RotateFragment fragment = new RotateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_edit_image_rotate, null);
        return mainView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        backToMenu = mainView.findViewById(R.id.back_to_main);
        mSeekBar = (SeekBar) mainView.findViewById(R.id.rotate_bar);
        mSeekBar.setProgress(0);

        this.mRotatePanel = ensureEditActivity().mRotatePanel;
        backToMenu.setOnClickListener(new BackToMenuClick());// backToMenu
        mSeekBar.setOnSeekBarChangeListener(new RotateAngleChange());
        onShow();
    }

    @Override
    public void onShow() {
        activity.mode = EditImageActivity.MODE_ROTATE;
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        activity.mainImage.setVisibility(View.GONE);

        activity.mRotatePanel.addBit(activity.getMainBit(),
                activity.mainImage.getBitmapRect());
        activity.mRotateFragment.mSeekBar.setProgress(0);
        activity.mRotatePanel.reset();
        activity.mRotatePanel.setVisibility(View.VISIBLE);
        activity.bannerFlipper.showNext();
    }

    /**
     * 角度改变监听
     *
     * @author openfde
     */
    private final class RotateAngleChange implements OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int angle,
                                      boolean fromUser) {
            // System.out.println("progress--->" + progress);
            mRotatePanel.rotateImage(angle);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }// end inner class

    /**
     * 返回按钮逻辑
     *
     * @author openfde
     */
    private final class BackToMenuClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            activity.setSaveBtnShow(true);
            backToMain();
        }
    }// end class

    /**
     * backToMenu
     */
    @Override
    public void backToMain() {
        activity.mode = EditImageActivity.MODE_NONE;
        activity.bottomGallery.setCurrentItem(0);
        activity.mainImage.setVisibility(View.VISIBLE);
        this.mRotatePanel.setVisibility(View.GONE);
        activity.bannerFlipper.showPrevious();
        activity.switchFragmentMain();
    }

    /**
     * 保存旋转图片
     */
    public void applyRotateImage() {
        // System.out.println("保存旋转图片");
        if (mSeekBar.getProgress() == 0 || mSeekBar.getProgress() == 360) {// 没有做旋转
            activity.setSaveBtnShow(false);
            backToMain();
            return;
        } else {// 保存图片
            SaveRotateImageTask task = new SaveRotateImageTask();
            task.execute(activity.getMainBit());
        }// end if
    }

    /**
     * 保存图片线程
     *
     * @author openfde
     */
    private final class SaveRotateImageTask extends
            AsyncTask<Bitmap, Void, Bitmap> {
        //private Dialog dialog;

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onCancelled(Bitmap result) {
            super.onCancelled(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Bitmap originBit = params[0];
            return rotate90AndSave(originBit);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            //dialog.dismiss();
            if (result == null)
                return;

            // 切换新底图
            activity.changeMainBitmap(result,true);
            activity.setSaveBtnShow(true);
            backToMain();
        }
    }// end inner class

    public Bitmap rotate90AndSave(Bitmap originBit ){
        RectF imageRect = mRotatePanel.getImageNewRect();
//        Bitmap originBit = params[0];
        Bitmap result = Bitmap.createBitmap((int) imageRect.width(),
                (int) imageRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        int w = originBit.getWidth() >> 1;
        int h = originBit.getHeight() >> 1;
        float centerX = imageRect.width() / 2;
        float centerY = imageRect.height() / 2;

        float left = centerX - w;
        float top = centerY - h;

        RectF dst = new RectF(left, top, left + originBit.getWidth(), top
                + originBit.getHeight());
        canvas.save();
        //bug fixed  应用时不需要考虑图片缩放问题 重新加载图片时 缩放控件会自动填充屏幕
//            canvas.scale(mRotatePanel.getScale(), mRotatePanel.getScale(),
//                    imageRect.width() / 2, imageRect.height() / 2);
        canvas.rotate(mRotatePanel.getRotateAngle(), imageRect.width() / 2,
                imageRect.height() / 2);

        canvas.drawBitmap(originBit, new Rect(0, 0, originBit.getWidth(),
                originBit.getHeight()), dst, null);
        canvas.restore();
        return  result;
    }
}// end class
