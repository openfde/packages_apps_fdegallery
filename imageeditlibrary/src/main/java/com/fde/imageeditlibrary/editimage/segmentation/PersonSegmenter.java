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
package com.fde.imageeditlibrary.editimage.segmentation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 人物抠图引擎：基于 TFLite / LiteRT 的 DeeplabV3 Person Segmentation 模型
 * （deeplabv3_257_mv_gpu.tflite）。
 *
 * <p>模型约定：
 * <ul>
 *     <li>输入：[1, 257, 257, 3] float32，RGB 像素归一化到 [-1, 1]</li>
 *     <li>输出：[1, 257, 257, 1] float32，每个像素为"人物"概率热力图</li>
 * </ul>
 *
 * <p>推理使用纯 CPU（关闭 XNNPACK / NNAPI 委托），避免在某些 armv8.0 芯片上触发
 * fp16/dotprod/SVE 等不支持的指令导致 SIGILL，与该库既有 TFLite 用法保持一致。
 *
 * @author openfde
 */
public class PersonSegmenter {
    private static final String TAG = "PersonSegmenter";
    public static final String MODEL_ASSET = "deeplabv3_person_segmentation.tflite";

    /** 模型输入边长 */
    private static final int DEFAULT_INPUT_SIZE = 257;

    /** 软阈值羽化区间：mask 概率在此区间内做过渡，边缘更平滑 */
    private static final float THRESHOLD_LOW = 0.30f;
    private static final float THRESHOLD_HIGH = 0.70f;

    private static volatile PersonSegmenter sInstance;

    private final Interpreter interpreter;
    private final int inputSize;
    private final ByteBuffer inputBuffer;

    public static PersonSegmenter getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PersonSegmenter.class) {
                if (sInstance == null) {
                    sInstance = new PersonSegmenter(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private PersonSegmenter(Context context) {
        Interpreter temp = null;
        try {
            ByteBuffer model = loadModelFile(context);
            Interpreter.Options options = new Interpreter.Options();
            int cores = Runtime.getRuntime().availableProcessors();
            options.setNumThreads(Math.min(4, Math.max(2, cores - 1)));
            // XNNPACK 委托会挑选 fp16 / dotprod / SVE 等更高版本指令集算子，
            // 在老 armv8.0 芯片上可能执行到不支持的指令导致 SIGILL，这里关闭，退回 armv8-a 基础内核。
            options.setUseXNNPACK(false);
            // NNAPI 委托可能引入设备相关问题，仅使用纯 CPU 推理
            options.setUseNNAPI(false);
            temp = new Interpreter(model, options);

            // 动态读取实际输入边长，兼容不同变体模型
            int side = DEFAULT_INPUT_SIZE;
            int[] inputShape = temp.getInputTensor(0).shape();
            if (inputShape != null && inputShape.length == 4) {
                side = Math.max(inputShape[1], inputShape[2]);
            }
            inputSize = side;
            inputBuffer = ByteBuffer.allocateDirect(side * side * 3 * 4)
                    .order(ByteOrder.nativeOrder());
        } catch (Exception e) {
            Log.e(TAG, "init person segmenter failed", e);
            if (temp != null) {
                try {
                    temp.close();
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("init person segmenter failed", e);
        }
        interpreter = temp;
    }

    /**
     * @return 是否已成功加载模型
     */
    public boolean isAvailable() {
        return interpreter != null;
    }

    /**
     * 人物抠图：返回与原图同尺寸的透明背景 Bitmap（人物区域保留原色），失败返回 null。
     * 背景像素 alpha 置 0，人物边缘做羽化（半透明过渡）。
     */
    public Bitmap cutout(Bitmap source) {
        if (source == null || interpreter == null) {
            return null;
        }
        try {
            synchronized (this) {
                int srcW = source.getWidth();
                int srcH = source.getHeight();
                if (srcW <= 0 || srcH <= 0) {
                    return null;
                }
                int size = inputSize;

                // letterbox：等比缩放入 257x257，四周补黑边保留原图宽高比
                float scale = Math.min((float) size / srcW, (float) size / srcH);
                int dw = Math.max(1, Math.round(srcW * scale));
                int dh = Math.max(1, Math.round(srcH * scale));
                int dx = (size - dw) / 2;
                int dy = (size - dh) / 2;

                // 一次性降采样并读入缩放后像素，避免对大图逐像素 getPixel()（性能差）
                Bitmap scaled = null;
                int[] scaledPixels = null;
                try {
                    scaled = Bitmap.createScaledBitmap(source, dw, dh, true);
                    scaledPixels = new int[dw * dh];
                    scaled.getPixels(scaledPixels, 0, dw, 0, 0, dw, dh);
                } finally {
                    if (scaled != null && scaled != source) {
                        scaled.recycle();
                    }
                }

                // 构建归一化输入 [-1, 1]
                inputBuffer.rewind();
                for (int ty = 0; ty < size; ty++) {
                    for (int tx = 0; tx < size; tx++) {
                        if (tx >= dx && tx < dx + dw && ty >= dy && ty < dy + dh) {
                            int sx = Math.min(dw - 1, Math.max(0, tx - dx));
                            int sy = Math.min(dh - 1, Math.max(0, ty - dy));
                            int pixel = scaledPixels[sy * dw + sx];
                            inputBuffer.putFloat(((pixel >> 16) & 0xff) / 127.5f - 1.0f);
                            inputBuffer.putFloat(((pixel >> 8) & 0xff) / 127.5f - 1.0f);
                            inputBuffer.putFloat((pixel & 0xff) / 127.5f - 1.0f);
                        } else {
                            inputBuffer.putFloat(0f);
                            inputBuffer.putFloat(0f);
                            inputBuffer.putFloat(0f);
                        }
                    }
                }

                float[][][][] output = new float[1][size][size][1];
                inputBuffer.rewind();
                interpreter.run(inputBuffer, output);

                return composeResult(source, output, srcW, srcH, size, dx, dy, dw, dh);
            }
        } catch (Exception e) {
            Log.e(TAG, "cutout failed", e);
            return null;
        }
    }

    /**
     * 由 257x257 概率图合成透明背景结果（按行处理，控制峰值内存）。
     */
    private Bitmap composeResult(Bitmap source, float[][][][] output,
                                 int srcW, int srcH, int size,
                                 int dx, int dy, int dw, int dh) {
        Bitmap result = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888);
        // 若内存紧张，回退到原图尺寸（基本不会发生）
        if (result == null) {
            return null;
        }

        int[] rowSrc = new int[srcW];
        int[] rowOut = new int[srcW];
        float[] alphaRow = new float[srcW];

        int right = dx + dw;
        int bottom = dy + dh;

        for (int y = 0; y < srcH; y++) {
            float ty = dy + (y * dh) / (float) srcH;
            for (int x = 0; x < srcW; x++) {
                float tx = dx + (x * dw) / (float) srcW;
                if (tx < dx || tx >= right || ty < dy || ty >= bottom) {
                    alphaRow[x] = 0f;
                } else {
                    alphaRow[x] = feather(bilinear(output, tx, ty));
                }
            }

            source.getPixels(rowSrc, 0, srcW, 0, y, srcW, 1);
            for (int x = 0; x < srcW; x++) {
                int a255 = Math.max(0, Math.min(255, (int) (alphaRow[x] * 255f + 0.5f)));
                rowOut[x] = (a255 << 24) | (rowSrc[x] & 0x00FFFFFF);
            }
            result.setPixels(rowOut, 0, srcW, 0, y, srcW, 1);
        }
        return result;
    }

    private float bilinear(float[][][][] output, float tx, float ty) {
        int x0 = Math.min(inputSize - 1, Math.max(0, (int) Math.floor(tx)));
        int y0 = Math.min(inputSize - 1, Math.max(0, (int) Math.floor(ty)));
        float fx = tx - x0;
        float fy = ty - y0;
        int x1 = Math.min(inputSize - 1, x0 + 1);
        int y1 = Math.min(inputSize - 1, y0 + 1);

        float v00 = output[0][y0][x0][0];
        float v10 = output[0][y0][x1][0];
        float v01 = output[0][y1][x0][0];
        float v11 = output[0][y1][x1][0];

        float top = v00 * (1 - fx) + v10 * fx;
        float bottom = v01 * (1 - fx) + v11 * fx;
        return top * (1 - fy) + bottom * fy;
    }

    /**
     * 软阈值羽化：概率 t 映射为 alpha。
     */
    private float feather(float t) {
        if (t <= THRESHOLD_LOW) {
            return 0f;
        }
        if (t >= THRESHOLD_HIGH) {
            return 1f;
        }
        return (t - THRESHOLD_LOW) / (THRESHOLD_HIGH - THRESHOLD_LOW);
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream is = context.getAssets().open(MODEL_ASSET)) {
            byte[] buffer = new byte[16384];
            int len;
            while ((len = is.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
        byte[] bytes = out.toByteArray();
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length);
        bb.order(ByteOrder.nativeOrder());
        bb.put(bytes);
        bb.rewind();
        return bb;
    }
}
