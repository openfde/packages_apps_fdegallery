package com.fde.gallery3d.ui.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.fde.gallery3d.ui.common.Constant;

import androidx.annotation.Nullable;

/**
 * AI 分类结果的本地缓存（SharedPreferences）：
 * 每张图片只分类一次，结果按路径缓存，避免每次进入分类页都重新推理。
 */
public class AiCategoryStore {

    private static final String KEY_PREFIX = "ai_cat_";

    private AiCategoryStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(Constant.SP_MEDIA, Context.MODE_PRIVATE);
    }

    /**
     * @return 已缓存的分类；从未分类过返回 null
     */
    @Nullable
    public static AiCategory get(Context context, String path) {
        if (context == null || TextUtils.isEmpty(path)) {
            return null;
        }
        String raw = prefs(context).getString(KEY_PREFIX + path, null);
        return TextUtils.isEmpty(raw) ? null : AiCategory.fromId(raw);
    }

    public static void put(Context context, String path, AiCategory category) {
        if (context == null || TextUtils.isEmpty(path)) {
            return;
        }
        prefs(context).edit()
                .putString(KEY_PREFIX + path, category == null ? null : category.name())
                .apply();
    }

    public static boolean isCached(Context context, String path) {
        if (context == null || TextUtils.isEmpty(path)) {
            return false;
        }
        return prefs(context).contains(KEY_PREFIX + path);
    }
}
