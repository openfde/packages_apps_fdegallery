package com.fde.gallery3d.ui.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.fde.gallery3d.ui.utils.LogTools;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 本机 TFLite 图片分类器：内置免费的 MobileNet V2 浮点模型，
 * 输入 224x224x3 的 float32 像素（归一化到 -1~1），输出 1001 个 ImageNet 类别置信度。
 * <p>
 * 注：不使用 uint8 量化模型。量化模型的卷积核在 aarch64 上会用到 dot-product
 * （sdot）等指令，某些 ARM 环境并不支持会触发 SIGILL 闪退，浮点模型只用
 * 基础 NEON 指令，兼容性最好。
 */
public class ImageClassifier {

    private static final String TAG = "ImageClassifier";
    private static final String MODEL_ASSET = "mobilenet_v2_1.0_224.tflite";
    private static final String LABELS_ASSET = "imagenet_labels.txt";
    private static final int INPUT_SIZE = 224;

    private static volatile ImageClassifier sInstance;

    private final Interpreter interpreter;
    private final List<String> labels;
    private final java.nio.FloatBuffer inputBuffer;
    private static final int OUTPUT_CLASSES = 1001;

    public static ImageClassifier getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ImageClassifier.class) {
                if (sInstance == null) {
                    sInstance = new ImageClassifier(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private ImageClassifier(Context context) {
        Interpreter tempInterpreter = null;
        try {
            ByteBuffer model = loadModelFile(context);
            Interpreter.Options options = new Interpreter.Options();
            int cores = Runtime.getRuntime().availableProcessors();
            options.setNumThreads(Math.min(4, Math.max(2, cores - 1)));
            // 默认启用的 XNNPACK 委托会在 aarch64 上选用更高版本指令集的算子
            // （如 fp16 / dotprod / SVE），在老一点的 armv8.0 芯片上会执行到
            // 不支持的指令导致 SIGILL 闪退，这里关闭委托，退回 armv8-a 基准内核。
            options.setUseXNNPACK(false);
            // NNAPI 委托同样可能引入设备的相关性问题，仅使用纯 CPU 推理
            options.setUseNNAPI(false);
            tempInterpreter = new Interpreter(model, options);
            labels = loadLabelList(context);
            inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
        } catch (Exception e) {
            LogTools.e(TAG + " init failed: " + e);
            if (tempInterpreter != null) {
                try {
                    tempInterpreter.close();
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("init TFLite failed", e);
        }
        interpreter = tempInterpreter;
    }

    /**
     * 对一张图片做分类，失败时返回 OTHER。
     */
    public AiCategory classify(String path) {
        if (interpreter == null || path == null) {
            return AiCategory.OTHER;
        }
        Bitmap bitmap = decodeSampledBitmap(path);
        if (bitmap == null) {
            return AiCategory.OTHER;
        }
        try {
            synchronized (this) {
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
                if (resized != bitmap) {
                    bitmap.recycle();
                }
                inputBuffer.rewind();
                convertBitmapToFloatBuffer(resized, inputBuffer);
                float[][] result = new float[1][OUTPUT_CLASSES];
                interpreter.run(inputBuffer, result);
                resized.recycle();

                int index = argmax(result[0]);
                String label = (index >= 0 && index < labels.size())
                        ? labels.get(index) : "";
                return AiCategory.fromLabel(label);
            }
        } catch (Exception e) {
            LogTools.e(TAG + " classify failed: " + path + ", " + e);
            return AiCategory.OTHER;
        }
    }

    /**
     * 先按比例降采样，再按 EXIF 方向旋转后返回原始位图（仍可能很大，由调用方缩放）。
     */
    @androidx.annotation.Nullable
    private Bitmap decodeSampledBitmap(String path) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            int sampleSize = 1;
            int target = INPUT_SIZE * 2;
            while (bounds.outWidth / (sampleSize * 2) >= target
                    && bounds.outHeight / (sampleSize * 2) >= target) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if (bitmap == null) {
                return null;
            }
            int rotation = getExifRotation(path);
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                bitmap = rotated;
            }
            return bitmap;
        } catch (Exception e) {
            LogTools.e(TAG + " decode failed: " + path + ", " + e);
            return null;
        }
    }

    private int getExifRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            switch (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void convertBitmapToFloatBuffer(Bitmap bitmap, java.nio.FloatBuffer buffer) {
        if (bitmap == null) {
            return;
        }
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        buffer.rewind();
        for (int pixel : pixels) {
            buffer.put(((pixel >> 16) & 0xff) / 127.5f - 1.0f);
            buffer.put(((pixel >> 8) & 0xff) / 127.5f - 1.0f);
            buffer.put((pixel & 0xff) / 127.5f - 1.0f);
        }
    }

    private int argmax(float[] scores) {
        int best = -1;
        float bestScore = -1f;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                best = i;
            }
        }
        return best;
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
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

    private List<String> loadLabelList(Context context) throws IOException {
        List<String> list = new ArrayList<>();
        try (InputStream is = context.getAssets().open(LABELS_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line.trim());
            }
        }
        return list;
    }
}
