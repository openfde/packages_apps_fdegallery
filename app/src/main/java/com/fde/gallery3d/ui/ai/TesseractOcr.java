package com.fde.gallery3d.ui.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;

import com.fde.gallery3d.ui.utils.LogTools;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 本机离线 OCR（Tesseract 4 LSTM）。
 * <p>
 * 替代 ML Kit gOCR：ML Kit 的 gOCR 原生管线在目标设备/ABI 上静默失效
 * （成功回调但 blockCount=0），Tesseract 是纯 C++/Leptonica 的传统实现，
 * 无委托/新指令集依赖（不依赖 dotprod/SVE 等），x86/ARM 都能稳定运行，
 * 与本项目 ImageClassifier 关闭 XNNPACK/NNAPI 的兼容性思路一致。
 * <p>
 * 使用前确保 app/src/main/assets/tessdata/ 下有 chi_sim.traineddata（中文）
 * 和 eng.traineddata（英文），首次初始化时会被拷贝到应用私有目录
 * {@code <filesDir>/tessdata/}（Tesseract 只能从文件系统加载语言包）。
 */
public class TesseractOcr {

    private static final String TAG = "TesseractOcr";
    private static final String ASSETS_DIR = "tessdata";
    private static final String LANGUAGE = "chi_sim+eng";

    /** 识别前把图片按比例降采样到该长边上限，OCR 精度与速度的平衡点 */
    private static final int MAX_INPUT_DIM = 2048;

    private static volatile TesseractOcr sInstance;

    private final TessBaseAPI tess;
    private final boolean ready;

    public static TesseractOcr getInstance(Context context) {
        if (sInstance == null) {
            synchronized (TesseractOcr.class) {
                if (sInstance == null) {
                    sInstance = new TesseractOcr(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private TesseractOcr(Context context) {
        TessBaseAPI tempTess = null;
        boolean ok = false;
        try {
            File dataDir = new File(context.getFilesDir(), ASSETS_DIR);
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                LogTools.e(TAG + " mkdir failed: " + dataDir);
            }
            copyAssetIfNeeded(context, ASSETS_DIR + "/chi_sim.traineddata",
                    new File(dataDir, "chi_sim.traineddata"));
            copyAssetIfNeeded(context, ASSETS_DIR + "/eng.traineddata",
                    new File(dataDir, "eng.traineddata"));

            tempTess = new TessBaseAPI();
            ok = tempTess.init(context.getFilesDir().getAbsolutePath(), LANGUAGE);
            if (!ok) {
                LogTools.e(TAG + " TessBaseAPI.init returned false");
            }
        } catch (Exception e) {
            LogTools.e(TAG + " init failed: " + e);
            if (tempTess != null) {
                try {
                    tempTess.recycle();
                } catch (Exception ignored) {
                }
            }
            tempTess = null;
        }
        tess = tempTess;
        ready = ok && tess != null;
    }

    /**
     * 识别一张图片文件（自动降采样 + 按 EXIF 方向转正）。
     *
     * @return 识别出的文字；失败或识别为空时返回空字符串。
     */
    public String recognize(String path) {
        if (!ready || tess == null || TextUtils.isEmpty(path)) {
            return "";
        }
        Bitmap bitmap = decodeSampledRotated(path);
        if (bitmap == null) {
            return "";
        }
        try {
            synchronized (this) {
                tess.setImage(bitmap);
                String text = tess.getUTF8Text();
                return TextUtils.isEmpty(text) ? "" : text.trim();
            }
        } catch (Exception e) {
            LogTools.e(TAG + " recognize failed: " + path + ", " + e);
            return "";
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * 先降采样，再按 EXIF 方向旋转后返回位图。
     * BitmapFactory.decodeFile 不会应用 EXIF 方向，竖屏照片需手动转正。
     */
    private Bitmap decodeSampledRotated(String path) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            int sampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2)
                    >= MAX_INPUT_DIM) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if (bitmap == null) {
                return null;
            }
            int degree = readExifDegree(path);
            if (degree != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degree);
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

    private int readExifDegree(String path) {
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(path);
            switch (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL)) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static void copyAssetIfNeeded(Context context, String asset, File dest)
            throws IOException {
        if (dest.exists() && dest.length() > 0) {
            return;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(asset);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            Log.d(TAG, "copied asset " + asset + " -> " + dest);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
