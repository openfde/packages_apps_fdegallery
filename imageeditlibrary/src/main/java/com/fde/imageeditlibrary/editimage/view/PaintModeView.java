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
package com.fde.imageeditlibrary.editimage.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by openfde on 17/2/11.
 */

public class PaintModeView extends View {
    private Paint mPaint;

    private int mStokeColor;
    private float mStokeWidth = -1;

    private float mRadius;

    public PaintModeView(Context context) {
        super(context);
        initView(context);
    }

    public PaintModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PaintModeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PaintModeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    protected void initView(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);

        //mStokeWidth = 10;
        //mStokeColor = Color.RED;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(mStokeColor);
        mRadius = mStokeWidth / 2;

        canvas.drawCircle(getWidth() >> 1, getHeight() >> 1, mRadius, mPaint);
    }

    public void setPaintStrokeColor(final int newColor) {
        this.mStokeColor = newColor;
        this.invalidate();
    }

    public void setPaintStrokeWidth(final float width) {
        this.mStokeWidth = width;
        this.invalidate();
    }

    public float getStokenWidth() {
        if (mStokeWidth < 0) {
            mStokeWidth = getMeasuredHeight();
        }
        return mStokeWidth;
    }

    public int getStokenColor() {
        return mStokeColor;
    }

}//end class
