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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * 透明棋盘格背景：用于抠图预览时直观标识透明（被抠除）区域。
 *
 * @author openfde
 */
public class CheckerBoardDrawable extends Drawable {
    private static final int LIGHT_COLOR = 0xFFFFFFFF;
    private static final int DARK_COLOR = 0xFFBDBDBD;

    private final Paint paint = new Paint();
    private final int squareSize;

    public CheckerBoardDrawable(int squareSize) {
        this.squareSize = Math.max(4, squareSize);
    }

    @Override
    public void draw(Canvas canvas) {
        int w = getBounds().width();
        int h = getBounds().height();
        if (w <= 0 || h <= 0) {
            return;
        }
        int row = 0;
        for (int y = 0; y < h; y += squareSize) {
            int col = 0;
            for (int x = 0; x < w; x += squareSize) {
                paint.setColor(((row + col) & 1) == 0 ? LIGHT_COLOR : DARK_COLOR);
                canvas.drawRect(x, y, Math.min(x + squareSize, w), Math.min(y + squareSize, h), paint);
                col++;
            }
            row++;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // ignore
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // ignore
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
